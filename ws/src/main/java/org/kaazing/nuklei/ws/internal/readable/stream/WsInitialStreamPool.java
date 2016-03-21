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
package org.kaazing.nuklei.ws.internal.readable.stream;

import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_BEGIN;
import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_DATA;
import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_END;

import java.util.function.Consumer;

import org.kaazing.nuklei.ws.internal.readable.Readable;
import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndFW;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class WsInitialStreamPool
{
    private final WsBeginFW wsBeginRO = new WsBeginFW();
    private final WsDataFW wsDataRO = new WsDataFW();
    private final WsEndFW wsEndRO = new WsEndFW();

    private final HttpBeginFW.Builder httpBeginRW = new HttpBeginFW.Builder();
    private final HttpDataFW.Builder httpDataRW = new HttpDataFW.Builder();
    private final HttpEndFW.Builder httpEndRW = new HttpEndFW.Builder();

    private final AtomicBuffer atomicBuffer;
    private final AtomicCounter streamsConnected;

    public WsInitialStreamPool(
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
        Readable destination,
        Consumer<MessageHandler> released)
    {
        return new WsInitialStream(released, destinationRef, sourceRoute, destinationRoute, destination);
    }

    private final class WsInitialStream implements MessageHandler
    {
        private final Consumer<MessageHandler> cleanup;
        private final long destinationRef;
        private final RingBuffer sourceRoute;
        private final RingBuffer destinationRoute;
        private final Readable destination;

        private long destinationInitialStreamId;

        private WsInitialStream(
            Consumer<MessageHandler> cleanup,
            long destinationRef,
            RingBuffer sourceRoute,
            RingBuffer destinationRoute,
            Readable destination)
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
            wsBeginRO.wrap(buffer, index, index + length);

            // TODO
        }

        private void onData(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            wsDataRO.wrap(buffer, index, index + length);

            // TODO
        }

        private void onEnd(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            wsEndRO.wrap(buffer, index, index + length);

            // TODO

            cleanup.accept(this);
        }
    }

}
