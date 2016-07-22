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
package org.kaazing.nuklei.tcp.internal.conductor;

import static org.kaazing.nuklei.tcp.internal.util.IpUtil.inetAddress;
import static org.kaazing.nuklei.tcp.internal.util.IpUtil.ipAddress;

import java.net.InetSocketAddress;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.router.Router;
import org.kaazing.nuklei.tcp.internal.types.control.BindFW;
import org.kaazing.nuklei.tcp.internal.types.control.BoundFW;
import org.kaazing.nuklei.tcp.internal.types.control.ErrorFW;
import org.kaazing.nuklei.tcp.internal.types.control.PrepareFW;
import org.kaazing.nuklei.tcp.internal.types.control.PreparedFW;
import org.kaazing.nuklei.tcp.internal.types.control.RouteFW;
import org.kaazing.nuklei.tcp.internal.types.control.RoutedFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnbindFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnboundFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnprepareFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnpreparedFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnrouteFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnroutedFW;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.broadcast.BroadcastTransmitter;
import org.agrona.concurrent.ringbuffer.RingBuffer;

@Reaktive
public final class Conductor implements Nukleus
{
    private static final int SEND_BUFFER_CAPACITY = 1024; // TODO: Configuration and Context

    private final BindFW bindRO = new BindFW();
    private final UnbindFW unbindRO = new UnbindFW();
    private final PrepareFW prepareRO = new PrepareFW();
    private final UnprepareFW unprepareRO = new UnprepareFW();
    private final RouteFW routeRO = new RouteFW();
    private final UnrouteFW unrouteRO = new UnrouteFW();

    private final ErrorFW.Builder errorRW = new ErrorFW.Builder();
    private final BoundFW.Builder boundRW = new BoundFW.Builder();
    private final UnboundFW.Builder unboundRW = new UnboundFW.Builder();
    private final PreparedFW.Builder preparedRW = new PreparedFW.Builder();
    private final UnpreparedFW.Builder unpreparedRW = new UnpreparedFW.Builder();
    private final RoutedFW.Builder routedRW = new RoutedFW.Builder();
    private final UnroutedFW.Builder unroutedRW = new UnroutedFW.Builder();

    private final RingBuffer conductorCommands;

    private final BroadcastTransmitter conductorResponses;
    private final AtomicBuffer sendBuffer;

    private Router router;

    public Conductor(Context context)
    {
        this.conductorCommands = context.conductorCommands();
        this.conductorResponses = context.conductorResponses();

        this.sendBuffer = new UnsafeBuffer(new byte[SEND_BUFFER_CAPACITY]);
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
        long correlationId,
        long referenceId)
    {
        UnboundFW unboundRO = unboundRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                       .correlationId(correlationId)
                                       .referenceId(referenceId)
                                       .build();

        conductorResponses.transmit(unboundRO.typeId(), unboundRO.buffer(), unboundRO.offset(), unboundRO.length());
    }

    public void onPreparedResponse(
        long correlationId,
        long referenceId)
    {
        PreparedFW preparedRO = preparedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                          .correlationId(correlationId)
                                          .referenceId(referenceId)
                                          .build();

        conductorResponses.transmit(preparedRO.typeId(), preparedRO.buffer(), preparedRO.offset(), preparedRO.length());
    }

    public void onUnpreparedResponse(
        long correlationId,
        long referenceId)
    {
        UnpreparedFW unpreparedRO = unpreparedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                                .correlationId(correlationId)
                                                .referenceId(referenceId)
                                                .build();

        conductorResponses.transmit(
            unpreparedRO.typeId(), unpreparedRO.buffer(), unpreparedRO.offset(), unpreparedRO.length());
    }

