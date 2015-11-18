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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.acceptor;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;
import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.MappedByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.StreamsFileDescriptor;
import org.kaazing.nuklei.tcp.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.tcp.internal.reader.ReaderProxy;
import org.kaazing.nuklei.tcp.internal.writer.WriterProxy;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import uk.co.real_logic.agrona.nio.TransportPoller;

public final class Acceptor extends TransportPoller implements Nukleus, Consumer<AcceptorCommand>
{
    private final ConductorProxy conductorProxy;
    private final ReaderProxy readerProxy;
    private final WriterProxy writerProxy;
    private final OneToOneConcurrentArrayQueue<AcceptorCommand> commandQueue;
    private final Long2ObjectHashMap<AcceptorState> stateByRef;
    private final AtomicCounter connectionsCount;
    private final File streamsDir;

    public Acceptor(Context context)
    {
        this.conductorProxy = new ConductorProxy(context);
        this.readerProxy = new ReaderProxy(context);
        this.writerProxy = new WriterProxy(context);
        this.commandQueue = context.acceptorCommandQueue();
        this.stateByRef = new Long2ObjectHashMap<>();
        this.connectionsCount = context.countersManager().newCounter("connections");
        this.streamsDir = context.controlFile().getParentFile(); // TODO: better abstraction
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        selector.selectNow();
        weight += selectedKeySet.forEach(this::processAccept);
        weight += commandQueue.drain(this);

        return weight;
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

    @Override
    public void accept(AcceptorCommand command)
    {
        command.execute(this);
    }

    public void doBind(
        long correlationId,
        String source,
        long sourceRef,
        String destination,
        InetSocketAddress localAddress)
    {
        final long reference = correlationId;

        AcceptorState oldState = stateByRef.get(reference);
        if (oldState != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                final ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.bind(localAddress);
                serverChannel.configureBlocking(false);

                // BIND goes first, so Acceptor owns bidirectional streams mapped file lifecycle
                // TODO: unmap mapped buffer (also cleanup in Context.close())
                int streamBufferSize = 1024 * 1024 + RingBufferDescriptor.TRAILER_LENGTH;
                File streamsFile = new File(streamsDir, String.format("%s.accepts", destination));
                StreamsFileDescriptor streams = new StreamsFileDescriptor(streamBufferSize);
                createEmptyFile(streamsFile, streams.totalLength());
                MappedByteBuffer inputByteBuffer = mapExistingFile(streamsFile, "input",
                        streams.inputOffset(), streams.inputLength());
                MappedByteBuffer outputByteBuffer = mapExistingFile(streamsFile, "output",
                        streams.outputOffset(), streams.outputLength());
                RingBuffer inputBuffer = new ManyToOneRingBuffer(new UnsafeBuffer(inputByteBuffer));
                RingBuffer outputBuffer = new ManyToOneRingBuffer(new UnsafeBuffer(outputByteBuffer));

                final AcceptorState newState = new AcceptorState(reference, source, sourceRef,
                                                                 destination, localAddress, inputBuffer, outputBuffer);

                serverChannel.register(selector, OP_ACCEPT, newState);
                newState.attach(serverChannel);

                stateByRef.put(newState.reference(), newState);

                conductorProxy.onBoundResponse(correlationId, newState.reference());
            }
            catch (IOException e)
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
                ServerSocketChannel serverChannel = state.channel();
                serverChannel.close();
                selector.selectNow();

                String source = state.source();
                long sourceRef = state.sourceRef();
                String destination = state.destination();
                InetSocketAddress localAddress = state.localAddress();

                conductorProxy.onUnboundResponse(correlationId, source, sourceRef, destination, localAddress);
            }
            catch (IOException e)
            {
                conductorProxy.onErrorResponse(correlationId);
            }
        }
    }

    private int processAccept(SelectionKey selectionKey)
    {
        try
        {
            AcceptorState state = (AcceptorState) selectionKey.attachment();
            ServerSocketChannel serverChannel = state.channel();
            SocketChannel channel = serverChannel.accept();
            long connectionId = connectionsCount.increment();

            readerProxy.doRegister(connectionId, state.reference(), channel, state.inputBuffer());
            writerProxy.doRegister(connectionId, state.reference(), channel, state.outputBuffer());

            return 1;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
