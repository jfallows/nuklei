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

import static org.kaazing.nuklei.ws.internal.routable.Route.protocolMatches;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.kaazing.nuklei.ws.internal.routable.Route;
import org.kaazing.nuklei.ws.internal.routable.Source;
import org.kaazing.nuklei.ws.internal.routable.Target;
import org.kaazing.nuklei.ws.internal.router.Correlation;
import org.kaazing.nuklei.ws.internal.types.HttpHeaderFW;
import org.kaazing.nuklei.ws.internal.types.ListFW;
import org.kaazing.nuklei.ws.internal.types.OctetsFW;
import org.kaazing.nuklei.ws.internal.types.stream.BeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.DataFW;
import org.kaazing.nuklei.ws.internal.types.stream.EndFW;
import org.kaazing.nuklei.ws.internal.types.stream.FrameFW;
import org.kaazing.nuklei.ws.internal.types.stream.ResetFW;
import org.kaazing.nuklei.ws.internal.types.stream.WindowFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginExFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataExFW;

public final class ServerReplyStreamFactory
{
    private static final int ENCODE_OVERHEAD_MAXIMUM = 14;

    private final FrameFW frameRO = new FrameFW();

    private final BeginFW beginRO = new BeginFW();
    private final DataFW dataRO = new DataFW();
    private final EndFW endRO = new EndFW();

    private final WindowFW windowRO = new WindowFW();
    private final ResetFW resetRO = new ResetFW();

    private final WsBeginExFW wsBeginExRO = new WsBeginExFW();
    private final WsDataExFW wsDataExRO = new WsDataExFW();

    private final Source source;
    private final LongFunction<List<Route>> supplyRoutes;
    private final LongSupplier supplyTargetId;
    private final LongFunction<Correlation> correlateReply;

    public ServerReplyStreamFactory(
        Source source,
        LongFunction<List<Route>> supplyRoutes,
        LongSupplier supplyTargetId,
        LongFunction<Correlation> correlateReply)
    {
        this.source = source;
        this.supplyRoutes = supplyRoutes;
        this.supplyTargetId = supplyTargetId;
        this.correlateReply = correlateReply;
    }

    public MessageHandler newStream()
    {
        return new ServerReplyStream()::handleStream;
    }

    private final class ServerReplyStream
    {
        private MessageHandler streamState;

        private long sourceId;

        private Target target;
        private long targetId;

        private ServerReplyStream()
        {
            this.streamState = this::beforeBegin;
        }

        private void handleStream(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            streamState.onMessage(msgTypeId, buffer, index, length);
        }

        private void beforeBegin(
            int msgTypeId,
            DirectBuffer buffer,
            int index,
            int length)
        {
            if (msgTypeId == BeginFW.TYPE_ID)
            {
                processBegin(buffer, index, length);
            }
            else
            {
                processUnexpected(buffer, index, length);
            }
        }

        private void afterBeginOrData(
            int msgTypeId,
            DirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case DataFW.TYPE_ID:
                processData(buffer, index, length);
                break;
            case EndFW.TYPE_ID:
                processEnd(buffer, index, length);
                break;
            default:
                processUnexpected(buffer, index, length);
                break;
            }
        }

        private void afterEnd(
            int msgTypeId,
            DirectBuffer buffer,
            int index,
            int length)
        {
            processUnexpected(buffer, index, length);
        }

        private void afterRejectOrReset(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            if (msgTypeId == DataFW.TYPE_ID)
            {
                dataRO.wrap(buffer, index, index + length);
                final long streamId = dataRO.streamId();

                source.doWindow(streamId, length);
            }
            else if (msgTypeId == EndFW.TYPE_ID)
            {
                endRO.wrap(buffer, index, index + length);
                final long streamId = endRO.streamId();

                source.removeStream(streamId);

                this.streamState = this::afterEnd;
            }
        }