    public void onRoutedResponse(
        long correlationId,
        String source,
        long sourceRef,
        String target,
        long targetRef,
        String reply,
        InetSocketAddress address)
    {
        RoutedFW routedRO = routedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                    .correlationId(correlationId)
                                    .source(source)
                                    .sourceRef(sourceRef)
                                    .target(target)
                                    .targetRef(targetRef)
                                    .reply(reply)
                                    .address(a -> ipAddress(address, a::ipv4Address, a::ipv6Address))
                                    .port(address.getPort())
                                    .build();

        conductorResponses.transmit(routedRO.typeId(), routedRO.buffer(), routedRO.offset(), routedRO.length());
    }

    public void onUnroutedResponse(
        long correlationId,
        String source,
        long sourceRef,
        String target,
        long targetRef,
        String reply,
        InetSocketAddress address)
    {
        UnroutedFW unroutedRO = unroutedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                          .correlationId(correlationId)
                                          .source(source)
                                          .sourceRef(sourceRef)
                                          .target(target)
                                          .targetRef(targetRef)
                                          .reply(reply)
                                          .address(a -> ipAddress(address, a::ipv4Address, a::ipv6Address))
                                          .port(address.getPort())
                                          .build();

        conductorResponses.transmit(unroutedRO.typeId(), unroutedRO.buffer(), unroutedRO.offset(), unroutedRO.length());
    }

    private void handleCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case BindFW.TYPE_ID:
            handleBindCommand(buffer, index, length);
            break;
        case UnbindFW.TYPE_ID:
            handleUnbindCommand(buffer, index, length);
            break;
        case PrepareFW.TYPE_ID:
            handlePrepareCommand(buffer, index, length);
            break;
        case UnprepareFW.TYPE_ID:
            handleUnprepareCommand(buffer, index, length);
            break;
        case RouteFW.TYPE_ID:
            handleRouteCommand(buffer, index, length);
            break;
        case UnrouteFW.TYPE_ID:
            handleUnrouteCommand(buffer, index, length);
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

        router.doBind(correlationId);
    }

    private void handleUnbindCommand(DirectBuffer buffer, int index, int length)
    {
        unbindRO.wrap(buffer, index, index + length);

        final long correlationId = unbindRO.correlationId();
        final long referenceId = unbindRO.referenceId();

        router.doUnbind(correlationId, referenceId);
    }

    private void handlePrepareCommand(DirectBuffer buffer, int index, int length)
    {
        prepareRO.wrap(buffer, index, index + length);

        final long correlationId = prepareRO.correlationId();

        router.doPrepare(correlationId);
    }

    private void handleUnprepareCommand(DirectBuffer buffer, int index, int length)
    {
        unprepareRO.wrap(buffer, index, index + length);

        final long correlationId = unprepareRO.correlationId();
        final long referenceId = unprepareRO.referenceId();

        router.doUnprepare(correlationId, referenceId);
    }

    private void handleRouteCommand(DirectBuffer buffer, int index, int length)
    {
        routeRO.wrap(buffer, index, index + length);

        final long correlationId = routeRO.correlationId();
        final String source = routeRO.source().asString();
        final long sourceRef = routeRO.sourceRef();
        final String target = routeRO.target().asString();
        final long targetRef = routeRO.targetRef();
        final String reply = routeRO.reply().asString();
        final InetSocketAddress address = new InetSocketAddress(inetAddress(routeRO.address()), routeRO.port());

        router.doRoute(correlationId, source, sourceRef, target, targetRef, reply, address);
    }

    private void handleUnrouteCommand(DirectBuffer buffer, int index, int length)
    {
        unrouteRO.wrap(buffer, index, index + length);

        final long correlationId = unrouteRO.correlationId();
        final String source = unrouteRO.source().asString();
        final long sourceRef = unrouteRO.sourceRef();
        final String target = unrouteRO.target().asString();
        final long targetRef = unrouteRO.targetRef();
        final String reply = unrouteRO.reply().asString();
        final InetSocketAddress address = new InetSocketAddress(inetAddress(unrouteRO.address()), unrouteRO.port());

        router.doUnroute(correlationId, source, sourceRef, target, targetRef, reply, address);
    }
}
