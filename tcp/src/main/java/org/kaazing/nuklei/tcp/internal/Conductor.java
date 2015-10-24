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

package org.kaazing.nuklei.tcp.internal;

import org.kaazing.nuklei.tcp.internal.state.Binding;
import org.kaazing.nuklei.tcp.internal.state.BindingHooks;
import org.kaazing.nuklei.tcp.internal.state.BindingStateMachine;
import org.kaazing.nuklei.tcp.internal.types.control.BindRO;
import org.kaazing.nuklei.tcp.internal.types.control.BindingRO;
import org.kaazing.nuklei.tcp.internal.types.control.BoundRW;
import org.kaazing.nuklei.tcp.internal.types.control.UnbindRO;
import org.kaazing.nuklei.tcp.internal.types.control.UnboundRW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Conductor implements Agent
{
    private static final int SEND_BUFFER_CAPACITY = 1024; // TODO: Configuration and Context

    private final BindingRO bindingRO = new BindingRO();
    private final BindRO bindRO = new BindRO();
    private final BoundRW boundRW = new BoundRW();
    private final UnbindRO unbindRO = new UnbindRO();
    private final UnboundRW unboundRW = new UnboundRW();

    private final RingBuffer toNukleusCommands;
    private final MessageHandler onNukleusCommandFunc;
    private final BroadcastTransmitter toControllerResponses;

    private final BindingStateMachine machine;
    private final Long2ObjectHashMap<Binding> bindings;

    private final UnsafeBuffer sendBuffer;

    Conductor(Context context)
    {
        this.toNukleusCommands = context.toNukleusCommands();
        this.onNukleusCommandFunc = this::onNukleusCommand;
        this.toControllerResponses = context.toControllerResponses();

        BindingHooks bindingHooks = new BindingHooks();
        bindingHooks.whenInitialized = this::whenInitialized;
        bindingHooks.whenBindReceived = this::whenBindReceived;
        bindingHooks.whenUnbindReceived = this::whenUnbindReceived;
        this.machine = new BindingStateMachine(bindingHooks);
        this.bindings = new Long2ObjectHashMap<>();
        this.sendBuffer = new UnsafeBuffer(new byte[SEND_BUFFER_CAPACITY]);
    }

    @Override
    public int doWork() throws Exception
    {
        int workCount = 0;

        workCount += toNukleusCommands.read(onNukleusCommandFunc);

        return workCount;
    }

    @Override
    public String roleName()
    {
        return "conductor";
    }

    private void onNukleusCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case 0x00000001:
            onNukleusBind(buffer, index, length);
            break;
        case 0x00000002:
            onNukleusUnbind(buffer, index, length);
            break;
        default:
            // ignore unrecognized commands
            break;
        }
    }

    private void onNukleusBind(DirectBuffer buffer, int index, int length)
    {
        bindRO.wrap(buffer, index, index + length);

        Binding newBinding = new Binding(0x01L); // TODO: increment
        machine.start(newBinding);
        machine.received(newBinding, bindRO);
    }

    private void onNukleusUnbind(DirectBuffer buffer, int index, int length)
    {
        unbindRO.wrap(buffer, index, index + length);
        Binding binding = bindings.get(unbindRO.bindingRef());
        // TODO: default binding when not found
        machine.received(binding, unbindRO);
    }

    private void whenInitialized(Binding binding)
    {
        bindings.put(binding.reference(), binding);
    }

    private void whenBindReceived(Binding binding, BindRO bind)
    {
        binding.saveBinding(bind.binding());

        // TODO: perform actual Socket bind
        System.out.println("BIND REQUEST: " + bind);

        boundRW.wrap(sendBuffer, 0)
               .correlationId(bind.correlationId())
               .bindingRef(binding.reference());

        System.out.println("BOUND RESPONSE: " + boundRW);

        toControllerResponses.transmit(0x40000001, boundRW.buffer(), boundRW.offset(), boundRW.remaining());
    }

    private void whenUnbindReceived(Binding binding, UnbindRO unbind)
    {
        // TODO: perform actual Socket unbind
        System.out.println("UNBIND REQUEST: " + unbind);

        unboundRW.wrap(sendBuffer, 0)
                 .correlationId(unbind.correlationId())
                 .binding(binding.loadBinding(bindingRO));

        System.out.println("UNBOUND RESPONSE: " + unboundRW);

        toControllerResponses.transmit(0x40000002, unboundRW.buffer(), unboundRW.offset(), unboundRW.remaining());
    }
}
