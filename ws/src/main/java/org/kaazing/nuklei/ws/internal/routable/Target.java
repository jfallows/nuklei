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

import static org.kaazing.nuklei.ws.internal.util.BufferUtil.xor;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_BYTE;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_LONG;

import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.ws.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.ws.internal.types.HeaderFW;
import org.kaazing.nuklei.ws.internal.types.ListFW;
import org.kaazing.nuklei.ws.internal.types.OctetsFW;
import org.kaazing.nuklei.ws.internal.types.stream.FrameFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsFrameFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Target implements Nukleus
{
    private final FrameFW frameRO = new FrameFW();

    private final HttpBeginFW.Builder httpBeginRW = new HttpBeginFW.Builder();
    private final HttpDataFW.Builder httpDataRW = new HttpDataFW.Builder();
    private final HttpEndFW.Builder httpEndRW = new HttpEndFW.Builder();

    private final WsFrameFW.Builder wsFrameRW = new WsFrameFW.Builder();

    private final WsBeginFW.Builder wsBeginRW = new WsBeginFW.Builder();
    private final WsDataFW.Builder wsDataRW = new WsDataFW.Builder();
    private final WsEndFW.Builder wsEndRW = new WsEndFW.Builder();

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
        final MessageHandler throttle = throttles.get(streamId);

        if (throttle != null)
        {
            throttle.onMessage(msgTypeId, buffer, index, length);
        }
    }

    public void doWsBegin(
        long routableRef,
        long streamId,
        long replyRef,
        long replyId,
        String protocol)
    {
        WsBeginFW wsBegin = wsBeginRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .routableRef(routableRef)
                .streamId(streamId)
                .replyRef(replyRef)
                .replyId(replyId)
                .protocol(protocol)
                .build();

        streamsBuffer.write(wsBegin.typeId(), wsBegin.buffer(), wsBegin.offset(), wsBegin.length());
    }

    public int doWsData(
        long streamId,
        int flags,
        int maskKey,
        DirectBuffer payload)
    {
        WsDataFW wsData = wsDataRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId)
                .payload(p -> p.set(payload, 0, payload.capacity()).set((b, o, l) -> xor(b, o, l, maskKey)))
                .flags(flags)
                .build();

        streamsBuffer.write(wsData.typeId(), wsData.buffer(), wsData.offset(), wsData.length());

        return wsData.length();
    }

    public void doWsEnd(
        long streamId,
        short status)
    {
        WsEndFW wsEnd = wsEndRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId)
                .code(status)
                .build();

        streamsBuffer.write(wsEnd.typeId(), wsEnd.buffer(), wsEnd.offset(), wsEnd.length());
    }

    public void doHttpBegin(
        long streamId,
        long routableRef,
        long replyId,
        long replyRef,
        Consumer<ListFW.Builder<HeaderFW.Builder, HeaderFW>> mutator)
    {
        HttpBeginFW httpBegin = httpBeginRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId)
                .routableRef(routableRef)
                .replyId(replyId)
                .replyRef(replyRef)
                .headers(mutator)
                .build();

        streamsBuffer.write(httpBegin.typeId(), httpBegin.buffer(), httpBegin.offset(), httpBegin.length());
    }

    public void doHttpData(
        long streamId,
        int flagsAndOpcode,
        OctetsFW payload)
    {
        WsFrameFW wsFrame = wsFrameRW.wrap(writeBuffer, SIZE_OF_LONG + SIZE_OF_BYTE, writeBuffer.capacity())
                .payload(payload.buffer(), payload.offset() + SIZE_OF_BYTE, payload.length() - SIZE_OF_BYTE)
                .flagsAndOpcode(flagsAndOpcode)
                .build();

        HttpDataFW httpData = httpDataRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId)
                .payload(p -> p.set(wsFrame.buffer(), wsFrame.offset(), wsFrame.length()))
                .build();

        streamsBuffer.write(httpData.typeId(), httpData.buffer(), httpData.offset(), httpData.length());
    }

    public void doHttpEnd(
        long streamId)
    {
        HttpEndFW httpEnd = httpEndRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(streamId)
                .build();

        streamsBuffer.write(httpEnd.typeId(), httpEnd.buffer(), httpEnd.offset(), httpEnd.length());
    }

}
