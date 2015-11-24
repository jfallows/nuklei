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
package org.kaazing.nuklei.echo.internal.acceptor;

import static java.lang.String.format;

import java.io.File;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.echo.internal.Context;
import org.kaazing.nuklei.echo.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.echo.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.echo.internal.reflector.ReflectorMode;
import org.kaazing.nuklei.echo.internal.reflector.ReflectorProxy;

import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Acceptor implements Nukleus, Consumer<AcceptorCommand>
{
    private final ConductorProxy conductorProxy;
    private final ReflectorProxy reflectorProxy;
    private final OneToOneConcurrentArrayQueue<AcceptorCommand> commandQueue;
    private final Long2ObjectHashMap<AcceptorState> stateByRef;
    private final File streamsDirectory;
    private final int streamsCapacity;

    public Acceptor(Context context)
    {
        this.conductorProxy = new ConductorProxy(context);
        this.reflectorProxy = new ReflectorProxy(context);
        this.commandQueue = context.acceptorCommandQueue();
        this.stateByRef = new Long2ObjectHashMap<>();
        this.streamsDirectory = context.streamsDirectory();
        this.streamsCapacity = context.streamsCapacity();
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

    public void doBind(
        long correlationId,
        String source,
        long sourceRef)
    {
        final long referenceId = correlationId;

        AcceptorState oldState = stateByRef.get(referenceId);
        if (oldState != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                // BIND goes first, so Acceptor owns bidirectional streams mapped file lifecycle
                // TODO: unmap mapped buffer (also cleanup in Context.close())

                StreamsLayout streamsRO = new StreamsLayout.Builder().streamsDirectory(streamsDirectory)
                                                                     .streamsFilename(format("%s.accepts", source))
                                                                     .streamsCapacity(streamsCapacity)
                                                                     .build();

                RingBuffer inputBuffer = streamsRO.inputBuffer();
                RingBuffer outputBuffer = streamsRO.outputBuffer();

                AcceptorState newState = new AcceptorState(referenceId, source, sourceRef,
                                                           inputBuffer, outputBuffer);

                stateByRef.put(newState.reference(), newState);

                reflectorProxy.doRegister(referenceId, ReflectorMode.ACCEPT, inputBuffer, outputBuffer);

                conductorProxy.onBoundResponse(correlationId, newState.reference());
            }
            catch (Exception e)
            {
                conductorProxy.onErrorResponse(correlationId);
                throw new RuntimeException(e);
            }
        }
    }

    public void doUnbind(
        long correlationId,
        long referenceId)
    {
        final AcceptorState state = stateByRef.remove(referenceId);

        if (state == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                String source = state.source();
                long sourceRef = state.sourceRef();

                conductorProxy.onUnboundResponse(correlationId, source, sourceRef);
            }
            catch (Exception e)
            {
                conductorProxy.onErrorResponse(correlationId);
            }
        }
    }
}
