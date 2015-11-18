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
    private final Long2ObjectHashMap<BindingInfo> bindingInfosByRef;
    private final AtomicCounter connectionsCount;
    private final File streamsDir;

    public Acceptor(Context context)
    {
        this.conductorProxy = new ConductorProxy(context);
        this.readerProxy = new ReaderProxy(context);
        this.writerProxy = new WriterProxy(context);
        this.commandQueue = context.acceptorCommandQueue();
        this.bindingInfosByRef = new Long2ObjectHashMap<>();
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
        bindingInfosByRef.values().forEach((bindingInfo) -> {
            try
            {
                bindingInfo.channel().close();
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
        long sourceBindingRef,
        String destination,
        InetSocketAddress address)
    {
        final long reference = correlationId;

        BindingInfo oldInfo = bindingInfosByRef.get(reference);
        if (oldInfo != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                final ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.bind(address);
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

                final BindingInfo newBindingInfo = new BindingInfo(reference, source, sourceBindingRef,
                                                                   destination, address, inputBuffer, outputBuffer);

                serverChannel.register(selector, OP_ACCEPT, newBindingInfo);
                newBindingInfo.attach(serverChannel);

                bindingInfosByRef.put(newBindingInfo.reference(), newBindingInfo);

                conductorProxy.onBoundResponse(correlationId, newBindingInfo.reference());
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
        long bindingRef)
    {
        final BindingInfo bindingInfo = bindingInfosByRef.remove(bindingRef);

        if (bindingInfo == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                ServerSocketChannel serverChannel = bindingInfo.channel();
                serverChannel.close();
                selector.selectNow();

                String source = bindingInfo.source();
                long sourceBindingRef = bindingInfo.sourceBindingRef();
                String destination = bindingInfo.destination();
                InetSocketAddress address = bindingInfo.address();

                conductorProxy.onUnboundResponse(correlationId, source, sourceBindingRef, destination, address);
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
            BindingInfo bindingInfo = (BindingInfo) selectionKey.attachment();
            ServerSocketChannel serverChannel = bindingInfo.channel();
            SocketChannel channel = serverChannel.accept();
            long connectionId = connectionsCount.increment();

            readerProxy.doRegister(connectionId, bindingInfo.reference(), channel, bindingInfo.inputBuffer());
            writerProxy.doRegister(connectionId, bindingInfo.reference(), channel, bindingInfo.outputBuffer());

            return 1;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
