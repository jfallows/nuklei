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
package org.kaazing.nuklei.tcp.internal;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataFW;
import org.kaazing.nuklei.tcp.internal.types.stream.EndFW;

public final class TcpStreams
{
    private static final int MAX_SEND_LENGTH = 1024; // TODO: Configuration and Context

    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final StreamsLayout captureStreams;
    private final StreamsLayout routeStreams;
    private final RingBuffer captureBuffer;
    private final RingBuffer routeBuffer;
    private final AtomicBuffer atomicBuffer;

    TcpStreams(
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
                                                       .readonly(true)
                                                       .build();
        this.routeBuffer = this.routeStreams.streamsBuffer();

        this.atomicBuffer = new UnsafeBuffer(allocateDirect(MAX_SEND_LENGTH).order(nativeOrder()));
    }

    public void close()
    {
        captureStreams.close();
        routeStreams.close();
    }

    public void begin(
        long streamId,
        long referenceId,
        long correlationId)
    {
        BeginFW beginRO = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .streamId(streamId)
                                 .referenceId(referenceId)
                                 .correlationId(correlationId)
                                 .build();

        if (!captureBuffer.write(beginRO.typeId(), beginRO.buffer(), beginRO.offset(), beginRO.length()))
        {
            throw new IllegalStateException("unable to write to captureStreams");
        }
    }

    public void data(
        long streamId,
        DirectBuffer buffer,
        int offset,
        int length)
    {
        DataFW dataRO = dataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                              .streamId(streamId)
                              .payload(o -> o.set(buffer, offset, length))
                              .build();

        if (!captureBuffer.write(dataRO.typeId(), dataRO.buffer(), dataRO.offset(), dataRO.length()))
        {
            throw new IllegalStateException("unable to write to captureStreams");
        }
    }

    public void end(
        long streamId)
    {
        EndFW endRO = endRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                           .streamId(streamId)
                           .build();

        if (!captureBuffer.write(endRO.typeId(), endRO.buffer(), endRO.offset(), endRO.length()))
        {
            throw new IllegalStateException("unable to write to captureStreams");
        }
    }

    public int read(
        MessageHandler handler)
    {
        return routeBuffer.read(handler);
    }
}
