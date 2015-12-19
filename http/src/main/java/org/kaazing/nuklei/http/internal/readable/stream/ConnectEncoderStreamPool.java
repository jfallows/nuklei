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
package org.kaazing.nuklei.http.internal.readable.stream;

import static org.kaazing.nuklei.http.internal.types.stream.Types.TYPE_ID_BEGIN;
import static org.kaazing.nuklei.http.internal.types.stream.Types.TYPE_ID_DATA;
import static org.kaazing.nuklei.http.internal.types.stream.Types.TYPE_ID_END;

import java.util.function.Consumer;

import org.kaazing.nuklei.http.internal.readable.ReadableProxy;
import org.kaazing.nuklei.http.internal.types.stream.BeginFW;
import org.kaazing.nuklei.http.internal.types.stream.DataFW;
import org.kaazing.nuklei.http.internal.types.stream.EndFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpEndFW;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class ConnectEncoderStreamPool
{
    private final HttpBeginFW httpBeginRO = new HttpBeginFW();
    private final HttpDataFW httpDataRO = new HttpDataFW();
    private final HttpEndFW httpEndRO = new HttpEndFW();

    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final AtomicBuffer atomicBuffer;
    private final AtomicCounter streamsConnected;

    public ConnectEncoderStreamPool(
        int capacity,
        AtomicBuffer atomicBuffer,
        AtomicCounter streamsConnected)
    {
        this.atomicBuffer = atomicBuffer;
        this.streamsConnected = streamsConnected;
    }

    public MessageHandler acquire(
        long destinationRef,
        RingBuffer sourceRoute,
        RingBuffer destinationRoute,
        ReadableProxy destination,
        Consumer<MessageHandler> released)
    {
        return new ConnectEncoderStream(released, destinationRef, sourceRoute, destinationRoute, destination);
    }

    private final class ConnectEncoderStream implements MessageHandler
    {
        private final Consumer<MessageHandler> cleanup;
        private final long destinationRef;
        private final RingBuffer sourceRoute;
        private final RingBuffer destinationRoute;
        private final ReadableProxy destination;

        private long destinationInitialStreamId;

        private ConnectEncoderStream(
            Consumer<MessageHandler> cleanup,
            long destinationRef,
            RingBuffer sourceRoute,
            RingBuffer destinationRoute,
            ReadableProxy destination)
        {
            this.cleanup = cleanup;
            this.destinationRef = destinationRef;
            this.sourceRoute = sourceRoute;
            this.destinationRoute = destinationRoute;
            this.destination = destination;
        }

        @Override
        public void onMessage(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case TYPE_ID_BEGIN:
                onBegin(buffer, index, length);
                break;
            case TYPE_ID_DATA:
                onData(buffer, index, length);
                break;
            case TYPE_ID_END:
                onEnd(buffer, index, length);
                break;
            }
        }

        private void onBegin(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpBeginRO.wrap(buffer, index, index + length);

            long sourceInitialStreamId = httpBeginRO.streamId();

            streamsConnected.increment();

            // TODO: start of connection pool section
            // connection pool for reference id (for http origin instead?)
            this.destinationInitialStreamId = (streamsConnected.get() << 1L) | 0x0000000000000001L;

            BeginFW begin = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                   .streamId(destinationInitialStreamId)
                                   .referenceId(destinationRef)
                                   .build();

            if (!destinationRoute.write(begin.typeId(), begin.buffer(), begin.offset(), begin.length()))
            {
                 throw new IllegalStateException("could not write to ring buffer");
            }
            // TODO: end of connection pool section

            destination.doRegisterDecoder(destinationInitialStreamId, sourceInitialStreamId, sourceRoute);

            // TODO

            final DataFW data = dataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                      .streamId(destinationInitialStreamId)
                                      .build();

            if (!destinationRoute.write(data.typeId(), data.buffer(), data.offset(), data.length()))
            {
                 throw new IllegalStateException("could not write to ring buffer");
            }
        }

        private void onData(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpDataRO.wrap(buffer, index, index + length);

            // TODO: decode httpBegin, httpData, httpEnd
        }

        private void onEnd(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpEndRO.wrap(buffer, index, index + length);

            // TODO: httpReset if necessary?
        }
    }

}
