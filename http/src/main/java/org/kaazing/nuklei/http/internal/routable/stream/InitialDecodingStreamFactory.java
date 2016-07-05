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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.nuklei.http.internal.routable.Route;
import org.kaazing.nuklei.http.internal.routable.Source;
import org.kaazing.nuklei.http.internal.routable.Target;
import org.kaazing.nuklei.http.internal.router.Router;
import org.kaazing.nuklei.http.internal.types.OctetsFW;
import org.kaazing.nuklei.http.internal.types.stream.BeginFW;
import org.kaazing.nuklei.http.internal.types.stream.DataFW;
import org.kaazing.nuklei.http.internal.types.stream.EndFW;
import org.kaazing.nuklei.http.internal.types.stream.ResetFW;
import org.kaazing.nuklei.http.internal.types.stream.WindowFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class InitialDecodingStreamFactory
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

    private final Router router;
    private final Source source;

    private final int initialWindowSize;

    public InitialDecodingStreamFactory(
        Router router,
        Source source)
    {
        this.router = router;
        this.source = source;
        this.initialWindowSize = 8192; // TODO: configure
    }

    public MessageHandler newStream(
        Route route,
        long streamId)
    {
        return new InitialDecodingStream(route, streamId)::handleStream;
    }

    private final class InitialDecodingStream
    {
        private final Route route;
        private final long sourceId;

        private long sourceRef;
        private long replyId;
        private long replyRef;

        private DecoderState decoderState;
        private Target target;
        private long targetId;

        private InitialDecodingStream(
            Route route,
            long streamId)
        {
            this.route = route;
            this.sourceId = streamId;
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
            DirectBuffer buffer,
            int index,
            int length)
        {
            beginRO.wrap(buffer, index, index + length);

            this.sourceRef = beginRO.routableRef();
            this.replyId = beginRO.replyId();
            this.replyRef = beginRO.replyRef();

            source.doWindow(sourceId, initialWindowSize);
        }

        private void onData(
            DirectBuffer buffer,
            int index,
            int length)
        {
            dataRO.wrap(buffer, index, index + length);

            final OctetsFW payload = dataRO.payload();
            final int limit = payload.limit();

            loop:
            for (int offset = payload.offset() + 1; offset < limit;)
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
            DirectBuffer buffer,
            int index,
            int length)
        {
            endRO.wrap(buffer, index, index + length);
            final long streamId = endRO.streamId();

            // TODO: httpEnd if request not fully decoded

            decoderState = DecoderState.END;

            source.removeStream(streamId);
            target.removeThrottle(targetId);
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
            String[] lines = payload.getStringWithoutLengthUtf8(offset, endOfHeadersAt - offset).split("\r\n");
            String[] start = lines[0].split("\\s+");

            Pattern versionPattern = Pattern.compile("HTTP/1\\.(\\d)");
            Matcher versionMatcher = versionPattern.matcher(start[2]);
            if (!versionMatcher.matches())
            {
                errorResponse("HTTP/1.1 505 HTTP Version Not Supported\r\n\r\n");
            }
            else
            {
                URI requestURI = URI.create(start[1]);

                Map<String, String> headers = new LinkedHashMap<>();
                headers.put(":scheme", "http");
                headers.put(":method", start[0]);
                headers.put(":path", requestURI.getPath());

                String host = null;
                String upgrade = null;
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

                    if ("host".equals(name))
                    {
                        host = value;
                    }
                    else if ("upgrade".equals(name))
                    {
                        upgrade = value;
                    }

                    headers.put(name, value);
                }
                // TODO: replace with lightweight approach (end)

                if (host == null || requestURI.getUserInfo() != null)
                {
                    errorResponse("HTTP/1.1 400 Bad Request\r\n\r\n");
                }
                else
                {
                    final Route.Option resolved = route.resolve(headers);
                    final String replyName = resolved.reply();
                    final long httpTargetRef = resolved.targetRef();
                    final Target httpTarget = resolved.target();
                    final long httpTargetId = route.newTargetId();

                    final long httpReplyRef = ~httpTargetRef;
                    final long httpReplyId = ~httpTargetId;

                    router.doRegisterReplyEncoder(httpTarget.name(), httpReplyRef, httpReplyId,
                            replyName, replyRef, replyId, sourceRef, sourceId);

                    httpTarget.doHttpBegin(httpTargetId, httpTargetRef, httpReplyId, httpReplyRef,
                            l -> l.iterate(headers.entrySet(), e -> l.item(i -> i.name(e.getKey()).value(e.getValue()))));
                    httpTarget.addThrottle(httpTargetId, this::handleThrottle);

                    this.target = httpTarget;
                    this.targetId = httpTargetId;
                    this.replyId = httpReplyId;

                    // TODO: wait for 101 first
                    if (upgrade != null)
                    {
                        decoderState = DecoderState.UPGRADED;
                    }
                    else
                    {
                        decoderState = DecoderState.BODY;
                    }
                }
            }

            return endOfHeadersAt;
        }

        private int decodeHttpData(
            DirectBuffer payload,
            int offset,
            int limit)
        {
            target.doHttpData(targetId, payload, offset, limit - offset);
            return limit;
        }

        private int decodeHttpDataAfterUpgrade(
            DirectBuffer payload,
            int offset,
            int limit)
        {
            target.doData(targetId, payload, offset, limit - offset);
            return limit;
        }

        private int decodeHttpEnd(
            DirectBuffer payload,
            int offset,
            int limit)
        {
            target.doHttpEnd(targetId);
            return limit;
        }

        private void errorResponse(
            String payloadChars)
        {
            final Target replyTo = route.replyTo();

            // TODO: replace with connection pool (start)
            replyTo.doBegin(replyRef, replyId, sourceRef, sourceId);
            // TODO: replace with connection pool (end)

            DirectBuffer payload = new UnsafeBuffer(payloadChars.getBytes(StandardCharsets.UTF_8));
            replyTo.doData(replyId, payload, 0, payload.capacity());

            decoderState = DecoderState.IDLE;
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
