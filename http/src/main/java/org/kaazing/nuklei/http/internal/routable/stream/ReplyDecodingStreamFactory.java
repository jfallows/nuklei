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
package org.kaazing.nuklei.http.internal.routable.stream;

import static org.kaazing.nuklei.http.internal.util.BufferUtil.limitOfBytes;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.nuklei.http.internal.routable.Source;
import org.kaazing.nuklei.http.internal.routable.Target;
import org.kaazing.nuklei.http.internal.types.OctetsFW;
import org.kaazing.nuklei.http.internal.types.stream.BeginFW;
import org.kaazing.nuklei.http.internal.types.stream.DataFW;
import org.kaazing.nuklei.http.internal.types.stream.EndFW;
import org.kaazing.nuklei.http.internal.types.stream.ResetFW;
import org.kaazing.nuklei.http.internal.types.stream.WindowFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;

public final class ReplyDecodingStreamFactory
{
    private static final byte[] CRLFCRLF_BYTES = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private enum DecoderState
    {
        IDLE, HEADERS, UPGRADED, BODY, TRAILERS, END
    }

    private final BeginFW beginRO = new BeginFW();
    private final DataFW dataRO = new DataFW();
    private final EndFW endRO = new EndFW();

    private final WindowFW windowRO = new WindowFW();
    private final ResetFW resetRO = new ResetFW();

    private final Source source;
    private final int initialWindowSize;

    public ReplyDecodingStreamFactory(
        Source source)
    {
        this.source = source;
        this.initialWindowSize = 8192; // TODO: configure
    }

    public MessageHandler newStream(
        long sourceId,
        Target target,
        long targetRef,
        long targetId,
        long replyRef,
        long replyId)
    {
        return new ReplyDecodingStream(sourceId, target, targetRef, targetId, replyRef, replyId)::handleStream;
    }

    private final class ReplyDecodingStream
    {
        private final long sourceId;
        private final Target httpTarget;
        private final long httpTargetRef;
        private final long httpTargetId;
        private final long httpReplyRef;
        private final long httpReplyId;

        private DecoderState decoderState;

        private ReplyDecodingStream(
            long sourceId,
            Target target,
            long targetRef,
            long targetId,
            long replyRef,
            long replyId)
        {
            this.sourceId = sourceId;
            this.httpTarget = target;
            this.httpTargetRef = targetRef;
            this.httpTargetId = targetId;
            this.httpReplyRef = replyRef;
            this.httpReplyId = replyId;
            this.decoderState = DecoderState.IDLE;
        }

        private void handleStream(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case BeginFW.TYPE_ID:
                onBegin(buffer, index, length);
                break;
            case DataFW.TYPE_ID:
                onData(buffer, index, length);
                break;
            case EndFW.TYPE_ID:
                onEnd(buffer, index, length);
                break;
            default:
                // ignore
                break;
            }
        }

        private void onBegin(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            beginRO.wrap(buffer, index, index + length);

            source.doWindow(sourceId, initialWindowSize);

            // connection pool setup, connection established
        }

        private void onData(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            dataRO.wrap(buffer, index, index + length);

            final OctetsFW payload = dataRO.payload();
            final int limit = payload.limit();

            loop:
            for (int offset = payload.offset(); offset < limit;)
            {
                switch (decoderState)
                {
                case IDLE:
                    offset = decodeHttpBegin(buffer, offset, limit);
                    break;
                case HEADERS:
                    // TODO: partial headers
                    break;
                case UPGRADED:
                    offset = decodeHttpDataAfterUpgrade(buffer, offset, limit);
                    break;
                case BODY:
                    offset = decodeHttpData(buffer, offset, limit);
                    break;
                case TRAILERS:
                    offset = decodeHttpEnd(buffer, offset, limit);
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
            final long streamId = endRO.streamId();

            // TODO: httpEnd if response not fully decoded

            decoderState = DecoderState.END;

            source.removeStream(streamId);
            httpTarget.removeThrottle(httpTargetId);
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

            // TODO: replace with lightweight approach (start)
            Map<String, String> headers = new LinkedHashMap<>();
            int startOfBlockAt = offset + 1;
            int lengthOfBlock = endOfHeadersAt - CRLFCRLF_BYTES.length - startOfBlockAt;
            String block = payload.getStringWithoutLengthUtf8(startOfBlockAt, lengthOfBlock);
            String[] lines = block.split("\r\n");
            String[] start = lines[0].split("\\s+");

            headers.put(":status", start[1]);

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

                headers.put(name, value);
            }
            // TODO: replace with lightweight approach (end)

            httpTarget.doHttpBegin(httpTargetId, httpTargetRef, httpReplyId, httpReplyRef,
                    l -> l.iterate(headers.entrySet(), e -> l.item(i -> i.name(e.getKey()).value(e.getValue()))));
            httpTarget.addThrottle(httpTargetId, this::handleThrottle);

            decoderState = DecoderState.BODY;

            return endOfHeadersAt;
        }

        private int decodeHttpData(
            DirectBuffer payload,
            int offset,
            int limit)
        {
            httpTarget.doHttpData(httpTargetId, payload, offset, limit - offset);
            return limit;
        }

        private int decodeHttpDataAfterUpgrade(
            DirectBuffer payload,
            int offset,
            int limit)
        {
            httpTarget.doData(httpTargetId, payload, offset, limit - offset);
            return limit;
        }

        private int decodeHttpEnd(
            DirectBuffer payload,
            int offset,
            int limit)
        {
            httpTarget.doHttpEnd(httpTargetId);
            return limit;
        }

        private void handleThrottle(
            int msgTypeId,
            DirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case WindowFW.TYPE_ID:
                processWindow(buffer, index, length);
                break;
            case ResetFW.TYPE_ID:
                processReset(buffer, index, length);
                break;
            default:
                // ignore
                break;
            }
        }

        private void processWindow(
            DirectBuffer buffer,
            int index,
            int length)
        {
            windowRO.wrap(buffer, index, length);

            final int update = windowRO.update();

            source.doWindow(sourceId, update + headerSize(update));
        }

        private int headerSize(
            int update)
        {
            // TODO: chunk overhead
            return 0;
        }

        private void processReset(
            DirectBuffer buffer,
            int index,
            int length)
        {
            resetRO.wrap(buffer, index, length);

            source.doReset(sourceId);
        }
    }
}
