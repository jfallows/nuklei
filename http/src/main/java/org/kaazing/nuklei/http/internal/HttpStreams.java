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
package org.kaazing.nuklei.http.internal;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

import java.util.Map;

import org.kaazing.nuklei.http.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.http.internal.types.stream.BeginFW;
import org.kaazing.nuklei.http.internal.types.stream.DataFW;
import org.kaazing.nuklei.http.internal.types.stream.EndFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpEndFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class HttpStreams
{
    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final HttpBeginFW.Builder httpBeginRW = new HttpBeginFW.Builder();
    private final HttpDataFW.Builder httpDataRW = new HttpDataFW.Builder();
    private final HttpEndFW.Builder httpEndRW = new HttpEndFW.Builder();

    private final StreamsLayout sourceStreams;
    private final StreamsLayout targetStreams;
    private final RingBuffer sourceBuffer;
    private final RingBuffer targetBuffer;
    private final AtomicBuffer atomicBuffer;

    HttpStreams(
        Context context,
        String source,
        String target)
    {
        this.sourceStreams = new StreamsLayout.Builder().streamsCapacity(context.streamsBufferCapacity())
                                                        .path(context.captureStreamsPath().apply(source))
                                                        .readonly(true)
                                                        .build();
        this.sourceBuffer = this.sourceStreams.streamsBuffer();

        this.targetStreams = new StreamsLayout.Builder().streamsCapacity(context.streamsBufferCapacity())
                                                        .path(context.routeStreamsPath().apply(target, source))
                                                        .readonly(false)
                                                        .build();
        this.targetBuffer = this.targetStreams.streamsBuffer();

        this.atomicBuffer = new UnsafeBuffer(allocateDirect(context.maxMessageLength()).order(nativeOrder()));
    }

    public void close()
    {
        sourceStreams.close();
        targetStreams.close();
    }

    public boolean begin(
        long streamId,
        long routableRef)
    {
        BeginFW begin = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                               .streamId(streamId)
                               .routableRef(routableRef)
                               .build();

        return sourceBuffer.write(begin.typeId(), begin.buffer(), begin.offset(), begin.length());
    }

    public boolean data(
        long streamId,
        DirectBuffer buffer,
        int offset,
        int length)
    {
        DataFW data = dataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                            .streamId(streamId)
                            .payload(b-> b.set(buffer, offset, length))
                            .build();

        return sourceBuffer.write(data.typeId(), data.buffer(), data.offset(), data.length());
    }

    public boolean end(
        long streamId)
    {
        EndFW endRO = endRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                           .streamId(streamId)
                           .build();

        return sourceBuffer.write(endRO.typeId(), endRO.buffer(), endRO.offset(), endRO.length());
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

        return sourceBuffer.write(httpBegin.typeId(), httpBegin.buffer(), httpBegin.offset(), httpBegin.length());
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

        return sourceBuffer.write(httpData.typeId(), httpData.buffer(), httpData.offset(), httpData.length());
    }

    public boolean httpEnd(
        long streamId)
    {
        HttpEndFW end = httpEndRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .streamId(streamId)
                                 .build();

        return sourceBuffer.write(end.typeId(), end.buffer(), end.offset(), end.length());
    }

    public int read(
        MessageHandler handler)
    {
        return targetBuffer.read(handler);
    }
}
