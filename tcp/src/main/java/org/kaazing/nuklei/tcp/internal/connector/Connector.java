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
package org.kaazing.nuklei.tcp.internal.connector;

import static java.nio.channels.SelectionKey.OP_CONNECT;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.tcp.internal.reader.ReaderProxy;
import org.kaazing.nuklei.tcp.internal.writer.WriterProxy;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.nio.TransportPoller;

public final class Connector extends TransportPoller implements Nukleus, Consumer<ConnectorCommand>
{
    private final ConductorProxy.FromConnector conductorProxy;
    private final ReaderProxy readerProxy;
    private final WriterProxy writerProxy;
    private final OneToOneConcurrentArrayQueue<ConnectorCommand> commandQueueFromConductor;
    private final OneToOneConcurrentArrayQueue<ConnectorCommand> commandQueueFromReader;
    private final Long2ObjectHashMap<ConnectorState> stateByRef;

    private final AtomicCounter streamsPrepared;
    private final AtomicCounter streamsConnected;

    public Connector(Context context)
    {
        this.conductorProxy = new ConductorProxy.FromConnector(context);
        this.readerProxy = new ReaderProxy(context);
        this.writerProxy = new WriterProxy(context);
        this.commandQueueFromConductor = context.connectorCommandQueueFromConductor();
        this.commandQueueFromReader = context.connectorCommandQueueFromReader();
        this.stateByRef = new Long2ObjectHashMap<>();
        this.streamsPrepared = context.counters().streamsPrepared();
        this.streamsConnected = context.counters().streamsConnected();
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        selector.selectNow();
        weight += selectedKeySet.forEach(this::processConnect);
        weight += commandQueueFromConductor.drain(this);
        weight += commandQueueFromReader.drain(this);

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
        String handler,
        InetSocketAddress remoteAddress)
    {
        try
        {
            final long handlerRef = streamsPrepared.increment();

            final ConnectorState newState = new ConnectorState(handler, handlerRef, remoteAddress);

            stateByRef.put(newState.handlerRef(), newState);

            conductorProxy.onPreparedResponse(correlationId, newState.handlerRef());
        }
        catch (Exception ex)
        {
            conductorProxy.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
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
                String handler = state.handler();
                InetSocketAddress remoteAddress = state.remoteAddress();

                conductorProxy.onUnpreparedResponse(correlationId, handler, remoteAddress);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doConnect(
        String handler,
        long handlerRef,
        long streamId)
    {
        // TODO: reference uniqueness, scope by handler too
        final ConnectorState state = stateByRef.get(handlerRef);

        if (state == null)
        {
            writerProxy.doReset(handler, handlerRef, streamId);
        }
        else
        {
            try
            {
                InetSocketAddress remoteAddress = state.remoteAddress();

                SocketChannel channel = SocketChannel.open();
                channel.configureBlocking(false);
                if (channel.connect(remoteAddress))
                {
                    // even, positive, non-zero
                    final long newServerStreamId = streamsConnected.increment() << 1L;

                    readerProxy.doRegister(handler, handlerRef, streamId, newServerStreamId, channel);
                    writerProxy.doRegister(handler, handlerRef, streamId, newServerStreamId, channel);
                }
                else
                {
                    ConnectRequestState attachment = new ConnectRequestState(state, streamId, channel);
                    channel.register(selector, OP_CONNECT, attachment);
                }
            }
            catch (Exception ex)
            {
                writerProxy.doReset(handler, handlerRef, streamId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    private int processConnect(SelectionKey selectionKey)
    {
        ConnectRequestState attachment = (ConnectRequestState) selectionKey.attachment();
        ConnectorState state = attachment.owner();
        long streamId = attachment.streamId();
        SocketChannel channel = attachment.channel();

        String handler = state.handler();
        long handlerRef = state.handlerRef();

        try
        {
            channel.finishConnect();

            // even, positive, non-zero
            streamsConnected.increment();
            final long newServerStreamId = streamsConnected.get() << 1L;

            readerProxy.doRegister(handler, handlerRef, streamId, newServerStreamId, channel);
            writerProxy.doRegister(handler, handlerRef, streamId, newServerStreamId, channel);

            selectionKey.cancel();
        }
        catch (Exception ex)
        {
            writerProxy.doReset(handler, handlerRef, streamId);
            LangUtil.rethrowUnchecked(ex);
        }

        return 1;
    }
}
