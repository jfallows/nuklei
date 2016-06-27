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
package org.kaazing.nuklei.ws.internal;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

import java.util.Map;

import org.kaazing.nuklei.ws.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class WsStreams
{
    private final HttpBeginFW.Builder httpBeginRW = new HttpBeginFW.Builder();
    private final HttpDataFW.Builder httpDataRW = new HttpDataFW.Builder();
    private final HttpEndFW.Builder httpEndRW = new HttpEndFW.Builder();

    private final WsBeginFW.Builder wsBeginRW = new WsBeginFW.Builder();
    private final WsDataFW.Builder wsDataRW = new WsDataFW.Builder();
    private final WsEndFW.Builder wsEndRW = new WsEndFW.Builder();

    private final StreamsLayout captureStreams;
    private final StreamsLayout routeStreams;
    private final RingBuffer captureBuffer;
    private final RingBuffer routeBuffer;
    private final AtomicBuffer atomicBuffer;

    WsStreams(
        Context context,
        String source,
        String target)
    {
        this.captureStreams = new StreamsLayout.Builder().streamsCapacity(context.streamsBufferCapacity())
                                                         .path(context.captureStreamsPath().apply(source))
                                                         .readonly(true)
                                                         .build();
        this.captureBuffer = this.captureStreams.streamsBuffer();

        this.routeStreams = new StreamsLayout.Builder().streamsCapacity(context.streamsBufferCapacity())
                                                       .path(context.routeStreamsPath().apply(target, source))
                                                       .readonly(false)
                                                       .build();
        this.routeBuffer = this.routeStreams.streamsBuffer();

        this.atomicBuffer = new UnsafeBuffer(allocateDirect(context.maxMessageLength()).order(nativeOrder()));
    }

    public void close()
    {
        captureStreams.close();
        routeStreams.close();
    }

    public boolean httpBegin(
        long streamId,
        long routableRef,
        Map<String, String> headers)
    {
        httpBeginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                   .streamId(streamId)
                   .routableRef(routableRef);

        for (Map.Entry<String, String> header : headers.entrySet())
        {
            String name = header.getKey();
            String value = header.getValue();
            httpBeginRW.headers(headersRW -> headersRW.item(itemRW -> itemRW.name(name).value(value)));
        }

        HttpBeginFW httpBegin = httpBeginRW.build();

        return captureBuffer.write(httpBegin.typeId(), httpBegin.buffer(), httpBegin.offset(), httpBegin.length());
    }

    public boolean httpData(
        long streamId,
        DirectBuffer buffer,
        int offset,
        int length)
    {
        HttpDataFW httpData = httpDataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                        .streamId(streamId)
                                        .payload(b-> b.set(buffer, offset, length))
                                        .build();

        return captureBuffer.write(httpData.typeId(), httpData.buffer(), httpData.offset(), httpData.length());
    }

    public boolean httpEnd(
        long streamId)
    {
        HttpEndFW httpEnd = httpEndRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                     .streamId(streamId)
                                     .build();

        return captureBuffer.write(httpEnd.typeId(), httpEnd.buffer(), httpEnd.offset(), httpEnd.length());
    }

    public boolean wsBegin(
        long streamId,
        long routableRef,
        String protocol)
    {
        WsBeginFW wsBegin = wsBeginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                     .streamId(streamId)
                                     .routableRef(routableRef)
                                     .protocol(protocol)
                                     .build();

        return captureBuffer.write(wsBegin.typeId(), wsBegin.buffer(), wsBegin.offset(), wsBegin.length());
    }

    public boolean wsData(
        long streamId,
        DirectBuffer buffer,
        int offset,
        int length)
    {
        WsDataFW wsData = wsDataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                  .streamId(streamId)
                                  .payload(b-> b.set(buffer, offset, length))
                                  .build();

        return captureBuffer.write(wsData.typeId(), wsData.buffer(), wsData.offset(), wsData.length());
    }

    public boolean wsEnd(
        long streamId)
    {
        WsEndFW wsEnd = wsEndRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                               .streamId(streamId)
                               .build();

        return captureBuffer.write(wsEnd.typeId(), wsEnd.buffer(), wsEnd.offset(), wsEnd.length());
    }

    public int read(MessageHandler handler)
    {
        return routeBuffer.read(handler);
    }
}
