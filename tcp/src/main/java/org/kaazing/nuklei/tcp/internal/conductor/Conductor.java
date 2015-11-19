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
package org.kaazing.nuklei.tcp.internal.conductor;

import static org.kaazing.nuklei.tcp.internal.types.control.BindFW.BIND_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.control.ConnectFW.CONNECT_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.control.PrepareFW.PREPARE_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.control.UnbindFW.UNBIND_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.control.UnprepareFW.UNPREPARE_TYPE_ID;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.acceptor.AcceptorProxy;
import org.kaazing.nuklei.tcp.internal.connector.ConnectorProxy;
import org.kaazing.nuklei.tcp.internal.types.control.BindFW;
import org.kaazing.nuklei.tcp.internal.types.control.BindingFW;
import org.kaazing.nuklei.tcp.internal.types.control.BoundFW;
import org.kaazing.nuklei.tcp.internal.types.control.ConnectFW;
import org.kaazing.nuklei.tcp.internal.types.control.ConnectedFW;
import org.kaazing.nuklei.tcp.internal.types.control.ErrorFW;
import org.kaazing.nuklei.tcp.internal.types.control.PreparationFW;
import org.kaazing.nuklei.tcp.internal.types.control.PrepareFW;
import org.kaazing.nuklei.tcp.internal.types.control.PreparedFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnbindFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnboundFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnprepareFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnpreparedFW;

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

    private final BindFW bindRO = new BindFW();
    private final UnbindFW unbindRO = new UnbindFW();
    private final PrepareFW prepareRO = new PrepareFW();
    private final UnprepareFW unprepareRO = new UnprepareFW();
    private final ConnectFW connectRO = new ConnectFW();

    private final ErrorFW.Builder errorRW = new ErrorFW.Builder();
    private final BoundFW.Builder boundRW = new BoundFW.Builder();
    private final UnboundFW.Builder unboundRW = new UnboundFW.Builder();
    private final PreparedFW.Builder preparedRW = new PreparedFW.Builder();
    private final UnpreparedFW.Builder unpreparedRW = new UnpreparedFW.Builder();
    private final ConnectedFW.Builder connectedRW = new ConnectedFW.Builder();

    private final RingBuffer conductorCommands;

    private final AcceptorProxy acceptorProxy;
    private final ConnectorProxy connectorProxy;
    private final OneToOneConcurrentArrayQueue<ConductorResponse> acceptorResponses;
    private final OneToOneConcurrentArrayQueue<ConductorResponse> connectorResponses;

    private final BroadcastTransmitter conductorResponses;
    private final AtomicBuffer sendBuffer;

    public Conductor(Context context)
    {
        this.acceptorProxy = new AcceptorProxy(context);
        this.connectorProxy = new ConnectorProxy(context);
        this.acceptorResponses = context.acceptorResponseQueue();
        this.connectorResponses = context.connectorResponseQueue();
        this.conductorCommands = context.conductorCommands();
        this.conductorResponses = context.conductorResponses();
        this.sendBuffer = new UnsafeBuffer(new byte[SEND_BUFFER_CAPACITY]);
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += conductorCommands.read(this::handleCommand);
        weight += acceptorResponses.drain(this);
        weight += connectorResponses.drain(this);

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

        conductorResponses.transmit(errorRO.typeId(), errorRO.buffer(), errorRO.offset(), errorRO.remaining());
    }

    public void onBoundResponse(
        long correlationId,
        long referenceId)
    {
        BoundFW boundRO = boundRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                 .correlationId(correlationId)
                                 .referenceId(referenceId)
                                 .build();

        conductorResponses.transmit(boundRO.typeId(), boundRO.buffer(), boundRO.offset(), boundRO.remaining());
    }

    public void onUnboundResponse(
        long correlationId,
        String source,
        long sourceRef,
        String destination,
        InetSocketAddress localAddress)
    {
        unboundRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                 .correlationId(correlationId)
                 .binding()
                     .source(source)
                     .sourceRef(sourceRef)
                     .destination(destination)
                     .address(localAddress.getAddress())
                     .port(localAddress.getPort());
        UnboundFW unboundRO = unboundRW.build();

        conductorResponses.transmit(unboundRO.typeId(), unboundRO.buffer(), unboundRO.offset(), unboundRO.remaining());
    }

    public void onPreparedResponse(
        long correlationId,
        long referenceId)
    {
        PreparedFW preparedRO = preparedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                          .correlationId(correlationId)
                                          .referenceId(referenceId)
                                          .build();

        conductorResponses.transmit(preparedRO.typeId(), preparedRO.buffer(), preparedRO.offset(), preparedRO.remaining());
    }

    public void onUnpreparedResponse(
        long correlationId,
        String destination,
        long destinationRef,
        String source,
        InetSocketAddress remoteAddress)
    {
        unpreparedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                    .correlationId(correlationId)
                    .preparation()
                        .destination(destination)
                        .destinationRef(destinationRef)
                        .source(source)
                        .address(remoteAddress.getAddress())
                        .port(remoteAddress.getPort());
        UnpreparedFW unpreparedRO = unpreparedRW.build();

        conductorResponses.transmit(
            unpreparedRO.typeId(), unpreparedRO.buffer(), unpreparedRO.offset(), unpreparedRO.remaining());
    }

    public void onConnectedResponse(
        long correlationId,
        long connectionId)
    {
        ConnectedFW connectedRO = connectedRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                             .correlationId(correlationId)
                                             .connectionId(connectionId)
                                             .build();

        conductorResponses.transmit(connectedRO.typeId(), connectedRO.buffer(), connectedRO.offset(), connectedRO.remaining());
    }

    private void handleCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case BIND_TYPE_ID:
            handleBindCommand(buffer, index, length);
            break;
        case UNBIND_TYPE_ID:
            handleUnbindCommand(buffer, index, length);
            break;
        case PREPARE_TYPE_ID:
            handlePrepareCommand(buffer, index, length);
            break;
        case UNPREPARE_TYPE_ID:
            handleUnprepareCommand(buffer, index, length);
            break;
        case CONNECT_TYPE_ID:
            handleConnectCommand(buffer, index, length);
            break;
        default:
            // ignore unrecognized commands (forwards compatible)
            break;
        }
    }

    private void handleBindCommand(DirectBuffer buffer, int index, int length)
    {
        bindRO.wrap(buffer, index, index + length);

        long correlationId = bindRO.correlationId();
        BindingFW binding = bindRO.binding();

        String source = binding.source().asString();
        long sourceBindingRef = binding.sourceRef();
        String destination = binding.destination().asString();
        InetSocketAddress address = new InetSocketAddress(binding.address().asInetAddress(), binding.port());

        acceptorProxy.doBind(correlationId, source, sourceBindingRef, destination, address);
    }

    private void handleUnbindCommand(DirectBuffer buffer, int index, int length)
    {
        unbindRO.wrap(buffer, index, index + length);

        long correlationId = unbindRO.correlationId();
        long referenceId = unbindRO.referenceId();

        acceptorProxy.doUnbind(correlationId, referenceId);
    }

    private void handlePrepareCommand(DirectBuffer buffer, int index, int length)
    {
        prepareRO.wrap(buffer, index, index + length);

        long correlationId = prepareRO.correlationId();
        PreparationFW preparation = prepareRO.preparation();

        String destination = preparation.destination().asString();
        long destinationRef = preparation.destinationRef();
        String source = preparation.source().asString();
        InetSocketAddress address = new InetSocketAddress(preparation.address().asInetAddress(), preparation.port());

        connectorProxy.doPrepare(correlationId, destination, destinationRef, source, address);
    }

    private void handleUnprepareCommand(DirectBuffer buffer, int index, int length)
    {
        unprepareRO.wrap(buffer, index, index + length);

        long correlationId = unprepareRO.correlationId();
        long referenceId = unprepareRO.referenceId();

        connectorProxy.doUnprepare(correlationId, referenceId);
    }

    private void handleConnectCommand(DirectBuffer buffer, int index, int length)
    {
        connectRO.wrap(buffer, index, index + length);

        long correlationId = connectRO.correlationId();
        long referenceId = connectRO.referenceId();

        connectorProxy.doConnect(correlationId, referenceId);
    }
}
