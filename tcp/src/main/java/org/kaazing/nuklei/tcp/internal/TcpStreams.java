/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataFW;
import org.kaazing.nuklei.tcp.internal.types.stream.EndFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class TcpStreams
{
    private static final int MAX_SEND_LENGTH = 1024; // TODO: Configuration and Context

    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final StreamsLayout captureStreams;
    private final StreamsLayout routeStreams;
    private final RingBuffer streamsInput;
    private final RingBuffer streamsOutput;
    private final AtomicBuffer atomicBuffer;

    TcpStreams(
        Context context,
        String handler)
    {
        this.captureStreams = new StreamsLayout.Builder().streamsCapacity(context.streamsCapacity())
                                                         .streamsFile(context.captureStreamsFile().apply(handler))
                                                         .createFile(false)
                                                         .build();
        this.streamsInput = this.captureStreams.buffer();

        this.routeStreams = new StreamsLayout.Builder().streamsCapacity(context.streamsCapacity())
                                                       .streamsFile(context.routeStreamsFile().apply(handler))
                                                       .createFile(false)
                                                       .build();
        this.streamsOutput = this.routeStreams.buffer();

        this.atomicBuffer = new UnsafeBuffer(allocateDirect(MAX_SEND_LENGTH).order(nativeOrder()));
    }

    public void begin(
        long streamId,
        long referenceId)
    {
        BeginFW beginRO = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .streamId(streamId)
                                 .referenceId(referenceId)
                                 .build();

        if (!streamsInput.write(beginRO.typeId(), beginRO.buffer(), beginRO.offset(), beginRO.length()))
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
                              .payload(buffer, offset, length)
                              .build();

        if (!streamsInput.write(dataRO.typeId(), dataRO.buffer(), dataRO.offset(), dataRO.length()))
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

        if (!streamsInput.write(endRO.typeId(), endRO.buffer(), endRO.offset(), endRO.length()))
        {
            throw new IllegalStateException("unable to write to captureStreams");
        }
    }

    public int read(MessageHandler handler)
    {
        return streamsOutput.read(handler);
    }
}