        private void processUnexpected(
            DirectBuffer buffer,
            int index,
            int length)
        {
            frameRO.wrap(buffer, index, index + length);

            final long streamId = frameRO.streamId();

            source.doReset(streamId);

            this.streamState = this::afterRejectOrReset;
        }

        private void processBegin(
            DirectBuffer buffer,
            int index,
            int length)
        {
            beginRO.wrap(buffer, index, index + length);

            final long newSourceId = beginRO.streamId();
            final long sourceRef = beginRO.referenceId();
            final long correlationId = beginRO.correlationId();
            final OctetsFW extension = beginRO.extension();

            final List<Route> routes = supplyRoutes.apply(sourceRef);

            String protocol = null;
            if (extension.length() > 0)
            {
                final WsBeginExFW wsBeginEx = extension.get(wsBeginExRO::wrap);
                protocol = wsBeginEx.protocol().asString();
            }

            final Predicate<Route> predicate = protocolMatches(protocol);
            final Optional<Route> optional = routes.stream().filter(predicate).findFirst();

            if (optional.isPresent())
            {
                final Route route = optional.get();
                final Target newTarget = route.target();
                final long targetRef = route.targetRef();
                final long newTargetId = supplyTargetId.getAsLong();

                final Correlation correlation = correlateReply.apply(correlationId);

                if (correlation != null)
                {
                    newTarget.doHttpBegin(newTargetId, targetRef, correlation.id(), setHttpHeaders(correlation.hash()));
                    newTarget.addThrottle(newTargetId, this::handleThrottle);

                    this.sourceId = newSourceId;
                    this.target = newTarget;
                    this.targetId = newTargetId;

                    this.streamState = this::afterBeginOrData;
                }
                else
                {
                    processUnexpected(buffer, index, length);
                }
            }
            else
            {
                processUnexpected(buffer, index, length);
            }
        }

        private void processData(
            DirectBuffer buffer,
            int index,
            int length)
        {
            dataRO.wrap(buffer, index, index + length);

            int flags = 0;
            final OctetsFW extension = dataRO.extension();
            if (extension.length() > 0)
            {
                final WsDataExFW wsDataEx = extension.get(wsDataExRO::wrap);
                flags = wsDataEx.flags();
            }

            target.doHttpData(targetId, dataRO.payload(), flags);
        }

        private void processEnd(
            DirectBuffer buffer,
            int index,
            int length)
        {
            endRO.wrap(buffer, index, index + length);

            target.doHttpEnd(targetId);
            target.removeThrottle(targetId);
            source.removeStream(sourceId);
        }

        private Consumer<ListFW.Builder<HttpHeaderFW.Builder, HttpHeaderFW>> setHttpHeaders(
            String handshakeHash)
        {
            return headers ->
            {
                headers.item(h -> h.name(":status").value("101"));
                headers.item(h -> h.name("upgrade").value("websocket"));
                headers.item(h -> h.name("connection").value("upgrade"));
                headers.item(h -> h.name("sec-websocket-accept").value(handshakeHash));

                // TODO: auto-exclude header if value is null
                final OctetsFW extension = beginRO.extension();
                if (extension.length() > 0)
                {
                    final WsBeginExFW wsBeginEx = extension.get(wsBeginExRO::wrap);
                    final String wsProtocol = wsBeginEx.protocol().asString();
                    final String negotiated = wsProtocol;
                    if (negotiated != null)
                    {
                        headers.item(h -> h.name("sec-websocket-protocol").value(negotiated));
                    }
                }
            };
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
            windowRO.wrap(buffer, index, index + length);

            final int httpUpdate = windowRO.update();
            final int wsUpdate = httpUpdate - ENCODE_OVERHEAD_MAXIMUM;

            source.doWindow(sourceId, wsUpdate);
        }

        private void processReset(
            DirectBuffer buffer,
            int index,
            int length)
        {
            resetRO.wrap(buffer, index, index + length);

            source.doReset(sourceId);
        }
    }
}
