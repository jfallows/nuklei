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

import static java.nio.charset.StandardCharsets.UTF_8;

import org.kaazing.nuklei.tcp.internal.state.Binding;
import org.kaazing.nuklei.tcp.internal.state.BindingHooks;
import org.kaazing.nuklei.tcp.internal.state.BindingStateMachine;
import org.kaazing.nuklei.tcp.internal.types.StringRO;
import org.kaazing.nuklei.tcp.internal.types.StringRW;
import org.kaazing.nuklei.tcp.internal.types.control.BindRO;
import org.kaazing.nuklei.tcp.internal.types.control.BoundRW;
import org.kaazing.nuklei.tcp.internal.types.control.UnbindRO;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Conductor implements Agent
{
    private static final int SEND_BUFFER_CAPACITY = 1024;
    private final BindRO bindRO = new BindRO();
    private final BoundRW boundRW = new BoundRW();
    private final StringRO nukleusRO = new StringRW().wrap(new UnsafeBuffer(new byte[4]), 0).set("tcp", UTF_8).asReadOnly();

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
        default:
            // ignore unrecognized commands
            break;
        }
    }

    private void onNukleusBind(MutableDirectBuffer buffer, int index, int length)
    {
        bindRO.wrap(buffer, index, index + length);
        Binding newBinding = new Binding();
        machine.start(newBinding);
        machine.received(newBinding, bindRO);
    }

    private void whenBindReceived(Binding binding, BindRO bind)
    {
        binding.correlationId = bind.correlationId();
        binding.reference = 0x01L;  // TODO: increment (when initialized?)

        bindings.put(binding.reference, binding);

        // TODO: perform actual Socket bind

        boundRW.wrap(sendBuffer, 0)
               .correlationId(binding.correlationId)
               .destination(nukleusRO)
               .bindingRef(binding.reference);

        toControllerResponses.transmit(0x40000001, boundRW.buffer(), boundRW.offset(), boundRW.remaining());
    }

    private void whenUnbindReceived(Binding binding, UnbindRO bind)
    {
    }
}
