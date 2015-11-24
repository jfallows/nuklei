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
package org.kaazing.nuklei.echo.internal.connector;

import static java.lang.String.format;

import java.io.File;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.echo.internal.Context;
import org.kaazing.nuklei.echo.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.echo.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.echo.internal.reflector.ReflectorMode;
import org.kaazing.nuklei.echo.internal.reflector.ReflectorProxy;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Connector implements Nukleus, Consumer<ConnectorCommand>
{
    private final ConductorProxy conductorProxy;
    private final ReflectorProxy reflectorProxy;
    private final OneToOneConcurrentArrayQueue<ConnectorCommand> commandQueue;
    private final Long2ObjectHashMap<ConnectorState> stateByRef;
    private final AtomicCounter connectedCount;
    private final File streamsDirectory;
    private final int streamsCapacity;

    public Connector(Context context)
    {
        this.conductorProxy = new ConductorProxy(context);
        this.reflectorProxy = new ReflectorProxy(context);
        this.commandQueue = context.connectorCommandQueue();
        this.stateByRef = new Long2ObjectHashMap<>();
        this.connectedCount = context.counters().connectedCount();
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
        return "connector";
    }

    @Override
    public void accept(ConnectorCommand command)
    {
        command.execute(this);
    }

    public void doPrepare(
        long correlationId,
        String destination,
        long destinationRef)
    {
        final long referenceId = correlationId;

        ConnectorState oldState = stateByRef.get(referenceId);
        if (oldState != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                // PREPARE goes first, so Connector owns bidirectional streams mapped file lifecycle
                // TODO: unmap mapped buffer (also cleanup in Context.close())

                StreamsLayout streamsRO = new StreamsLayout.Builder().streamsDirectory(streamsDirectory)
                                                                     .streamsFilename(format("%s.connects", destination))
                                                                     .streamsCapacity(streamsCapacity)
                                                                     .build();

                RingBuffer inputBuffer = streamsRO.inputBuffer();
                RingBuffer outputBuffer = streamsRO.outputBuffer();

                final ConnectorState newState = new ConnectorState(referenceId, destination, destinationRef, inputBuffer,
                                                                   outputBuffer);

                stateByRef.put(newState.reference(), newState);

                reflectorProxy.doRegister(referenceId, ReflectorMode.CONNECT, inputBuffer, outputBuffer);

                conductorProxy.onPreparedResponse(correlationId, newState.reference());
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doUnprepare(
        long correlationId,
        long referenceId)
    {
        final ConnectorState state = stateByRef.remove(referenceId);

        if (state == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                String destination = state.destination();
                long destinationRef = state.destinationRef();

                conductorProxy.onUnpreparedResponse(correlationId, destination, destinationRef);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doConnect(
        long correlationId,
        long referenceId)
    {
        final ConnectorState state = stateByRef.get(referenceId);

        if (state == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                long connectionId = connectedCount.increment();

                reflectorProxy.doConnect(state.reference(), connectionId);

                conductorProxy.onConnectedResponse(correlationId, connectionId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }
}
