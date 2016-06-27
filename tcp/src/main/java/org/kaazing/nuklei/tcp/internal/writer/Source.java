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
import java.util.function.LongFunction;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.FrameFW;
import org.kaazing.nuklei.tcp.internal.types.stream.ResetFW;
import org.kaazing.nuklei.tcp.internal.types.stream.WindowFW;
import org.kaazing.nuklei.tcp.internal.writer.stream.StreamFactory;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Source implements Nukleus
{
    private final FrameFW frameRO = new FrameFW();
    private final BeginFW beginRO = new BeginFW();

    private final ResetFW.Builder resetRW = new ResetFW.Builder();
    private final WindowFW.Builder windowRW = new WindowFW.Builder();

    private final String name;
    private final Writer writer;
    private final LongFunction<Route> lookupRoute;
    private final LongFunction<Reply> lookupReply;
    private final StreamsLayout layout;
    private final AtomicBuffer writeBuffer;
    private final RingBuffer streamsBuffer;
    private final RingBuffer throttleBuffer;
    private final StreamFactory streamFactory;
    private final Long2ObjectHashMap<MessageHandler> streams;
    private final BeginFW.Builder beginRW = new BeginFW.Builder();

    Source(
        String partitionName,
        Writer writer,
        LongFunction<Route> lookupRoute,
        LongFunction<Reply> lookupReply,
        StreamsLayout layout,
        AtomicBuffer writeBuffer)
    {
        this.name = String.format("sources[%s]", partitionName);
        this.writer = writer;
        this.lookupRoute = lookupRoute;
        this.lookupReply = lookupReply;
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
        return name;
    }

    @Override
    public String toString()
    {
        return name;
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
        final long routableRef = beginRO.routableRef();
        final long replyId = beginRO.replyId();
        final long replyRef = beginRO.replyRef();

        if (routableRef > 0)
        {
            final Route route = lookupRoute.apply(routableRef);
            if (route != null)
            {
                final Target target = route.target();
                final SocketChannel channel = newSocketChannel();

                final MessageHandler newStream = streamFactory.newStream(streamId, target, channel);

                streams.put(streamId, newStream);

                final long sourceRef = route.sourceRef();
                final long sourceId = streamId;
                final String targetName = route.target().name();
                final long targetRef = route.targetRef();
                final String replyName = route.reply();
                final InetSocketAddress address = route.address();

                writer.doConnect(this, sourceRef, sourceId, targetName, targetRef,
                        replyName, replyRef, replyId, channel, address);
            }
            else
            {
                doReset(streamId);
            }
        }
        else
        {
            final Reply reply = lookupReply.apply(routableRef);
            final Reply.Path path = reply.consume(streamId);

            if (path != null)
            {
                final Target target = path.target();
                final SocketChannel channel = path.channel();
                final MessageHandler newStream = streamFactory.newStream(streamId, target, channel);
                streams.put(streamId, newStream);

                newStream.onMessage(BeginFW.TYPE_ID, buffer, index, length);
            }
            else
            {
                new WriterException().doThrow(ex -> doReset(streamId));
            }
        }
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

    public void doBegin(
        long sourceId,
        long sourceRef,
        long replyId,
        long replyRef)
    {
        final MessageHandler stream = streams.get(sourceId);

        if (stream != null)
        {
            final BeginFW begin = beginRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(sourceId)
                .routableRef(sourceRef)
                .replyId(replyId)
                .replyRef(replyRef)
                .build();

            stream.onMessage(BeginFW.TYPE_ID, writeBuffer, begin.offset(), begin.length());
        }
        else
        {
            doReset(sourceId);
        }
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
