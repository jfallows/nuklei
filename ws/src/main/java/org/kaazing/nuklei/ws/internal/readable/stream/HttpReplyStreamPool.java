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
package org.kaazing.nuklei.ws.internal.readable.stream;

import java.util.function.Consumer;

import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndFW;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class HttpReplyStreamPool
{
    private final HttpBeginFW httpBeginRO = new HttpBeginFW();
    private final HttpDataFW httpDataRO = new HttpDataFW();
    private final HttpEndFW httpEndRO = new HttpEndFW();

    private final WsBeginFW.Builder wsBeginRW = new WsBeginFW.Builder();
    private final WsDataFW.Builder wsDataRW = new WsDataFW.Builder();
    private final WsEndFW.Builder wsEndRW = new WsEndFW.Builder();

    private final AtomicBuffer atomicBuffer;

    public HttpReplyStreamPool(
        int capacity,
        AtomicBuffer atomicBuffer)
    {
        this.atomicBuffer = atomicBuffer;
    }

    public MessageHandler acquire(
        long sourceInitialStreamId,
        RingBuffer sourceRoute,
        Consumer<MessageHandler> released)
    {
        return new HttpReplyStream(released, sourceInitialStreamId, sourceRoute);
    }

    private final class HttpReplyStream implements MessageHandler
    {
        private final Consumer<MessageHandler> cleanup;
        private final long sourceInitialStreamId;
        private final RingBuffer sourceRoute;

        private long sourceReplyStreamId;

        private HttpReplyStream(
            Consumer<MessageHandler> cleanup,
            long sourceInitialStreamId,
            RingBuffer sourceRoute)
        {
            this.cleanup = cleanup;
            this.sourceInitialStreamId = sourceInitialStreamId;
            this.sourceRoute = sourceRoute;
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
            case HttpBeginFW.TYPE_ID:
                onBegin(buffer, index, length);
                break;
            case HttpDataFW.TYPE_ID:
                onData(buffer, index, length);
                break;
            case HttpEndFW.TYPE_ID:
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

            // TODO
        }

        private void onData(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpDataRO.wrap(buffer, index, index + length);

            // TODO
        }

        private void onEnd(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpEndRO.wrap(buffer, index, index + length);

            // TODO: httpReset if necessary?

            cleanup.accept(this);
        }
    }
}
