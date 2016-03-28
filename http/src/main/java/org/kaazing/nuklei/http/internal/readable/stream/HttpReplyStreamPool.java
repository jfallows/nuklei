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
import static org.kaazing.nuklei.http.internal.util.BufferUtil.limitOfBytes;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.nuklei.http.internal.types.stream.BeginFW;
import org.kaazing.nuklei.http.internal.types.stream.DataFW;
import org.kaazing.nuklei.http.internal.types.stream.EndFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpEndFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class HttpReplyStreamPool
{
    private static final byte[] CRLFCRLF_BYTES = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private enum DecoderState
    {
        IDLE, HEADERS, BODY, TRAILERS, END
    }

    private final BeginFW beginRO = new BeginFW();
    private final DataFW dataRO = new DataFW();
    private final EndFW endRO = new EndFW();

    private final HttpBeginFW.Builder httpBeginRW = new HttpBeginFW.Builder();
    private final HttpDataFW.Builder httpDataRW = new HttpDataFW.Builder();
    private final HttpEndFW.Builder httpEndRW = new HttpEndFW.Builder();

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

        private DecoderState decoderState;
        private long sourceReplyStreamId;

        private HttpReplyStream(
            Consumer<MessageHandler> cleanup,
            long sourceInitialStreamId,
            RingBuffer sourceRoute)
        {
            this.cleanup = cleanup;
            this.sourceInitialStreamId = sourceInitialStreamId;
            this.sourceRoute = sourceRoute;
            this.decoderState = DecoderState.IDLE;
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

            // connection pool setup, connection established
        }

        private void onData(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            dataRO.wrap(buffer, index, index + length);

            final DirectBuffer payload = dataRO.payload();
            final int limit = payload.capacity();

            loop:
            for (int offset = 0; offset < limit;)
            {
                switch (decoderState)
                {
                case IDLE:
                    offset = decodeHttpBegin(payload, offset, limit);
                    break;
                case HEADERS:
                    // TODO: partial headers
                    break;
                case BODY:
                    offset = decodeHttpData(payload, offset, limit);
                    break;
                case TRAILERS:
                    offset = decodeHttpEnd(payload, offset, limit);
                    break;
                case END:
                    break loop;
                }
            }
        }

        private void onEnd(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            endRO.wrap(buffer, index, index + length);

            // TODO: httpReset if necessary?

            cleanup.accept(this);

            decoderState = DecoderState.END;
        }

        private int decodeHttpBegin(
            final DirectBuffer payload,
            final int offset,
            final int limit)
        {
            final int endOfHeadersAt = limitOfBytes(payload, offset, limit, CRLFCRLF_BYTES);
            if (endOfHeadersAt == -1)
            {
                throw new IllegalStateException("incomplete http headers");
            }

            // positive, non-zero, even source reply stream id
            this.sourceReplyStreamId = (sourceInitialStreamId << 1L);

            // TODO: replace with lightweight approach (start)
            String[] lines = payload.getStringWithoutLengthUtf8(0, endOfHeadersAt).split("\r\n");
            String[] start = lines[0].split("\\s+");

            httpBeginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                       .streamId(sourceReplyStreamId)
                       .referenceId(sourceInitialStreamId)
                       .header(":status", start[1]);

            Pattern headerPattern = Pattern.compile("([^\\s:]+)\\s*:\\s*(.*)");
            for (int i = 1; i < lines.length; i++)
            {
                Matcher headerMatcher = headerPattern.matcher(lines[i]);
                if (!headerMatcher.matches())
                {
                    throw new IllegalStateException("illegal http header syntax");
                }

                String name = headerMatcher.group(1).toLowerCase();
                String value = headerMatcher.group(2);
                httpBeginRW.header(name, value);
            }
            // TODO: replace with lightweight approach (end)

            final HttpBeginFW httpBegin = httpBeginRW.build();

            if (!sourceRoute.write(httpBegin.typeId(), httpBegin.buffer(), httpBegin.offset(), httpBegin.length()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }

            decoderState = DecoderState.BODY;

            return endOfHeadersAt;
        }

        private int decodeHttpData(
            DirectBuffer payload,
            int offset,
            int limit)
        {
            final HttpDataFW httpData = httpDataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                    .streamId(sourceReplyStreamId)
                    .payload(payload, offset, limit - offset)
                    .build();

            // TODO: http chunk flag and extension

            if (!sourceRoute.write(httpData.typeId(), httpData.buffer(), httpData.offset(), httpData.length()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }

            return limit;
        }

        private int decodeHttpEnd(
            DirectBuffer payload,
            int offset,
            int limit)
        {
            final HttpEndFW httpEnd = httpEndRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                               .streamId(sourceReplyStreamId)
                                               .build();

            // TODO: http trailers

            if (!sourceRoute.write(httpEnd.typeId(), httpEnd.buffer(), httpEnd.offset(), httpEnd.length()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }

            return limit;
        }
    }
}
