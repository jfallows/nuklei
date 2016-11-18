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

import static org.agrona.BitUtil.SIZE_OF_BYTE;
import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.kaazing.nuklei.ws.internal.util.BufferUtil.xor;

import java.util.function.Consumer;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.ws.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.ws.internal.types.Flyweight;
import org.kaazing.nuklei.ws.internal.types.HttpHeaderFW;
import org.kaazing.nuklei.ws.internal.types.ListFW;
import org.kaazing.nuklei.ws.internal.types.OctetsFW;
import org.kaazing.nuklei.ws.internal.types.stream.BeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.DataFW;
import org.kaazing.nuklei.ws.internal.types.stream.EndFW;
import org.kaazing.nuklei.ws.internal.types.stream.FrameFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginExFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginExFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataExFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndExFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsFrameFW;

public final class Target implements Nukleus
{
    private final FrameFW frameRO = new FrameFW();

    private final WsFrameFW.Builder wsFrameRW = new WsFrameFW.Builder();

    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final WsBeginExFW.Builder wsBeginExRW = new WsBeginExFW.Builder();
    private final WsDataExFW.Builder wsDataExRW = new WsDataExFW.Builder();
    private final WsEndExFW.Builder wsEndExRW = new WsEndExFW.Builder();

    private final HttpBeginExFW.Builder httpBeginExRW = new HttpBeginExFW.Builder();

    private final String name;
    private final StreamsLayout layout;
    private final AtomicBuffer writeBuffer;

    private final RingBuffer streamsBuffer;
    private final RingBuffer throttleBuffer;
    private final Long2ObjectHashMap<MessageHandler> throttles;

    public Target(
        String name,
        StreamsLayout layout,
        AtomicBuffer writeBuffer)
    {
        this.name = name;
        this.layout = layout;
        this.writeBuffer = writeBuffer;
        this.streamsBuffer = layout.streamsBuffer();
        this.throttleBuffer = layout.throttleBuffer();
        this.throttles = new Long2ObjectHashMap<>();
    }

    @Override
    public int process()
    {
        return throttleBuffer.read(this::handleRead);
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

    public void addThrottle(
        long streamId,
        MessageHandler throttle)
    {
        throttles.put(streamId, throttle);
    }

    public void removeThrottle(
        long streamId)
    {
        throttles.remove(streamId);
    }

    private void handleRead(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        frameRO.wrap(buffer, index, index + length);

        final long streamId = frameRO.streamId();
        // TODO: use Long2ObjectHashMap.getOrDefault(long, T) instead
        final MessageHandler throttle = throttles.get(streamId);

        if (throttle != null)
        {
            throttle.onMessage(msgTypeId, buffer, index, length);
        }
    }

    public void doWsBegin(
        long targetId,
        long targetRef,
        long correlationId,
        String protocol)
    {
        final BeginFW begin = beginRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .referenceId(targetRef)
                .streamId(targetId)
                .correlationId(correlationId)
                .extension(b -> b.set(visitWsBeginEx(protocol)))
                .build();

        streamsBuffer.write(begin.typeId(), begin.buffer(), begin.offset(), begin.length());
    }

    public int doWsData(
        long targetId,
        int flags,
        int maskKey,
        DirectBuffer payload)
    {
        final int capacity = payload.capacity();
        final DataFW data = dataRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(targetId)
                .payload(p -> p.set(payload, 0, capacity).set((b, o, l) -> xor(b, o, o + capacity, maskKey)))
                .extension(b -> b.set(visitWsDataEx(flags)))
                .build();

        streamsBuffer.write(data.typeId(), data.buffer(), data.offset(), data.length());

        return data.length();
    }

    public void doWsEnd(
        long targetId,
        int status)
    {
        final EndFW end = endRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(targetId)
                .extension(b -> b.set(visitWsEndEx(status)))
                .build();

        streamsBuffer.write(end.typeId(), end.buffer(), end.offset(), end.length());
    }

    public void doHttpBegin(
        long targetId,
        long targetRef,
        long correlationId,
        Consumer<ListFW.Builder<HttpHeaderFW.Builder, HttpHeaderFW>> mutator)
    {
        BeginFW begin = beginRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(targetId)
                .referenceId(targetRef)
                .correlationId(correlationId)
                .extension(b -> b.set(visitHttpBeginEx(mutator)))
                .build();

        streamsBuffer.write(begin.typeId(), begin.buffer(), begin.offset(), begin.length());
    }

    public void doHttpData(
        long targetId,
        OctetsFW payload,
        int flagsAndOpcode)
    {
        WsFrameFW wsFrame = wsFrameRW.wrap(writeBuffer, SIZE_OF_LONG + SIZE_OF_BYTE, writeBuffer.capacity())
                .payload(payload.buffer(), payload.offset() + SIZE_OF_BYTE, payload.length() - SIZE_OF_BYTE)
                .flagsAndOpcode(flagsAndOpcode)
                .build();

        DataFW data = dataRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(targetId)
                .payload(p -> p.set(wsFrame.buffer(), wsFrame.offset(), wsFrame.length()))
                .extension(e -> e.reset())
                .build();

        streamsBuffer.write(data.typeId(), data.buffer(), data.offset(), data.length());
    }

    public void doHttpEnd(
        long targetId)
    {
        EndFW end = endRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(targetId)
                .extension(e -> e.reset())
                .build();

        streamsBuffer.write(end.typeId(), end.buffer(), end.offset(), end.length());
    }

    private Flyweight.Builder.Visitor visitWsBeginEx(
        String protocol)
    {
        return (buffer, offset, limit) ->
            wsBeginExRW.wrap(buffer, offset, limit)
                       .protocol(protocol)
                       .build()
                       .length();
    }

    private Flyweight.Builder.Visitor visitWsDataEx(
        int flags)
    {
        return (buffer, offset, limit) ->
            wsDataExRW.wrap(buffer, offset, limit)
                      .flags((byte) flags)
                      .build()
                      .length();
    }

    private Flyweight.Builder.Visitor visitWsEndEx(
        int status)
    {
        return (buffer, offset, limit) ->
            wsEndExRW.wrap(buffer, offset, limit)
                     .code((short) status)
                     .build()
                     .length();
    }

    private Flyweight.Builder.Visitor visitHttpBeginEx(
        Consumer<ListFW.Builder<HttpHeaderFW.Builder, HttpHeaderFW>> headers)
    {
        return (buffer, offset, limit) ->
            httpBeginExRW.wrap(buffer, offset, limit)
                         .headers(headers)
                         .build()
                         .length();
    }

}
