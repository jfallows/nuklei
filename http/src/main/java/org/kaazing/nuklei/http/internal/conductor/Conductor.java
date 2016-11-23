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
package org.kaazing.nuklei.http.internal.conductor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.broadcast.BroadcastTransmitter;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.http.internal.Context;
import org.kaazing.nuklei.http.internal.router.Router;
import org.kaazing.nuklei.http.internal.types.OctetsFW;
import org.kaazing.nuklei.http.internal.types.control.BindFW;
import org.kaazing.nuklei.http.internal.types.control.BoundFW;
import org.kaazing.nuklei.http.internal.types.control.ErrorFW;
import org.kaazing.nuklei.http.internal.types.control.HttpRouteExFW;
import org.kaazing.nuklei.http.internal.types.control.RejectFW;
import org.kaazing.nuklei.http.internal.types.control.RejectedFW;
import org.kaazing.nuklei.http.internal.types.control.RouteFW;
import org.kaazing.nuklei.http.internal.types.control.RoutedFW;
import org.kaazing.nuklei.http.internal.types.control.UnbindFW;
import org.kaazing.nuklei.http.internal.types.control.UnboundFW;
import org.kaazing.nuklei.http.internal.types.control.UnrejectFW;
import org.kaazing.nuklei.http.internal.types.control.UnrejectedFW;
import org.kaazing.nuklei.http.internal.types.control.UnrouteFW;
import org.kaazing.nuklei.http.internal.types.control.UnroutedFW;

@Reaktive
public final class Conductor implements Nukleus
{
    private static final Map<String, String> EMPTY_HEADERS = Collections.emptyMap();

    private final BindFW bindRO = new BindFW();
    private final UnbindFW unbindRO = new UnbindFW();
    private final RouteFW routeRO = new RouteFW();
    private final UnrouteFW unrouteRO = new UnrouteFW();
    private final RejectFW rejectRO = new RejectFW();
    private final UnrejectFW unrejectRO = new UnrejectFW();

    private final HttpRouteExFW httpRouteExRO = new HttpRouteExFW();

    private final ErrorFW.Builder errorRW = new ErrorFW.Builder();
    private final BoundFW.Builder boundRW = new BoundFW.Builder();
    private final UnboundFW.Builder unboundRW = new UnboundFW.Builder();
    private final RoutedFW.Builder routedRW = new RoutedFW.Builder();
    private final UnroutedFW.Builder unroutedRW = new UnroutedFW.Builder();
    private final RejectedFW.Builder rejectedRW = new RejectedFW.Builder();
    private final UnrejectedFW.Builder unrejectedRW = new UnrejectedFW.Builder();

    private final RingBuffer conductorCommands;
    private final BroadcastTransmitter conductorResponses;
    private final AtomicBuffer sendBuffer;

    private Router router;

    public Conductor(
        Context context)
    {
        this.conductorCommands = context.conductorCommands();
        this.conductorResponses = context.conductorResponses();
        this.sendBuffer = new UnsafeBuffer(new byte[context.maxControlResponseLength()]);
    }

    public void setRouter(
        Router router)
    {
        this.router = router;
    }

    @Override
    public int process()
    {
        return conductorCommands.read(this::handleCommand);
    }

    @Override
    public String name()
    {
        return "conductor";
    }

    public void onErrorResponse(long correlationId)
    {
        ErrorFW errorRO = errorRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                 .correlationId(correlationId)
                                 .build();

        conductorResponses.transmit(errorRO.typeId(), errorRO.buffer(), errorRO.offset(), errorRO.length());
    }

    public void onBoundResponse(
        long correlationId,
        long referenceId)
    {
        BoundFW boundRO = boundRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                 .correlationId(correlationId)
                                 .referenceId(referenceId)
                                 .build();

        conductorResponses.transmit(boundRO.typeId(), boundRO.buffer(), boundRO.offset(), boundRO.length());
    }

    public void onUnboundResponse(
        long correlationId)
    {
        UnboundFW unboundRO = unboundRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                       .correlationId(correlationId)
                                       .build();

        conductorResponses.transmit(unboundRO.typeId(), unboundRO.buffer(), unboundRO.offset(), unboundRO.length());
    }

    public void onRoutedResponse(
        long correlationId)
    {
        RoutedFW routedRO = routedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                    .correlationId(correlationId)
                                    .build();

        conductorResponses.transmit(routedRO.typeId(), routedRO.buffer(), routedRO.offset(), routedRO.length());
    }

    public void onUnroutedResponse(
        long correlationId)
    {
        UnroutedFW unroutedRO = unroutedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                          .correlationId(correlationId)
                                          .build();

        conductorResponses.transmit(unroutedRO.typeId(), unroutedRO.buffer(), unroutedRO.offset(), unroutedRO.length());
    }

    public void onRejectedResponse(
        long correlationId)
    {
        RejectedFW rejectedRO = rejectedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                          .correlationId(correlationId)
                                          .build();

        conductorResponses.transmit(rejectedRO.typeId(), rejectedRO.buffer(), rejectedRO.offset(), rejectedRO.length());
    }

    public void onUnrejectedResponse(
        long correlationId)
    {
        UnrejectedFW unrejectedRO = unrejectedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                                .correlationId(correlationId)
                                                .build();

        conductorResponses.transmit(unrejectedRO.typeId(), unrejectedRO.buffer(), unrejectedRO.offset(), unrejectedRO.length());
    }

