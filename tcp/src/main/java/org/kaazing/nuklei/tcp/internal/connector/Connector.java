/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.conductor.Conductor;
import org.kaazing.nuklei.tcp.internal.reader.Reader;
import org.kaazing.nuklei.tcp.internal.writer.Writer;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.nio.TransportPoller;

@Reaktive
public final class Connector extends TransportPoller implements Nukleus
{
    private final Long2ObjectHashMap<ConnectorState> stateByRef;

    private final AtomicCounter streamsPrepared;
    private final AtomicCounter streamsConnected;

    private Conductor conductor;
    private Reader reader;
    private Writer writer;

    public Connector(Context context)
    {
        this.stateByRef = new Long2ObjectHashMap<>();
        this.streamsPrepared = context.counters().streamsPrepared();
        this.streamsConnected = context.counters().streamsConnected();
    }

    public void setConductor(Conductor conductor)
    {
        this.conductor = conductor;
    }

    public void setReader(Reader reader)
    {
        this.reader = reader;
    }

    public void setWriter(Writer writer)
    {
        this.writer = writer;
    }

    @Override
    public int process()
    {
        selectNow();
        return selectedKeySet.forEach(this::processConnect);
    }

    @Override
    public String name()
    {
        return "connector";
    }

    public void doPrepare(
        long correlationId,
        String source,
        InetSocketAddress remoteAddress)
    {
        try
        {
            final long sourceRef = streamsPrepared.increment();

            final ConnectorState newState = new ConnectorState(source, sourceRef, remoteAddress);

            stateByRef.put(newState.sourceRef(), newState);

            conductor.onPreparedResponse(correlationId, newState.sourceRef());
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
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
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                String source = state.source();
                InetSocketAddress remoteAddress = state.remoteAddress();

                conductor.onUnpreparedResponse(correlationId, source, remoteAddress);
            }
            catch (Exception ex)
            {
                conductor.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doConnect(
        String source,
        long sourceRef,
        long streamId)
    {
        // TODO: reference uniqueness, scope by handler too
        final ConnectorState state = stateByRef.get(sourceRef);

        if (state == null)
        {
            writer.doReset(streamId, source, sourceRef);
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

                    reader.doRegister(source, sourceRef, streamId, newServerStreamId, channel);
                    writer.doRegister(source, sourceRef, streamId, newServerStreamId, channel);
                }
                else
                {
                    ConnectRequestState attachment = new ConnectRequestState(state, streamId, channel);
                    channel.register(selector, OP_CONNECT, attachment);
                }
            }
            catch (Exception ex)
            {
                writer.doReset(streamId, source, sourceRef);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    private void selectNow()
    {
        try
        {
            selector.selectNow();
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    private int processConnect(SelectionKey selectionKey)
    {
        ConnectRequestState attachment = (ConnectRequestState) selectionKey.attachment();
        ConnectorState state = attachment.owner();
        long streamId = attachment.streamId();
        SocketChannel channel = attachment.channel();

        String handler = state.source();
        long handlerRef = state.sourceRef();

        try
        {
            channel.finishConnect();

            // even, positive, non-zero
            streamsConnected.increment();
            final long newServerStreamId = streamsConnected.get() << 1L;

            reader.doRegister(handler, handlerRef, streamId, newServerStreamId, channel);
            writer.doRegister(handler, handlerRef, streamId, newServerStreamId, channel);

            selectionKey.cancel();
        }
        catch (Exception ex)
        {
            writer.doReset(streamId, handler, handlerRef);
            LangUtil.rethrowUnchecked(ex);
        }

        return 1;
    }
}
