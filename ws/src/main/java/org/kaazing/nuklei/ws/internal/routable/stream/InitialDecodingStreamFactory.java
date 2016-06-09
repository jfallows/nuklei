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
package org.kaazing.nuklei.ws.internal.routable.stream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.kaazing.nuklei.ws.internal.types.stream.WsFrameFW.STATUS_NORMAL_CLOSURE;
import static org.kaazing.nuklei.ws.internal.types.stream.WsFrameFW.STATUS_PROTOCOL_ERROR;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_BYTE;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_SHORT;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.kaazing.nuklei.ws.internal.routable.Route;
import org.kaazing.nuklei.ws.internal.routable.Source;
import org.kaazing.nuklei.ws.internal.routable.Target;
import org.kaazing.nuklei.ws.internal.router.Router;
import org.kaazing.nuklei.ws.internal.types.OctetsFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.ResetFW;
import org.kaazing.nuklei.ws.internal.types.stream.WindowFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsFrameFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;

public final class InitialDecodingStreamFactory
{
    private static final byte[] HANDSHAKE_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(UTF_8);
    private static final int DECODE_OVERHEAD_MINIMUM = 6;

    private final MessageDigest sha1 = initSHA1();

    private final HttpBeginFW httpBeginRO = new HttpBeginFW();
    private final HttpDataFW httpDataRO = new HttpDataFW();
    private final HttpEndFW httpEndRO = new HttpEndFW();

    private final WsFrameFW wsFrameRO = new WsFrameFW();

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
        private final long httpSourceId;

        private long replyId;

        private Target target;
        private long targetId;

        private InitialDecodingStream(
            Route route,
            long streamId)
        {
            this.route = route;
            this.httpSourceId = streamId;
        }

