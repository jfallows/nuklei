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
package org.kaazing.nuklei.echo.internal.conductor;

import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_BIND_COMMAND;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_CAPTURE_COMMAND;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_CONNECT_COMMAND;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_PREPARE_COMMAND;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_ROUTE_COMMAND;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_UNBIND_COMMAND;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_UNCAPTURE_COMMAND;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_UNPREPARE_COMMAND;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_UNROUTE_COMMAND;

import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.echo.internal.Context;
import org.kaazing.nuklei.echo.internal.reflector.ReflectorProxy;
import org.kaazing.nuklei.echo.internal.types.control.BindFW;
import org.kaazing.nuklei.echo.internal.types.control.BindingFW;
import org.kaazing.nuklei.echo.internal.types.control.BoundFW;
import org.kaazing.nuklei.echo.internal.types.control.CaptureFW;
import org.kaazing.nuklei.echo.internal.types.control.CapturedFW;
import org.kaazing.nuklei.echo.internal.types.control.ConnectFW;
import org.kaazing.nuklei.echo.internal.types.control.ConnectedFW;
import org.kaazing.nuklei.echo.internal.types.control.ErrorFW;
import org.kaazing.nuklei.echo.internal.types.control.PreparationFW;
import org.kaazing.nuklei.echo.internal.types.control.PrepareFW;
import org.kaazing.nuklei.echo.internal.types.control.PreparedFW;
import org.kaazing.nuklei.echo.internal.types.control.RouteFW;
import org.kaazing.nuklei.echo.internal.types.control.RoutedFW;
import org.kaazing.nuklei.echo.internal.types.control.UnbindFW;
import org.kaazing.nuklei.echo.internal.types.control.UnboundFW;
import org.kaazing.nuklei.echo.internal.types.control.UncaptureFW;
import org.kaazing.nuklei.echo.internal.types.control.UncapturedFW;
import org.kaazing.nuklei.echo.internal.types.control.UnprepareFW;
import org.kaazing.nuklei.echo.internal.types.control.UnpreparedFW;
import org.kaazing.nuklei.echo.internal.types.control.UnrouteFW;
import org.kaazing.nuklei.echo.internal.types.control.UnroutedFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Conductor implements Nukleus, Consumer<ConductorResponse>
{
    private static final int SEND_BUFFER_CAPACITY = 1024; // TODO: Configuration and Context

    private final CaptureFW captureRO = new CaptureFW();
    private final UncaptureFW uncaptureRO = new UncaptureFW();
    private final RouteFW routeRO = new RouteFW();
    private final UnrouteFW unrouteRO = new UnrouteFW();
    private final BindFW bindRO = new BindFW();
    private final UnbindFW unbindRO = new UnbindFW();
    private final PrepareFW prepareRO = new PrepareFW();
    private final UnprepareFW unprepareRO = new UnprepareFW();
    private final ConnectFW connectRO = new ConnectFW();

    private final ErrorFW.Builder errorRW = new ErrorFW.Builder();
    private final CapturedFW.Builder capturedRW = new CapturedFW.Builder();
    private final UncapturedFW.Builder uncapturedRW = new UncapturedFW.Builder();
    private final RoutedFW.Builder routedRW = new RoutedFW.Builder();
    private final UnroutedFW.Builder unroutedRW = new UnroutedFW.Builder();
    private final BoundFW.Builder boundRW = new BoundFW.Builder();
    private final UnboundFW.Builder unboundRW = new UnboundFW.Builder();
    private final PreparedFW.Builder preparedRW = new PreparedFW.Builder();
    private final UnpreparedFW.Builder unpreparedRW = new UnpreparedFW.Builder();
    private final ConnectedFW.Builder connectedRW = new ConnectedFW.Builder();

    private final ReflectorProxy reflectorProxy;
    private final OneToOneConcurrentArrayQueue<ConductorResponse> reflectorResponses;

    private final RingBuffer conductorCommands;
    private final BroadcastTransmitter conductorResponses;
    private final AtomicBuffer sendBuffer;

    public Conductor(Context context)
    {
        this.reflectorProxy = new ReflectorProxy(context);
        this.reflectorResponses = context.reflectorResponseQueue();
        this.conductorCommands = context.conductorCommands();
        this.conductorResponses = context.conductorResponses();
        this.sendBuffer = new UnsafeBuffer(new byte[SEND_BUFFER_CAPACITY]);
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += conductorCommands.read(this::handleCommand);
        weight += reflectorResponses.drain(this);

        return weight;
    }

    @Override
    public String name()
    {
        return "conductor";
    }

    @Override
    public void accept(ConductorResponse response)
    {
        response.execute(this);
    }

    public void onErrorResponse(long correlationId)
    {
        ErrorFW errorRO = errorRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                 .correlationId(correlationId)
                                 .build();

        conductorResponses.transmit(errorRO.typeId(), errorRO.buffer(), errorRO.offset(), errorRO.length());
    }

    public void onCapturedResponse(long correlationId)
    {
        CapturedFW capturedRO = capturedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                          .correlationId(correlationId)
                                          .build();

        conductorResponses.transmit(capturedRO.typeId(), capturedRO.buffer(), capturedRO.offset(), capturedRO.length());
    }

    public void onUncapturedResponse(long correlationId)
    {
        UncapturedFW uncapturedRO = uncapturedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                                .correlationId(correlationId)
                                                .build();

        conductorResponses.transmit(
                uncapturedRO.typeId(), uncapturedRO.buffer(), uncapturedRO.offset(), uncapturedRO.length());
    }

    public void onRoutedResponse(long correlationId)
    {
        RoutedFW routedRO = routedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                    .correlationId(correlationId)
                                    .build();

        conductorResponses.transmit(routedRO.typeId(), routedRO.buffer(), routedRO.offset(), routedRO.length());
    }

    public void onUnroutedResponse(long correlationId)
    {
        UnroutedFW unroutedRO = unroutedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                          .correlationId(correlationId)
                                          .build();

        conductorResponses.transmit(unroutedRO.typeId(), unroutedRO.buffer(), unroutedRO.offset(), unroutedRO.length());
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
        String source,
        long sourceRef)
    {
        unboundRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                 .correlationId(correlationId)
                 .binding()
                     .source(source)
                     .sourceRef(sourceRef);
        UnboundFW unboundRO = unboundRW.build();

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
        String destination,
        long destinationRef)
    {
        unpreparedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                    .correlationId(correlationId)
                    .preparation()
                        .destination(destination)
                        .destinationRef(destinationRef);
        UnpreparedFW unpreparedRO = unpreparedRW.build();

        conductorResponses.transmit(
            unpreparedRO.typeId(), unpreparedRO.buffer(), unpreparedRO.offset(), unpreparedRO.length());
    }

    public void onConnectedResponse(
        long correlationId,
        long connectionId)
    {
        ConnectedFW connectedRO = connectedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                             .correlationId(correlationId)
                                             .connectionId(connectionId)
                                             .build();

        conductorResponses.transmit(connectedRO.typeId(), connectedRO.buffer(), connectedRO.offset(), connectedRO.length());
    }

    private void handleCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case TYPE_ID_CAPTURE_COMMAND:
            handleCaptureCommand(buffer, index, length);
            break;
        case TYPE_ID_UNCAPTURE_COMMAND:
            handleUncaptureCommand(buffer, index, length);
            break;
        case TYPE_ID_ROUTE_COMMAND:
            handleRouteCommand(buffer, index, length);
            break;
        case TYPE_ID_UNROUTE_COMMAND:
            handleUnrouteCommand(buffer, index, length);
            break;
        case TYPE_ID_BIND_COMMAND:
            handleBindCommand(buffer, index, length);
            break;
        case TYPE_ID_UNBIND_COMMAND:
            handleUnbindCommand(buffer, index, length);
            break;
        case TYPE_ID_PREPARE_COMMAND:
            handlePrepareCommand(buffer, index, length);
            break;
        case TYPE_ID_UNPREPARE_COMMAND:
            handleUnprepareCommand(buffer, index, length);
            break;
        case TYPE_ID_CONNECT_COMMAND:
            handleConnectCommand(buffer, index, length);
            break;
        default:
            // ignore unrecognized commands (forwards compatible)
            break;
        }
    }

    private void handleCaptureCommand(DirectBuffer buffer, int index, int length)
    {
        captureRO.wrap(buffer, index, index + length);

        long correlationId = captureRO.correlationId();
        String source = captureRO.source().asString();

        reflectorProxy.doCapture(correlationId, source);
    }

    private void handleUncaptureCommand(DirectBuffer buffer, int index, int length)
    {
        uncaptureRO.wrap(buffer, index, index + length);

        long correlationId = uncaptureRO.correlationId();
        String source = uncaptureRO.source().asString();

        reflectorProxy.doUncapture(correlationId, source);
    }

    private void handleRouteCommand(DirectBuffer buffer, int index, int length)
    {
        routeRO.wrap(buffer, index, index + length);

        long correlationId = routeRO.correlationId();
        String destination = routeRO.destination().asString();

        reflectorProxy.doRoute(correlationId, destination);
    }

    private void handleUnrouteCommand(DirectBuffer buffer, int index, int length)
    {
        unrouteRO.wrap(buffer, index, index + length);

        long correlationId = unrouteRO.correlationId();
        String destination = unrouteRO.destination().asString();

        reflectorProxy.doUnroute(correlationId, destination);
    }

    private void handleBindCommand(DirectBuffer buffer, int index, int length)
    {
        bindRO.wrap(buffer, index, index + length);

        long correlationId = bindRO.correlationId();
        BindingFW binding = bindRO.binding();

        String source = binding.source().asString();
        long sourceRef = binding.sourceRef();

        reflectorProxy.doBind(correlationId, source, sourceRef);
    }

    private void handleUnbindCommand(DirectBuffer buffer, int index, int length)
    {
        unbindRO.wrap(buffer, index, index + length);

        long correlationId = unbindRO.correlationId();
        long referenceId = unbindRO.referenceId();

        reflectorProxy.doUnbind(correlationId, referenceId);
    }

    private void handlePrepareCommand(DirectBuffer buffer, int index, int length)
    {
        prepareRO.wrap(buffer, index, index + length);

        long correlationId = prepareRO.correlationId();
        PreparationFW preparation = prepareRO.preparation();

        String destination = preparation.destination().asString();
        long destinationRef = preparation.destinationRef();

        reflectorProxy.doPrepare(correlationId, destination, destinationRef);
    }

    private void handleUnprepareCommand(DirectBuffer buffer, int index, int length)
    {
        unprepareRO.wrap(buffer, index, index + length);

        long correlationId = unprepareRO.correlationId();
        long referenceId = unprepareRO.referenceId();

        reflectorProxy.doUnprepare(correlationId, referenceId);
    }

    private void handleConnectCommand(DirectBuffer buffer, int index, int length)
    {
        connectRO.wrap(buffer, index, index + length);

        long correlationId = connectRO.correlationId();
        long referenceId = connectRO.referenceId();

        reflectorProxy.doConnect(correlationId, referenceId);
    }
}
