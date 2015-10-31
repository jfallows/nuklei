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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.conductor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.acceptor.AcceptorProxy;
import org.kaazing.nuklei.tcp.internal.acceptor.AcceptorResponse;
import org.kaazing.nuklei.tcp.internal.types.control.BindRO;
import org.kaazing.nuklei.tcp.internal.types.control.BoundRW;
import org.kaazing.nuklei.tcp.internal.types.control.ErrorRW;
import org.kaazing.nuklei.tcp.internal.types.control.UnbindRO;
import org.kaazing.nuklei.tcp.internal.types.control.UnboundRW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Conductor implements Nukleus, Consumer<AcceptorResponse>
{
    private static final int SEND_BUFFER_CAPACITY = 1024; // TODO: Configuration and Context

    private final BindRO bindRO = new BindRO();
    private final BoundRW boundRW = new BoundRW();
    private final UnbindRO unbindRO = new UnbindRO();
    private final UnboundRW unboundRW = new UnboundRW();
    private final ErrorRW errorRW = new ErrorRW();

    private final RingBuffer toNukleusCommands;
    private final MessageHandler onNukleusCommandFunc;

    private final AcceptorProxy acceptorProxy;
    private final OneToOneConcurrentArrayQueue<AcceptorResponse> acceptorResponses;

    private final BroadcastTransmitter toControllerResponses;
    private final UnsafeBuffer sendBuffer;

    public Conductor(Context context)
    {
        this.acceptorProxy = new AcceptorProxy(context);
        this.acceptorResponses = context.acceptorResponseQueue();
        this.toNukleusCommands = context.toNukleusCommands();
        this.onNukleusCommandFunc = this::onReceiveCommand;
        this.toControllerResponses = context.toControllerResponses();
        this.sendBuffer = new UnsafeBuffer(new byte[SEND_BUFFER_CAPACITY]);
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += toNukleusCommands.read(onNukleusCommandFunc);
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
        errorRW.wrap(sendBuffer, 0)
               .correlationId(correlationId);

        System.out.println("ERROR RESPONSE: " + errorRW);

        toControllerResponses.transmit(0x40000000, errorRW.buffer(), errorRW.offset(), errorRW.remaining());
    }

    public void onBoundResponse(
        long correlationId,
        long bindingRef)
    {
        boundRW.wrap(sendBuffer, 0)
               .correlationId(correlationId)
               .bindingRef(bindingRef);

        System.out.println("BOUND RESPONSE: " + boundRW);

        toControllerResponses.transmit(0x40000001, boundRW.buffer(), boundRW.offset(), boundRW.remaining());
    }

    public void onUnboundResponse(
        long correlationId,
        String source,
        long sourceBindingRef,
        String destination,
        InetSocketAddress address)
    {
        unboundRW.wrap(sendBuffer, 0)
                 .correlationId(correlationId)
                 .binding()
                     .source().set(source, UTF_8);
        unboundRW.binding()
                     .sourceBindingRef(sourceBindingRef)
                     .destination().set(destination, UTF_8);
        unboundRW.binding().address().ipAddress(address.getAddress());
        unboundRW.binding().port(address.getPort());

        System.out.println("UNBOUND RESPONSE: " + unboundRW);

        toControllerResponses.transmit(0x40000002, unboundRW.buffer(), unboundRW.offset(), unboundRW.remaining());
    }

    private void onReceiveCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case 0x00000001:
            onReceiveBindCommand(buffer, index, length);
            break;
        case 0x00000002:
            onReceiveUnbindCommand(buffer, index, length);
            break;
        default:
            // ignore unrecognized commands
            break;
        }
    }

    private void onReceiveBindCommand(DirectBuffer buffer, int index, int length)
    {
        bindRO.wrap(buffer, index, index + length);

        long correlationId = bindRO.correlationId();
        String source = bindRO.binding().source().asString();
        long sourceBindingRef = bindRO.binding().sourceBindingRef();
        String destination = bindRO.binding().destination().asString();
        InetSocketAddress address = new InetSocketAddress(bindRO.binding().address().asInetAddress(), bindRO.binding().port());

        acceptorProxy.onBindRequest(correlationId, source, sourceBindingRef, destination, address);
    }

    private void onReceiveUnbindCommand(DirectBuffer buffer, int index, int length)
    {
        unbindRO.wrap(buffer, index, index + length);

        long correlationId = unbindRO.correlationId();
        long bindingRef = unbindRO.bindingRef();

        acceptorProxy.onUnbindRequest(correlationId, bindingRef);
    }
}