        private void handleStream(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case HttpBeginFW.TYPE_ID:
                onHttpBegin(buffer, index, length);
                break;
            case HttpDataFW.TYPE_ID:
                onData(buffer, index, length);
                break;
            case HttpEndFW.TYPE_ID:
                onEnd(buffer, index, length);
                break;
            default:
                // ignore
                break;
            }
        }

        private void onHttpBegin(
            DirectBuffer buffer,
            int index,
            int length)
        {
            httpBeginRO.wrap(buffer, index, index + length);

            // TODO: need lightweight approach (start)
            final Map<String, String> headers = new LinkedHashMap<>();
            httpBeginRO.headers().forEach(header ->
            {
                final String name = header.name().asString();
                final String value = header.value().asString();
                headers.merge(name, value, (v1, v2) -> String.format("%s, %s", v1, v2));
            });

            final String version = headers.get("sec-websocket-version");
            final String key = headers.get("sec-websocket-key");
            final String protocols = headers.get("sec-websocket-protocol");
            // TODO: need lightweight approach (end)

            final long streamId = httpBeginRO.streamId();
            final long routableRef = httpBeginRO.routableRef();
            final long httpReplyId = httpBeginRO.replyId();
            final long httpReplyRef = httpBeginRO.replyRef();

            final Route.Option resolved = route.resolve(protocols);

            if (resolved != null && key != null && "13".equals(version))
            {
                final String httpReplyName = resolved.reply();
                final String wsProtocol = resolved.protocol();
                final Target wsTarget = resolved.target();
                final long wsTargetRef = resolved.targetRef();
                final long wsTargetId = route.newTargetId();

                final long wsReplyRef = ~wsTargetRef;
                final long wsReplyId = ~wsTargetId;

                sha1.reset();
                sha1.update(key.getBytes(US_ASCII));
                byte[] digest = sha1.digest(HANDSHAKE_GUID);
                final Encoder encoder = Base64.getEncoder();
                String wsHandshakeHash = new String(encoder.encode(digest), US_ASCII);

                router.doRegisterReplyEncoder(wsTarget.name(), wsReplyRef, wsReplyId,
                        httpReplyName, httpReplyRef, httpReplyId, routableRef, streamId, wsProtocol, wsHandshakeHash);

                wsTarget.doWsBegin(wsTargetRef, wsTargetId, wsReplyRef, wsReplyId, wsProtocol);
                wsTarget.addThrottle(wsTargetId, this::handleThrottle);

                this.target = wsTarget;
                this.targetId = wsTargetId;
                this.replyId = httpReplyId;
            }
            else
            {
                final Target httpReplyTo = route.replyTo();

                httpReplyTo.doHttpBegin(httpReplyId, httpReplyRef, streamId, routableRef,
                        hs -> hs.item(h -> h.name(":status").value("404")));

                httpReplyTo.doHttpEnd(httpReplyId);

                source.replaceStream(streamId, this::skipToEnd);
            }

            source.doWindow(httpSourceId, initialWindowSize);
        }

        private void onData(
            DirectBuffer buffer,
            int index,
            int length)
        {
            httpDataRO.wrap(buffer, index, index + length);

            final OctetsFW payload = httpDataRO.payload();

            processPayload(payload);
        }

        private void onEnd(
            DirectBuffer buffer,
            int index,
            int length)
        {
            httpEndRO.wrap(buffer, index, index + length);
            final long streamId = httpEndRO.streamId();

            target.doWsEnd(targetId, STATUS_NORMAL_CLOSURE);

            source.removeStream(streamId);
            target.removeThrottle(targetId);
        }

        private int processPayload(
            final OctetsFW httpPayload)
        {
            final DirectBuffer buffer = httpPayload.buffer();
            final int offset = httpPayload.offset() + SIZE_OF_BYTE;
            final int limit = httpPayload.limit();

            int bytesWritten = 0;
            for (int nextOffset = offset; nextOffset < limit; nextOffset = wsFrameRO.limit())
            {
                wsFrameRO.wrap(buffer, nextOffset, limit);

                if (wsFrameRO.mask() && wsFrameRO.maskingKey() != 0L)
                {
                    final int maskingKey = wsFrameRO.maskingKey();
                    final DirectBuffer payload = wsFrameRO.payload();

                    switch (wsFrameRO.opcode())
                    {
                    case 1: // TEXT
                        bytesWritten += target.doWsData(targetId, 0x81, maskingKey, payload);
                        break;
                    case 2: // BINARY
                        bytesWritten += target.doWsData(targetId, 0x82, maskingKey, payload);
                        break;
                    case 8: // CLOSE
                        final short status = payload.capacity() >= SIZE_OF_SHORT ? payload.getShort(0) : STATUS_NORMAL_CLOSURE;
                        target.doWsEnd(targetId, status);
                        break;
                    default:
                        throw new IllegalStateException("not yet implemented");
                    }
                }
                else
                {
                    // TODO: inform Routable.target that protocol error has occurred (cleanup)
                    // RFC-6455, section 6.1, step 5
                    // client frames MUST be masked (with non-zero masking key)
                    route.replyTo().doWsEnd(replyId, STATUS_PROTOCOL_ERROR);
                }
            }

            return bytesWritten;
        }

        private void skipToEnd(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            if (msgTypeId == HttpDataFW.TYPE_ID)
            {
                httpDataRO.wrap(buffer, index, index + length);
                final long sourceId = httpDataRO.streamId();

                source.doWindow(sourceId, length);
            }
            else if (msgTypeId == HttpEndFW.TYPE_ID)
            {
                httpEndRO.wrap(buffer, index, index + length);
                final long sourceId = httpEndRO.streamId();

                source.replaceStream(sourceId, this::handleStream);
            }
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

            final int wsUpdate = windowRO.update();
            final int httpUpdate = wsUpdate + DECODE_OVERHEAD_MINIMUM;

            source.doWindow(httpSourceId, httpUpdate);
        }

        private void processReset(
            DirectBuffer buffer,
            int index,
            int length)
        {
            resetRO.wrap(buffer, index, length);

            source.doReset(httpSourceId);
        }
    }

    private static MessageDigest initSHA1()
    {
        try
        {
            return MessageDigest.getInstance("SHA-1");
        }
        catch (Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
            return null;
        }
    }
}
