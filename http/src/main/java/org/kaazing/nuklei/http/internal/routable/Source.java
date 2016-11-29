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
package org.kaazing.nuklei.http.internal.routable;

import static org.kaazing.nuklei.http.internal.router.RouteKind.CLIENT_INITIAL;
import static org.kaazing.nuklei.http.internal.router.RouteKind.CLIENT_REPLY;
import static org.kaazing.nuklei.http.internal.router.RouteKind.SERVER_INITIAL;
import static org.kaazing.nuklei.http.internal.router.RouteKind.SERVER_REPLY;

import java.util.EnumMap;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.LongLongConsumer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.http.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.http.internal.routable.stream.ClientInitialStreamFactory;
import org.kaazing.nuklei.http.internal.routable.stream.ClientReplyStreamFactory;
import org.kaazing.nuklei.http.internal.routable.stream.ServerInitialStreamFactory;
import org.kaazing.nuklei.http.internal.routable.stream.ServerReplyStreamFactory;
import org.kaazing.nuklei.http.internal.router.RouteKind;
import org.kaazing.nuklei.http.internal.types.stream.BeginFW;
import org.kaazing.nuklei.http.internal.types.stream.FrameFW;
import org.kaazing.nuklei.http.internal.types.stream.ResetFW;
import org.kaazing.nuklei.http.internal.types.stream.WindowFW;

public final class Source implements Nukleus
{
    private final FrameFW frameRO = new FrameFW();
    private final BeginFW beginRO = new BeginFW();

    private final ResetFW.Builder resetRW = new ResetFW.Builder();
    private final WindowFW.Builder windowRW = new WindowFW.Builder();

    private final String sourceName;
    private final String partitionName;
    private final StreamsLayout layout;
    private final AtomicBuffer writeBuffer;
    private final RingBuffer streamsBuffer;
    private final RingBuffer throttleBuffer;
    private final Long2ObjectHashMap<MessageHandler> streams;

    private final EnumMap<RouteKind, Supplier<MessageHandler>> streamFactories;

    Source(
        String sourceName,
        String partitionName,
        StreamsLayout layout,
        AtomicBuffer writeBuffer,
        LongFunction<List<Route>> supplyRoutes,
        LongFunction<List<Route>> supplyRejects,
        LongSupplier supplyTargetId,
        LongLongConsumer correlateInitial,
        LongUnaryOperator correlateReply)
    {
        this.sourceName = sourceName;
        this.partitionName = partitionName;
        this.layout = layout;
        this.writeBuffer = writeBuffer;

        this.streamsBuffer = layout.streamsBuffer();
        this.throttleBuffer = layout.throttleBuffer();
        this.streams = new Long2ObjectHashMap<>();

        this.streamFactories = new EnumMap<>(RouteKind.class);
        this.streamFactories.put(SERVER_INITIAL,
                new ServerInitialStreamFactory(this, supplyRoutes, supplyRejects, supplyTargetId, correlateInitial)::newStream);
        this.streamFactories.put(SERVER_REPLY,
                new ServerReplyStreamFactory(this, supplyRoutes, supplyTargetId, correlateReply)::newStream);
        this.streamFactories.put(CLIENT_INITIAL,
                new ClientInitialStreamFactory(this, supplyRoutes, supplyTargetId, correlateInitial)::newStream);
        this.streamFactories.put(CLIENT_REPLY,
                new ClientReplyStreamFactory(this, supplyRoutes, supplyTargetId, correlateReply)::newStream);
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

    public String routableName()
    {
        return sourceName;
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
        if (length == 0)
        {
            return;
        }
        frameRO.wrap(buffer, index, index + length);

        final long streamId = frameRO.streamId();

        // TODO: use Long2LongHashMap.getOrDefault(long, long)
        final MessageHandler handler = streams.get(streamId);

        if (handler != null)
        {
            handler.onMessage(msgTypeId, buffer, index, length);
        }
        else
        {
            handleUnrecognized(msgTypeId, buffer, index, length);
        }
    }

    private void handleUnrecognized(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        if (msgTypeId == BeginFW.TYPE_ID)
        {
            handleBegin(msgTypeId, buffer, index, length);
        }
        else
        {
            frameRO.wrap(buffer, index, index + length);

            final long streamId = frameRO.streamId();

            doReset(streamId);
        }
    }

    private void handleBegin(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        beginRO.wrap(buffer, index, index + length);
        final long sourceId = beginRO.streamId();
        final long sourceRef = beginRO.referenceId();

        final Supplier<MessageHandler> streamFactory = streamFactories.get(RouteKind.match(sourceRef));
        final MessageHandler newStream = streamFactory.get();
        streams.put(sourceId, newStream);
        newStream.onMessage(msgTypeId, buffer, index, length);
    }

    public void doWindow(
        final long streamId,
        final int update)
    {
        final WindowFW window = windowRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId).update(update).build();

        throttleBuffer.write(window.typeId(), window.buffer(), window.offset(), window.length());
    }

    public void doReset(
        final long streamId)
    {
        final ResetFW reset = resetRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId).build();

        throttleBuffer.write(reset.typeId(), reset.buffer(), reset.offset(), reset.length());
    }

    public void removeStream(
        long streamId)
    {
        streams.remove(streamId);
    }
}
