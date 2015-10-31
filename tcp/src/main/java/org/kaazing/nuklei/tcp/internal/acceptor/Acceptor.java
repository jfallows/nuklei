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

package org.kaazing.nuklei.tcp.internal.acceptor;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;

import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class Acceptor implements Nukleus, Consumer<AcceptorCommand>
{
    private final ConductorProxy conductorProxy;
    private final OneToOneConcurrentArrayQueue<AcceptorCommand> commandQueue;
    private final Long2ObjectHashMap<Binding> bindings;

    public Acceptor(Context context)
    {
        this.conductorProxy = new ConductorProxy(context);
        this.commandQueue = context.acceptorCommandQueue();
        this.bindings = new Long2ObjectHashMap<>();
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += commandQueue.drain(this);

        return weight;
    }

    @Override
    public String name()
    {
        return "acceptor";
    }

    @Override
    public void accept(AcceptorCommand command)
    {
        command.execute(this);
    }

    public void onBindRequest(
        long correlationId,
        String source,
        long sourceBindingRef,
        String destination,
        InetSocketAddress address)
    {
        final Binding newBinding = new Binding(correlationId, source, sourceBindingRef, destination, address);

        Binding oldBinding = bindings.get(newBinding.reference());
        if (oldBinding != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            bindings.put(newBinding.reference(), newBinding);

            // TODO: perform actual Socket bind
            System.out.println("BIND REQUEST: " + newBinding);

            conductorProxy.onBoundResponse(correlationId, newBinding.reference());
        }
    }

    public void onUnbindRequest(
        long correlationId,
        long bindingRef)
    {
        final Binding binding = bindings.remove(bindingRef);

        if (binding == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            // TODO: perform actual Socket unbind
            System.out.println("UNBIND REQUEST: " + binding);

            String source = binding.source();
            long sourceBindingRef = binding.sourceBindingRef();
            String destination = binding.destination();
            InetSocketAddress address = binding.address();

            conductorProxy.onUnboundResponse(correlationId, source, sourceBindingRef, destination, address);
        }
    }
}