    private void handleCommand(int msgTypeId, DirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case BindFW.TYPE_ID:
            handleBindCommand(buffer, index, length);
            break;
        case UnbindFW.TYPE_ID:
            handleUnbindCommand(buffer, index, length);
            break;
        case RouteFW.TYPE_ID:
            handleRouteCommand(buffer, index, length);
            break;
        case UnrouteFW.TYPE_ID:
            handleUnrouteCommand(buffer, index, length);
            break;
        case RejectFW.TYPE_ID:
            handleRejectCommand(buffer, index, length);
            break;
        case UnrejectFW.TYPE_ID:
            handleUnrejectCommand(buffer, index, length);
            break;
        default:
            // ignore unrecognized commands (forwards compatible)
            break;
        }
    }

    private void handleBindCommand(DirectBuffer buffer, int index, int length)
    {
        bindRO.wrap(buffer, index, index + length);

        final long correlationId = bindRO.correlationId();
        final byte kind = bindRO.kind();

        router.doBind(correlationId, kind);
    }

    private void handleUnbindCommand(DirectBuffer buffer, int index, int length)
    {
        unbindRO.wrap(buffer, index, index + length);

        final long correlationId = unbindRO.correlationId();
        final long referenceId = unbindRO.referenceId();

        router.doUnbind(correlationId, referenceId);
    }

    private void handleRouteCommand(DirectBuffer buffer, int index, int length)
    {
        routeRO.wrap(buffer, index, index + length);

        final long correlationId = routeRO.correlationId();
        final String source = routeRO.source().asString();
        final long sourceRef = routeRO.sourceRef();
        final String target = routeRO.target().asString();
        final long targetRef = routeRO.targetRef();
        final OctetsFW extension = routeRO.extension();

        if (extension.length() == 0)
        {
            router.doRoute(correlationId, source, sourceRef, target, targetRef, EMPTY_HEADERS);
        }
        else
        {
            final HttpRouteExFW routeEx = extension.get(httpRouteExRO::wrap);
            final Map<String, String> headers = new LinkedHashMap<>();
            routeEx.headers().forEach(h -> headers.put(h.name().asString(), h.value().asString()));

            router.doRoute(correlationId, source, sourceRef, target, targetRef, headers);
        }
    }

    private void handleUnrouteCommand(DirectBuffer buffer, int index, int length)
    {
        unrouteRO.wrap(buffer, index, index + length);

        final long correlationId = unrouteRO.correlationId();
        final String source = unrouteRO.source().asString();
        final long sourceRef = unrouteRO.sourceRef();
        final String target = unrouteRO.target().asString();
        final long targetRef = unrouteRO.targetRef();
        final OctetsFW extension = unrouteRO.extension();

        if (extension.length() == 0)
        {
            router.doUnroute(correlationId, source, sourceRef, target, targetRef, EMPTY_HEADERS);
        }
        else
        {
            final HttpRouteExFW routeEx = extension.get(httpRouteExRO::wrap);
            final Map<String, String> headers = new LinkedHashMap<>();
            routeEx.headers().forEach(h -> headers.put(h.name().asString(), h.value().asString()));

            router.doUnroute(correlationId, source, sourceRef, target, targetRef, headers);
        }
    }

    private void handleRejectCommand(DirectBuffer buffer, int index, int length)
    {
        rejectRO.wrap(buffer, index, index + length);

        final long correlationId = rejectRO.correlationId();
        final String source = rejectRO.source().asString();
        final long sourceRef = rejectRO.sourceRef();
        final String target = rejectRO.target().asString();
        final long targetRef = rejectRO.targetRef();
        final OctetsFW extension = rejectRO.extension();

        if (extension.length() == 0)
        {
            router.doReject(correlationId, source, sourceRef, target, targetRef, EMPTY_HEADERS);
        }
        else
        {
            final HttpRouteExFW routeEx = extension.get(httpRouteExRO::wrap);
            final Map<String, String> headers = new LinkedHashMap<>();
            routeEx.headers().forEach(h -> headers.put(h.name().asString(), h.value().asString()));

            router.doReject(correlationId, source, sourceRef, target, targetRef, headers);
        }
    }

    private void handleUnrejectCommand(DirectBuffer buffer, int index, int length)
    {
        unrejectRO.wrap(buffer, index, index + length);

        final long correlationId = unrejectRO.correlationId();
        final String source = unrejectRO.source().asString();
        final long sourceRef = unrejectRO.sourceRef();
        final String target = unrejectRO.target().asString();
        final long targetRef = unrejectRO.targetRef();
        final OctetsFW extension = unrejectRO.extension();

        if (extension.length() == 0)
        {
            router.doUnreject(correlationId, source, sourceRef, target, targetRef, EMPTY_HEADERS);
        }
        else
        {
            final HttpRouteExFW routeEx = extension.get(httpRouteExRO::wrap);
            final Map<String, String> headers = new LinkedHashMap<>();
            routeEx.headers().forEach(h -> headers.put(h.name().asString(), h.value().asString()));

            router.doUnreject(correlationId, source, sourceRef, target, targetRef, headers);
        }
    }
}
