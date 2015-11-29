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
package org.kaazing.nuklei.echo.internal.reflector;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static org.kaazing.nuklei.echo.internal.types.stream.Types.TYPE_ID_BEGIN;
import static org.kaazing.nuklei.echo.internal.types.stream.Types.TYPE_ID_DATA;
import static org.kaazing.nuklei.echo.internal.types.stream.Types.TYPE_ID_END;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.echo.internal.Context;
import org.kaazing.nuklei.echo.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.echo.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.echo.internal.types.stream.BeginFW;
import org.kaazing.nuklei.echo.internal.types.stream.DataFW;
import org.kaazing.nuklei.echo.internal.types.stream.EndFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.ArrayUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Reflector implements Nukleus, Consumer<ReflectorCommand>
{
    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final BeginFW beginRO = new BeginFW();
    private final DataFW dataRO = new DataFW();
    private final EndFW endRO = new EndFW();

    private final ConductorProxy conductorProxy;
    private final OneToOneConcurrentArrayQueue<ReflectorCommand> commandQueue;

    private final Long2ObjectHashMap<ConnectorState> connectorStateByRef;
    private final Long2ObjectHashMap<AcceptorState> acceptorStateByRef;
    private final Long2ObjectHashMap<ReflectorState> stateByStreamId;
    private final Long2ObjectHashMap<ConnectingState> connectingStateByStreamId;

    private final Map<String, StreamsLayout> streamsBySource;
    private final Map<String, StreamsLayout> streamsByDestination;
    private final Function<String, File> captureStreamsFile;
    private final Function<String, File> routeStreamsFile;
    private final int streamsCapacity;

    private final AtomicCounter streamsBound;
    private final AtomicCounter streamsPrepared;
    private final AtomicCounter streamsAccepted;
    private final AtomicCounter streamsConnected;
    private final AtomicCounter messagesReflected;
    private final AtomicCounter bytesReflected;

    private final AtomicBuffer atomicBuffer;

    private final Long2ObjectHashMap<AcceptorState> acceptorStateBySourceRef;
    private RingBuffer[] readBuffers;

    public Reflector(Context context)
    {
        this.conductorProxy = new ConductorProxy(context);
        this.commandQueue = context.reflectorCommandQueue();
        this.acceptorStateByRef = new Long2ObjectHashMap<>();
        this.connectorStateByRef = new Long2ObjectHashMap<>();
        this.stateByStreamId = new Long2ObjectHashMap<>();
        this.connectingStateByStreamId = new Long2ObjectHashMap<>();
        this.streamsBound = context.counters().streamsBound();
        this.streamsPrepared = context.counters().streamsPrepared();
        this.streamsAccepted = context.counters().streamsAccepted();
        this.streamsConnected = context.counters().streamsConnected();
        this.messagesReflected = context.counters().messagesReflected();
        this.bytesReflected = context.counters().bytesReflected();
        this.atomicBuffer = new UnsafeBuffer(allocateDirect(context.maxMessageLength()).order(nativeOrder()));
        this.captureStreamsFile = context.captureStreamsFile();
        this.routeStreamsFile = context.routeStreamsFile();
        this.streamsCapacity = context.streamsCapacity();
        this.streamsBySource = new HashMap<>();
        this.streamsByDestination = new HashMap<>();

        this.readBuffers = new RingBuffer[0];
        this.acceptorStateBySourceRef = new Long2ObjectHashMap<>();
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += commandQueue.drain(this);

        int length = readBuffers.length;
        for (int i = 0; i < length; i++)
        {
            weight += readBuffers[i].read(this::handleRead);
        }

        return weight;
    }

    @Override
    public String name()
    {
        return "reflector";
    }

    @Override
    public void accept(ReflectorCommand command)
    {
        command.execute(this);
    }

    public void doCapture(
        long correlationId,
        String source)
    {
        StreamsLayout layout = streamsBySource.get(source);
        if (layout != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                StreamsLayout newLayout = new StreamsLayout.Builder().streamsFile(captureStreamsFile.apply(source))
                                                                     .streamsCapacity(streamsCapacity)
                                                                     .createFile(true)
                                                                     .build();

                streamsBySource.put(source, newLayout);

                readBuffers = ArrayUtil.add(readBuffers, newLayout.buffer());

                conductorProxy.onCapturedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doUncapture(
        long correlationId,
        String source)
    {
        StreamsLayout oldLayout = streamsBySource.remove(source);
        if (oldLayout == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                readBuffers = ArrayUtil.remove(readBuffers, oldLayout.buffer());

                oldLayout.close();

                conductorProxy.onUncapturedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doRoute(
        long correlationId,
        String destination)
    {
        StreamsLayout layout = streamsByDestination.get(destination);
        if (layout != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                StreamsLayout newLayout = new StreamsLayout.Builder().streamsFile(routeStreamsFile.apply(destination))
                                                                     .streamsCapacity(streamsCapacity)
                                                                     .createFile(false)
                                                                     .build();

                streamsByDestination.put(destination, newLayout);
                conductorProxy.onRoutedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doUnroute(
        long correlationId,
        String destination)
    {
        StreamsLayout oldLayout = streamsByDestination.remove(destination);
        if (oldLayout == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                oldLayout.close();
                conductorProxy.onUnroutedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doBind(
        long correlationId,
        String source,
        long sourceRef)
    {
        StreamsLayout layout = streamsBySource.get(source);

        if (layout == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                final long referenceId = streamsBound.increment();

                AcceptorState newState = new AcceptorState(referenceId, source, sourceRef);

                acceptorStateByRef.put(newState.reference(), newState);

                // TODO: scope by source
                acceptorStateBySourceRef.put(newState.sourceRef(), newState);

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
        final AcceptorState oldState = acceptorStateByRef.remove(referenceId);

        if (oldState == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                String source = oldState.source();
                long sourceRef = oldState.sourceRef();

                StreamsLayout streams = streamsBySource.get(source);

                if (streams == null)
                {
                    conductorProxy.onErrorResponse(correlationId);
                }
                else
                {
                    // TODO: scope by source
                    acceptorStateBySourceRef.remove(oldState.sourceRef());

                    conductorProxy.onUnboundResponse(correlationId, source, sourceRef);
                }
            }
            catch (Exception e)
            {
                conductorProxy.onErrorResponse(correlationId);
            }
        }
    }

    public void doPrepare(
        long correlationId,
        String destination,
        long destinationRef)
    {
        StreamsLayout layout = streamsBySource.get(destination);

        if (layout == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                final long referenceId = streamsPrepared.increment();

                ConnectorState newState = new ConnectorState(referenceId, destination, destinationRef);

                connectorStateByRef.put(newState.reference(), newState);

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
        final ConnectorState state = connectorStateByRef.remove(referenceId);

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
        final ConnectorState state = connectorStateByRef.get(referenceId);

        if (state == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                StreamsLayout layout = streamsByDestination.get(state.destination());
                if (layout == null)
                {
                    conductorProxy.onErrorResponse(correlationId);
                }
                else
                {
                    // positive, odd, non-zero
                    streamsConnected.increment();
                    final long newClientStreamId = (streamsConnected.get() << 1L) | 0x0000000000000001L;
                    final long destinationRef = state.destinationRef();
                    final RingBuffer writeBuffer = layout.buffer();

                    // TODO: scope state by uniqueness of streamId
                    ConnectingState newState = new ConnectingState(newClientStreamId, writeBuffer);
                    connectingStateByStreamId.put(newState.streamId(), newState);

                    BeginFW beginRO = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                             .streamId(newClientStreamId)
                                             .referenceId(destinationRef)
                                             .build();

                    if (!writeBuffer.write(beginRO.typeId(), beginRO.buffer(), beginRO.offset(), beginRO.length()))
                    {
                        throw new IllegalStateException("could not write to ring buffer");
                    }

                    conductorProxy.onConnectedResponse(correlationId, newClientStreamId);
                }
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    private void handleRead(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case TYPE_ID_BEGIN:
            beginRO.wrap(buffer, index, index + length);

            final long streamId = beginRO.streamId();
            final long referenceId = beginRO.referenceId();

            if ((streamId & 0x0000000000000001L) != 0L)
            {
                // accepted stream (TODO: scope by source)
                AcceptorState acceptorState = acceptorStateBySourceRef.get(referenceId);
                if (acceptorState == null)
                {
                    throw new IllegalStateException("reference not found: " + referenceId);
                }
                else
                {
                    final long clientStreamId = streamId;

                    StreamsLayout layout = streamsByDestination.get(acceptorState.source());
                    if (layout == null)
                    {
                        throw new IllegalStateException("stream not found: " + clientStreamId);
                    }
                    else
                    {
                        // positive, even, non-zero
                        streamsAccepted.increment();
                        final long newServerStreamId = streamsAccepted.get() << 1L;
                        final RingBuffer writeBuffer = layout.buffer();

                        final ReflectorState newState = new ReflectorState(newServerStreamId, writeBuffer);
                        stateByStreamId.put(beginRO.streamId(), newState);

                        BeginFW begin = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                               .streamId(newState.streamId())
                                               .referenceId(clientStreamId)
                                               .build();

                        if (!writeBuffer.write(begin.typeId(), begin.buffer(), begin.offset(), begin.length()))
                        {
                            throw new IllegalStateException("could not write to ring buffer");
                        }
                    }
                }
            }
            else
            {
                // connecting stream (reference is connecting stream id)
                final long clientStreamId = referenceId;
                ConnectingState connectingState = connectingStateByStreamId.remove(clientStreamId);
                if (connectingState == null)
                {
                    throw new IllegalStateException("connecting stream not found: " + clientStreamId);
                }
                else
                {
                    final long serverStreamId = streamId;
                    ReflectorState newState = new ReflectorState(clientStreamId, connectingState.writeBuffer());
                    stateByStreamId.put(serverStreamId, newState);
                }
            }
            break;

        case TYPE_ID_DATA:
            dataRO.wrap(buffer, index, index + length);

            ReflectorState state = stateByStreamId.get(dataRO.streamId());
            if (state == null)
            {
                throw new IllegalStateException("stream not found: " + dataRO.streamId());
            }
            else
            {
                // reflect data with updated stream id
                atomicBuffer.putBytes(0, buffer, index, length);
                DataFW data = dataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                    .streamId(state.streamId())
                                    .payloadLength(dataRO.payloadLength())
                                    .build();

                messagesReflected.increment();
                bytesReflected.add(data.payloadLength());

                if (!state.writeBuffer().write(data.typeId(), data.buffer(), data.offset(), data.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }
            }
            break;

        case TYPE_ID_END:
            endRO.wrap(buffer, index, index + length);

            ReflectorState oldState = stateByStreamId.remove(endRO.streamId());
            if (oldState == null)
            {
                throw new IllegalStateException("stream not found: " + endRO.streamId());
            }
            else
            {
                EndFW end = endRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .streamId(oldState.streamId())
                                 .build();

                if (!oldState.writeBuffer().write(end.typeId(), end.buffer(), end.offset(), end.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }
            }
            break;
        }
    }
}
