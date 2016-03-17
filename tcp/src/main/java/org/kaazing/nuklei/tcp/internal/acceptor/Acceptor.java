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
package org.kaazing.nuklei.tcp.internal.acceptor;

import static java.nio.channels.SelectionKey.OP_ACCEPT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
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
public final class Acceptor extends TransportPoller implements Nukleus
{
    private final Long2ObjectHashMap<AcceptorState> stateByRef;
    private final AtomicCounter streamsBound;
    private final AtomicCounter streamsAccepted;

    private Conductor conductor;
    private Reader reader;
    private Writer writer;

    public Acceptor(Context context)
    {
        this.stateByRef = new Long2ObjectHashMap<>();
        this.streamsBound = context.counters().streamsBound();
        this.streamsAccepted = context.counters().streamsAccepted();
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
        return selectedKeySet.forEach(this::processAccept);
    }

    @Override
    public String name()
    {
        return "acceptor";
    }

    @Override
    public void close()
    {
        stateByRef.values().forEach((state) -> {
            try
            {
                state.channel().close();
                selectNowWithoutProcessing();
            }
            catch (final Exception ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
        });

        super.close();
    }

    public void doBind(
        long correlationId,
        String destination,
        long destinationRef,
        InetSocketAddress localAddress)
    {

        try
        {
            final long newReferenceId = streamsBound.increment();

            final ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(localAddress);
            serverChannel.configureBlocking(false);

            AcceptorState newState = new AcceptorState(destination, destinationRef, localAddress);

            serverChannel.register(selector, OP_ACCEPT, newState);
            newState.attach(serverChannel);

            stateByRef.put(newReferenceId, newState);

            conductor.onBoundResponse(correlationId, newReferenceId);
        }
        catch (IOException e)
        {
            conductor.onErrorResponse(correlationId);
            throw new RuntimeException(e);
        }
    }

    public void doUnbind(
        long correlationId,
        long referenceId)
    {
        final AcceptorState state = stateByRef.remove(referenceId);

        if (state == null)
        {
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                ServerSocketChannel serverChannel = state.channel();
                serverChannel.close();
                selector.selectNow();

                String destination = state.destination();
                long destinationRef = state.destinationRef();
                InetSocketAddress localAddress = state.localAddress();

                conductor.onUnboundResponse(correlationId, destination, destinationRef, localAddress);
            }
            catch (IOException e)
            {
                conductor.onErrorResponse(correlationId);
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

    private int processAccept(SelectionKey selectionKey)
    {
        try
        {
            AcceptorState state = (AcceptorState) selectionKey.attachment();
            String destination = state.destination();
            long destinationRef = state.destinationRef();
            ServerSocketChannel serverChannel = state.channel();

            // odd, positive, non-zero
            streamsAccepted.increment();
            final long newInitialStreamId = (streamsAccepted.get() << 1L) | 0x0000000000000001L;
            final SocketChannel channel = serverChannel.accept();

            writer.doRegister(destination, destinationRef, newInitialStreamId, 0L, channel);
            reader.doRegister(destination, destinationRef, newInitialStreamId, 0L, channel);
        }
        catch (Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        return 1;
    }
}
