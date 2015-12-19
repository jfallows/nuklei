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

public final class AcceptDecoderStreamPool
{
    private final BeginFW.Builder beginRW = new BeginFW.Builder();

    private final BeginFW beginRO = new BeginFW();
    private final DataFW dataRO = new DataFW();
    private final EndFW endRO = new EndFW();

    private final HttpBeginFW.Builder httpBeginRW = new HttpBeginFW.Builder();
    private final HttpDataFW.Builder httpDataRW = new HttpDataFW.Builder();
    private final HttpEndFW.Builder httpEndRW = new HttpEndFW.Builder();

    private final AtomicBuffer atomicBuffer;
    private final AtomicCounter streamsAccepted;

    public AcceptDecoderStreamPool(
        int capacity,
        AtomicBuffer atomicBuffer,
        AtomicCounter streamsAccepted)
    {
        this.atomicBuffer = atomicBuffer;
        this.streamsAccepted = streamsAccepted;
    }

    public MessageHandler acquire(
        long sourceReplyStreamId,
        ReadableProxy destination,
        RingBuffer sourceRoute,
        RingBuffer destinationRoute,
        Consumer<MessageHandler> released)
    {
        return new AcceptDecoderStream(released, sourceReplyStreamId, destination, sourceRoute, destinationRoute);
    }

    private final class AcceptDecoderStream implements MessageHandler
    {
        private final Consumer<MessageHandler> cleanup;
        private final long sourceReplyStreamId;
        private final ReadableProxy destination;
        private final RingBuffer sourceRoute;
        private final RingBuffer destinationRoute;

        private long destinationInitialStreamId;

        public AcceptDecoderStream(
            Consumer<MessageHandler> cleanup,
            long sourceReplyStreamId,
            ReadableProxy destination,
            RingBuffer sourceRoute,
            RingBuffer destinationRoute)
        {
            this.cleanup = cleanup;
            this.sourceReplyStreamId = sourceReplyStreamId;
            this.destination = destination;
            this.sourceRoute = sourceRoute;
            this.destinationRoute = destinationRoute;
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
            beginRO.wrap(buffer, index, index + length);

            final long sourceInitialStreamId = beginRO.streamId();

            final BeginFW begin = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                         .streamId(sourceReplyStreamId)
                                         .referenceId(sourceInitialStreamId)
                                         .build();

            if (!sourceRoute.write(begin.typeId(), begin.buffer(), begin.offset(), begin.length()))
            {
                 throw new IllegalStateException("could not write to ring buffer");
            }
        }

        private void onData(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            dataRO.wrap(buffer, index, index + length);

            // TODO: decode httpBegin, httpData, httpEnd

            // assume fully decoded HTTP request

            // positive, odd destination stream id
            this.destinationInitialStreamId = (streamsAccepted.increment() << 1L) | 0x0000000000000001L;

            destination.doRegisterEncoder(destinationInitialStreamId, sourceReplyStreamId, sourceRoute);

            final HttpBeginFW httpBegin = httpBeginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                                     .streamId(destinationInitialStreamId)
                                                     .build();

            // TODO: http request headers

            if (!destinationRoute.write(httpBegin.typeId(), httpBegin.buffer(), httpBegin.offset(), httpBegin.length()))
            {
                 throw new IllegalStateException("could not write to ring buffer");
            }

            final HttpDataFW httpData = httpDataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                                  .streamId(destinationInitialStreamId)
                                                  .build();

            // TODO: http chunk flag and extension

            if (!destinationRoute.write(httpData.typeId(), httpData.buffer(), httpData.offset(), httpData.length()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }

            final HttpEndFW httpEnd = httpEndRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                               .streamId(destinationInitialStreamId)
                                               .build();

            // TODO: http trailers

            if (!destinationRoute.write(httpEnd.typeId(), httpEnd.buffer(), httpEnd.offset(), httpEnd.length()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }

        }

        private void onEnd(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            endRO.wrap(buffer, index, index + length);

            // TODO: httpReset if necessary?

            // example release
            cleanup.accept(this);
        }
    }

}
