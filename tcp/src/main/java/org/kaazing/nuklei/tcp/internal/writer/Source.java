/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.nuklei.tcp.internal.writer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;

import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.connector.Connector;
import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.tcp.internal.router.RouteKind;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.FrameFW;
import org.kaazing.nuklei.tcp.internal.types.stream.ResetFW;
import org.kaazing.nuklei.tcp.internal.types.stream.WindowFW;
import org.kaazing.nuklei.tcp.internal.writer.stream.StreamFactory;

public final class Source implements Nukleus
{
    private final FrameFW frameRO = new FrameFW();
    private final BeginFW beginRO = new BeginFW();

    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final ResetFW.Builder resetRW = new ResetFW.Builder();
    private final WindowFW.Builder windowRW = new WindowFW.Builder();

    private final String partitionName;
    private final Connector connector;
    private final LongFunction<List<Route>> lookupRoutes;
    private final LongFunction<SocketChannel> resolveReply;
    private final StreamsLayout layout;
    private final AtomicBuffer writeBuffer;
    private final RingBuffer streamsBuffer;
    private final RingBuffer throttleBuffer;
    private final StreamFactory streamFactory;
    private final Long2ObjectHashMap<MessageHandler> streams;

    Source(
        String partitionName,
        Connector connector,
        LongFunction<List<Route>> lookupRoutes,
        LongFunction<SocketChannel> resolveReply,
        StreamsLayout layout,
        AtomicBuffer writeBuffer)
    {
        this.partitionName = partitionName;
        this.connector = connector;
        this.lookupRoutes = lookupRoutes;
        this.resolveReply = resolveReply;
        this.layout = layout;
        this.writeBuffer = writeBuffer;
        this.streamsBuffer = layout.streamsBuffer();
        this.throttleBuffer = layout.throttleBuffer();
        this.streamFactory = new StreamFactory(this, 8192, 8192); // TODO: configure 8192
        this.streams = new Long2ObjectHashMap<>();
    }

    @Override
    public int process()
    {
        return streamsBuffer.read(this::handleRead);
    }

    @Override
    public void close() throws Exception
    {
        layout.close();
    }

    @Override
    public String name()
    {
        return partitionName;
    }

    @Override
    public String toString()
    {
        return String.format("%s[name=%s]", getClass().getSimpleName(), partitionName);
    }

    private void handleRead(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        frameRO.wrap(buffer, index, index + length);

        final long streamId = frameRO.streamId();
        final MessageHandler stream = streams.get(streamId);

        if (stream != null)
        {
            stream.onMessage(msgTypeId, buffer, index, length);
        }
        else if (msgTypeId == BeginFW.TYPE_ID)
        {
            handleBegin(buffer, index, length);
        }
        else
        {
            doReset(streamId);
        }
    }

    private void handleBegin(
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        beginRO.wrap(buffer, index, index + length);

        final long streamId = beginRO.streamId();
        final long referenceId = beginRO.referenceId();
        final long correlationId = beginRO.correlationId();

        switch (RouteKind.match(referenceId))
        {
        case SERVER_REPLY:
            handleBeginServerReply(buffer, index, length, streamId, referenceId, correlationId);
            break;
        case CLIENT_INITIAL:
            handleBeginClientInitial(streamId, referenceId, correlationId);
            break;
        default:
            doReset(streamId);
            break;
        }
    }

    private void handleBeginServerReply(
        MutableDirectBuffer buffer,
        int index,
        int length,
        final long streamId,
        final long referenceId,
        final long correlationId)
    {
        final List<Route> routes = lookupRoutes.apply(referenceId);
        final SocketChannel channel = resolveReply.apply(correlationId);

        final Optional<Route> optional = routes.stream()
              .filter(r -> validateReplyRoute(r, channel))
              .findFirst();

        if (optional.isPresent())
        {
            final Route route = optional.get();
            final Target target = route.target();
            final MessageHandler newStream = streamFactory.newStream(streamId, target, channel);

            streams.put(streamId, newStream);

            newStream.onMessage(BeginFW.TYPE_ID, buffer, index, length);
        }
        else
        {
            doReset(streamId);
        }
    }

    private void handleBeginClientInitial(
        final long streamId,
        final long referenceId,
        final long correlationId)
    {
        final List<Route> routes = lookupRoutes.apply(referenceId);

        final Optional<Route> optional = routes.stream().findFirst();

        if (optional.isPresent())
        {
            final Route route = optional.get();
            final Target target = route.target();
            final long targetRef = route.targetRef();
            final SocketChannel channel = newSocketChannel();

            final MessageHandler newStream = streamFactory.newStream(streamId, target, channel);

            streams.put(streamId, newStream);

            final String targetName = route.target().name();
            final InetSocketAddress remoteAddress = route.address();

            connector.doConnect(
                    partitionName, referenceId, streamId, correlationId, targetName, targetRef, channel, remoteAddress);
        }
        else
        {
            doReset(streamId);
        }
    }

    private boolean validateReplyRoute(
        final Route route,
        final SocketChannel channel)
    {
        try
        {
            return route != null && channel != null && route.address().equals(channel.getLocalAddress());
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        // unreachable
        return false;
    }

    private SocketChannel newSocketChannel()
    {
        try
        {
            final SocketChannel channel = SocketChannel.open();
            channel.configureBlocking(false);
            return channel;
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        // unreachable
        return null;
    }

    public void onConnected(
        long sourceId,
        long sourceRef,
        Target target,
        SocketChannel channel,
        long correlationId)
    {
        final MessageHandler newStream = streamFactory.newStream(sourceId, target, channel);

        streams.put(sourceId, newStream);

        final BeginFW begin = beginRW.wrap(writeBuffer, 0, writeBuffer.capacity())
            .streamId(sourceId)
            .referenceId(sourceRef)
            .correlationId(correlationId)
            .build();

        newStream.onMessage(BeginFW.TYPE_ID, writeBuffer, begin.offset(), begin.length());
    }

    public void doWindow(
        final long streamId,
        final int update)
    {
        final WindowFW window = windowRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId)
                .update(update)
                .build();

        throttleBuffer.write(window.typeId(), window.buffer(), window.offset(), window.length());
    }

    public void doReset(
        final long streamId)
    {
        final ResetFW reset = resetRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId)
                .build();

        throttleBuffer.write(reset.typeId(), reset.buffer(), reset.offset(), reset.length());
    }

    public void replaceStream(
        long streamId,
        MessageHandler handler)
    {
        streams.put(streamId, handler);
    }

    public void removeStream(
        long streamId)
    {
        streams.remove(streamId);
    }
}
