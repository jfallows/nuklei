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
package org.kaazing.nuklei.echo.internal.readable.stream;

import java.util.function.Consumer;

import org.kaazing.nuklei.echo.internal.types.stream.BeginFW;
import org.kaazing.nuklei.echo.internal.types.stream.DataFW;
import org.kaazing.nuklei.echo.internal.types.stream.EndFW;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class InitialStreamPool
{
    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final BeginFW beginRO = new BeginFW();
    private final DataFW dataRO = new DataFW();
    private final EndFW endRO = new EndFW();

    private final AtomicBuffer atomicBuffer;

    public InitialStreamPool(
        int capacity,
        AtomicBuffer atomicBuffer)
    {
        this.atomicBuffer = atomicBuffer;
    }

    public MessageHandler acquire(
        long sourceInitialStreamId,
        long sourceReplyStreamId,
        RingBuffer sourceRoute,
        Consumer<MessageHandler> released)
    {
        return new InitialStream(released, sourceInitialStreamId, sourceReplyStreamId, sourceRoute);
    }

    public final class InitialStream implements MessageHandler
    {
        private final Consumer<MessageHandler> cleanup;
        private final long initialStreamId;
        private final long replyStreamId;
        private final RingBuffer routeBuffer;

        public InitialStream(
            Consumer<MessageHandler> cleanup,
            long initialStreamId,
            long replyStreamId,
            RingBuffer routeBuffer)
        {
            this.cleanup = cleanup;
            this.initialStreamId = initialStreamId;
            this.replyStreamId = replyStreamId;
            this.routeBuffer = routeBuffer;
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
            case BeginFW.TYPE_ID:
                beginRO.wrap(buffer, index, index + length);

                // distinct begin
                BeginFW begin = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                       .streamId(replyStreamId)
                                       .referenceId(initialStreamId)
                                       .build();

                if (!routeBuffer.write(begin.typeId(), begin.buffer(), begin.offset(), begin.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }
                break;

            case DataFW.TYPE_ID:
                dataRO.wrap(buffer, index, index + length);

                // reflect data with updated stream id
                atomicBuffer.putBytes(0, buffer, index, length);
                DataFW data = dataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                        .streamId(replyStreamId)
                        .payload(dataRO.payload())
                        .build();

                if (!routeBuffer.write(data.typeId(), data.buffer(), data.offset(), data.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }
                break;

            case EndFW.TYPE_ID:
                endRO.wrap(buffer, index, index + length);

                // reflect end with updated stream id
                atomicBuffer.putBytes(0, buffer, index, length);
                EndFW end = endRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .streamId(replyStreamId)
                                 .build();

                if (!routeBuffer.write(end.typeId(), end.buffer(), end.offset(), end.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }

                // release
                cleanup.accept(this);

                break;
            }
        }
    }

}
