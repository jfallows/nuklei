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
package org.kaazing.nuklei.ws.internal.routable;

import java.util.function.LongFunction;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.ws.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.ws.internal.routable.stream.InitialDecodingStreamFactory;
import org.kaazing.nuklei.ws.internal.routable.stream.InitialEncodingStreamFactory;
import org.kaazing.nuklei.ws.internal.routable.stream.ReplyDecodingStreamFactory;
import org.kaazing.nuklei.ws.internal.routable.stream.ReplyEncodingStreamFactory;
import org.kaazing.nuklei.ws.internal.router.Router;
import org.kaazing.nuklei.ws.internal.types.stream.BeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.FrameFW;
import org.kaazing.nuklei.ws.internal.types.stream.ResetFW;
import org.kaazing.nuklei.ws.internal.types.stream.WindowFW;

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
    private final LongFunction<Route> lookupRoute;
    private final LongFunction<Reply> lookupReply;
    private final StreamsLayout layout;
    private final AtomicBuffer writeBuffer;
    private final RingBuffer streamsBuffer;
    private final RingBuffer throttleBuffer;
    private final Long2ObjectHashMap<MessageHandler> streams;

    private final InitialDecodingStreamFactory initialDecoderFactory;
    private final InitialEncodingStreamFactory initialEncoderFactory;
    private final ReplyDecodingStreamFactory replyDecoderFactory;
    private final ReplyEncodingStreamFactory replyEncoderFactory;

    Source(
        String fileName,
        Router router,
        LongFunction<Route> lookupRoute,
        LongFunction<Reply> lookupReply,
        StreamsLayout layout,
        AtomicBuffer writeBuffer)
    {
        this.name = String.format("sources[%s]", fileName);
        this.lookupRoute = lookupRoute;
        this.lookupReply = lookupReply;
        this.layout = layout;
        this.writeBuffer = writeBuffer;
        this.streamsBuffer = layout.streamsBuffer();
        this.throttleBuffer = layout.throttleBuffer();
        this.streams = new Long2ObjectHashMap<>();
        this.initialDecoderFactory = new InitialDecodingStreamFactory(router, this);
        this.initialEncoderFactory = new InitialEncodingStreamFactory(router, this);
        this.replyDecoderFactory = new ReplyDecodingStreamFactory(this);
        this.replyEncoderFactory = new ReplyEncodingStreamFactory(this);
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
            handleBegin(msgTypeId, buffer, index, length);
        }
        else
        {
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
        final long streamId = beginRO.streamId();
        final long routableRef = beginRO.routableRef();

        if (routableRef > 0)
        {
            final Route route = lookupRoute.apply(routableRef);
            if (route != null)
            {
                final MessageHandler newStream = route.newStream(this, streamId);
                streams.put(streamId, newStream);

                newStream.onMessage(msgTypeId, buffer, index, length);
            }
            else
            {
                doReset(streamId);
            }
        }
        else
        {
            final Reply reply = lookupReply.apply(routableRef);
            final MessageHandler newStream = (reply != null) ? reply.newStream(this, streamId) : null;
            if (newStream != null)
            {
                streams.put(streamId, newStream);

                newStream.onMessage(msgTypeId, buffer, index, length);
            }
            else
            {
                new RoutableException().doThrow(ex -> doReset(streamId));
            }
        }
    }

    public MessageHandler newInitialDecodingStream(
        Route route,
        long streamId)
    {
        return initialDecoderFactory.newStream(route, streamId);
    }

    public MessageHandler newReplyEncodingStream(
        Target target,
        long targetRef,
        long targetId,
        long targetReplyRef,
        long targetReplyId,
        String protocol,
        String handshakeHash)
    {
        return replyEncoderFactory.newStream(target, targetRef, targetId, targetReplyRef, targetReplyId, protocol, handshakeHash);
    }

    public MessageHandler newInitialEncodingStream(
        Route route,
        long streamId)
    {
        return initialEncoderFactory.newStream(route, streamId);
    }

    public MessageHandler newReplyDecodingStream(
        Target target,
        long targetRef,
        long targetId,
        String protocol,
        byte[] handshakeKey)
    {
        return replyDecoderFactory.newStream(target, targetRef, targetId, protocol, handshakeKey);
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
