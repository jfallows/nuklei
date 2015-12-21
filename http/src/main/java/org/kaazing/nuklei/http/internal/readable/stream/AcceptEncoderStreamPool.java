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




import org.kaazing.nuklei.http.internal.types.stream.DataFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpEndFW;




import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class AcceptEncoderStreamPool
{
    private final HttpBeginFW httpBeginRO = new HttpBeginFW();
    private final HttpDataFW httpDataRO = new HttpDataFW();
    private final HttpEndFW httpEndRO = new HttpEndFW();

    private final DataFW.Builder dataRW = new DataFW.Builder();

    private final AtomicBuffer atomicBuffer;

    public AcceptEncoderStreamPool(
        int capacity,
        AtomicBuffer atomicBuffer)
    {
        this.atomicBuffer = atomicBuffer;
    }

    public MessageHandler acquire(
        long sourceReplyStreamId,
        RingBuffer sourceRoute,
        Consumer<MessageHandler> released)
    {
        return new AcceptEncoderStream(released, sourceReplyStreamId, sourceRoute);
    }

    private final class AcceptEncoderStream implements MessageHandler
    {
        private final Consumer<MessageHandler> cleanup;
        private final long sourceReplyStreamId;
        private final RingBuffer sourceRoute;

        public AcceptEncoderStream(
            Consumer<MessageHandler> cleanup,
            long sourceReplyStreamId,
            RingBuffer sourceRoute)
        {
            this.cleanup = cleanup;
            this.sourceReplyStreamId = sourceReplyStreamId;
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

            StringBuilder request = new StringBuilder();
            StringBuilder headers = new StringBuilder();
            httpBeginRO.headers().forEach((header) ->
            {
                String name = header.name().asString();
                String value = header.value().asString();

                if (":status".equals(name))
                {
                    request.append("HTTP/1.1 ").append(value).append(" OK\r\n");
                }
                else
                {
                    headers.append(name).append(": ").append(value).append("\r\n");
                }
            });
            request.append(headers).append("\r\n");

            dataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                  .streamId(sourceReplyStreamId);

            String payloadChars = request.toString();
            int payloadOffset = dataRW.payloadOffset();
            int payloadLength = atomicBuffer.putStringWithoutLengthUtf8(payloadOffset, payloadChars);

            final DataFW data = dataRW.payloadLength(payloadLength)
                                      .build();

            if (!sourceRoute.write(data.typeId(), data.buffer(), data.offset(), data.length()))
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

            // TODO
        }

        private void onEnd(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpEndRO.wrap(buffer, index, index + length);

            // TODO

            // release
            cleanup.accept(this);
        }
    }

}
