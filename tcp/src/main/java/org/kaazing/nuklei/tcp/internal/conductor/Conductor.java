/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.conductor;

import static org.kaazing.nuklei.tcp.internal.types.control.BindFW.BIND_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.control.UnbindFW.UNBIND_TYPE_ID;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.acceptor.AcceptorProxy;
import org.kaazing.nuklei.tcp.internal.acceptor.AcceptorResponse;
import org.kaazing.nuklei.tcp.internal.types.control.BindFW;
import org.kaazing.nuklei.tcp.internal.types.control.BoundFW;
import org.kaazing.nuklei.tcp.internal.types.control.ErrorFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnbindFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnboundFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Conductor implements Nukleus, Consumer<AcceptorResponse>
{
    private static final int SEND_BUFFER_CAPACITY = 1024; // TODO: Configuration and Context

    private final BindFW bindRO = new BindFW();
    private final UnbindFW unbindRO = new UnbindFW();

    private final BoundFW.Builder boundRW = new BoundFW.Builder();
    private final UnboundFW.Builder unboundRW = new UnboundFW.Builder();
    private final ErrorFW.Builder errorRW = new ErrorFW.Builder();

    private final RingBuffer conductorCommands;

    private final AcceptorProxy acceptorProxy;
    private final OneToOneConcurrentArrayQueue<AcceptorResponse> acceptorResponses;

    private final BroadcastTransmitter conductorResponses;
    private final AtomicBuffer sendBuffer;

    public Conductor(Context context)
    {
        this.acceptorProxy = new AcceptorProxy(context);
        this.acceptorResponses = context.acceptorResponseQueue();
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

        return weight;
    }

    @Override
    public String name()
    {
        return "conductor";
    }

    @Override
    public void accept(AcceptorResponse response)
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
        long bindingRef)
    {
        BoundFW boundRO = boundRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                                 .correlationId(correlationId)
                                 .bindingRef(bindingRef)
                                 .build();

        conductorResponses.transmit(boundRO.typeId(), boundRO.buffer(), boundRO.offset(), boundRO.remaining());
    }

    public void onUnboundResponse(
        long correlationId,
        String source,
        long sourceBindingRef,
        String destination,
        InetSocketAddress address)
    {
        unboundRW.wrap(sendBuffer, 0, sendBuffer.capacity())
                 .correlationId(correlationId)
                 .binding()
                     .source(source)
                     .sourceBindingRef(sourceBindingRef)
                     .destination(destination)
                     .address(address.getAddress())
                     .port(address.getPort());
        UnboundFW unboundRO = unboundRW.build();

        conductorResponses.transmit(unboundRO.typeId(), unboundRO.buffer(), unboundRO.offset(), unboundRO.remaining());
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
        default:
            // ignore unrecognized commands (forwards compatible)
            break;
        }
    }

    private void handleBindCommand(DirectBuffer buffer, int index, int length)
    {
        bindRO.wrap(buffer, index, index + length);

        long correlationId = bindRO.correlationId();
        String source = bindRO.binding().source().asString();
        long sourceBindingRef = bindRO.binding().sourceBindingRef();
        String destination = bindRO.binding().destination().asString();
        InetSocketAddress address = new InetSocketAddress(bindRO.binding().address().asInetAddress(), bindRO.binding().port());

        acceptorProxy.doBind(correlationId, source, sourceBindingRef, destination, address);
    }

    private void handleUnbindCommand(DirectBuffer buffer, int index, int length)
    {
        unbindRO.wrap(buffer, index, index + length);

        long correlationId = unbindRO.correlationId();
        long bindingRef = unbindRO.bindingRef();

        acceptorProxy.doUnbind(correlationId, bindingRef);
    }
}
