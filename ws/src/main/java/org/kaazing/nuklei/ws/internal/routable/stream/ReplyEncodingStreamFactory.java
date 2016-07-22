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

import org.kaazing.nuklei.ws.internal.routable.Source;
import org.kaazing.nuklei.ws.internal.routable.Target;
import org.kaazing.nuklei.ws.internal.types.HeaderFW;
import org.kaazing.nuklei.ws.internal.types.ListFW;
import org.kaazing.nuklei.ws.internal.types.stream.ResetFW;
import org.kaazing.nuklei.ws.internal.types.stream.WindowFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndFW;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.MessageHandler;

public final class ReplyEncodingStreamFactory
{
    private static final int ENCODE_OVERHEAD_MAXIMUM = 14;

    private final WsBeginFW wsBeginRO = new WsBeginFW();
    private final WsDataFW wsDataRO = new WsDataFW();
    private final WsEndFW wsEndRO = new WsEndFW();

    private final WindowFW windowRO = new WindowFW();
    private final ResetFW resetRO = new ResetFW();

    private final Source source;

    public ReplyEncodingStreamFactory(
        Source source)
    {
        this.source = source;
    }

    public MessageHandler newStream(
        Target target,
        long targetRef,
        long targetId,
        long targetReplyRef,
        long targetReplyId,
        String protocol,
        String handshakeHash)
    {
        return new ReplyEncodingStream(target, targetRef, targetId,
                targetReplyRef, targetReplyId, protocol, handshakeHash)::handleStream;
    }

    private final class ReplyEncodingStream
    {
        private final Target target;
        private final long httpTargetRef;
        private final long httpTargetId;
        private final long httpTargetReplyRef;
        private final long httpTargetReplyId;
        private final String protocol;
        private final String handshakeHash;

        private long wsSourceId;

        private ReplyEncodingStream(
            final Target target,
            final long targetRef,
            final long targetId,
            final long targetReplyRef,
            final long targetReplyId,
            final String protocol,
            final String handshakeHash)
        {
            this.target = target;
            this.httpTargetRef = targetRef;
            this.httpTargetId = targetId;
            this.httpTargetReplyRef = targetReplyRef;
            this.httpTargetReplyId = targetReplyId;
            this.protocol = protocol;
            this.handshakeHash = handshakeHash;
        }

        private void handleStream(
            int msgTypeId,
            DirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case WsBeginFW.TYPE_ID:
                onWsBegin(buffer, index, length);
                break;
            case WsDataFW.TYPE_ID:
                onWsData(buffer, index, length);
                break;
            case WsEndFW.TYPE_ID:
                onWsEnd(buffer, index, length);
                break;
            default:
                // ignore
                break;
            }
        }

        private void onWsBegin(
            DirectBuffer buffer,
            int index,
            int length)
        {
            wsBeginRO.wrap(buffer, index, index + length);
            this.wsSourceId = wsBeginRO.streamId();

            target.doHttpBegin(httpTargetId, httpTargetRef, httpTargetReplyId, httpTargetReplyRef, this::setHttpHeaders);
            target.addThrottle(httpTargetId, this::handleThrottle);
        }

        private void onWsData(
            DirectBuffer buffer,
            int index,
            int length)
        {
            wsDataRO.wrap(buffer, index, index + length);

            target.doHttpData(httpTargetId, wsDataRO.flags(), wsDataRO.payload());
        }

        private void onWsEnd(
            DirectBuffer buffer,
            int index,
            int length)
        {
            wsEndRO.wrap(buffer, index, index + length);

            target.doHttpEnd(httpTargetId);
            target.removeThrottle(httpTargetId);
            source.removeStream(wsSourceId);
        }

        private void setHttpHeaders(
            ListFW.Builder<HeaderFW.Builder, HeaderFW> headers)
        {

            headers.item(h -> h.name(":status").value("101"));
            headers.item(h -> h.name("upgrade").value("websocket"));
            headers.item(h -> h.name("connection").value("upgrade"));
            headers.item(h -> h.name("sec-websocket-accept").value(handshakeHash));

            // TODO: auto-exclude header if value is null
            final String wsProtocol = wsBeginRO.protocol().asString();
            final String negotiated = wsProtocol != null ? wsProtocol : protocol;
            if (negotiated != null)
            {
                headers.item(h -> h.name("sec-websocket-protocol").value(negotiated));
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

            final int httpUpdate = windowRO.update();
            final int wsUpdate = httpUpdate - ENCODE_OVERHEAD_MAXIMUM;

            source.doWindow(wsSourceId, wsUpdate);
        }

        private void processReset(
            DirectBuffer buffer,
            int index,
            int length)
        {
            resetRO.wrap(buffer, index, length);

            source.doReset(wsSourceId);
        }
    }
}
