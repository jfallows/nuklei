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
package org.kaazing.nuklei.tcp.internal.writer;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.nio.channels.SelectionKey.OP_READ;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataFW;
import org.kaazing.nuklei.tcp.internal.types.stream.EndFW;
import org.kaazing.nuklei.tcp.internal.types.stream.ResetFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.nio.TransportPoller;

public final class Writer extends TransportPoller implements Nukleus, Consumer<WriterCommand>
{
    private static final int MAX_RECEIVE_LENGTH = 1024; // TODO: Configuration and Context

    private final ResetFW.Builder resetRW = new ResetFW.Builder();
    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final ConductorProxy.FromWriter conductorProxy;
    private final OneToOneConcurrentArrayQueue<WriterCommand> commandQueue;
    private final ByteBuffer byteBuffer;
    private final AtomicBuffer atomicBuffer;

    private Function<String, File> streamsFile;

    private int streamsCapacity;

    private HashMap<String, StreamsLayout> layoutsByHandler;

    public Writer(Context context)
    {
        this.conductorProxy = new ConductorProxy.FromWriter(context);
        this.commandQueue = context.writerCommandQueue();
        this.byteBuffer = allocateDirect(MAX_RECEIVE_LENGTH).order(nativeOrder());
        this.atomicBuffer = new UnsafeBuffer(byteBuffer.duplicate());
        this.streamsFile = context.routeStreamsFile();
        this.streamsCapacity = context.streamsCapacity();
        this.layoutsByHandler = new HashMap<>();
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        selector.selectNow();
        weight += selectedKeySet.forEach(this::processRead);
        weight += commandQueue.drain(this);

        return weight;
    }

    @Override
    public String name()
    {
        return "reader";
    }

    @Override
    public void close()
    {
        selector.keys().forEach((key) -> {
            try
            {
                WriterState state = (WriterState) key.attachment();
                state.channel().shutdownInput();
            }
            catch (final Exception ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
        });

        layoutsByHandler.values().forEach((layout) -> {
            try
            {
                layout.close();
            }
            catch (final Exception ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
        });

        super.close();
    }

    @Override
    public void accept(WriterCommand command)
    {
        command.execute(this);
    }

    public void doRoute(
        long correlationId,
        String destination)
    {
        StreamsLayout layout = layoutsByHandler.get(destination);
        if (layout != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                StreamsLayout newLayout = new StreamsLayout.Builder().streamsFile(streamsFile.apply(destination))
                                                                     .streamsCapacity(streamsCapacity)
                                                                     .createFile(false)
                                                                     .build();

                layoutsByHandler.put(destination, newLayout);
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
        StreamsLayout oldLayout = layoutsByHandler.remove(destination);
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

    public void doRegister(
        String handler,
        long handlerRef,
        long clientStreamId,
        long serverStreamId,
        SocketChannel channel)
    {
        StreamsLayout layout = layoutsByHandler.get(handler);

        // TODO
        assert layout != null;

        RingBuffer writeBuffer = layout.buffer();

        final long streamId = serverStreamId != 0L ? serverStreamId : clientStreamId;
        final long referenceId = serverStreamId != 0L ? clientStreamId : handlerRef;

        WriterState state = new WriterState(writeBuffer, streamId, channel);

        BeginFW beginRO = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .streamId(state.streamId())
                                 .referenceId(referenceId)
                                 .build();

        if (!writeBuffer.write(beginRO.typeId(), beginRO.buffer(), beginRO.offset(), beginRO.length()))
        {
            throw new IllegalStateException("could not write to ring buffer");
        }

        try
        {
            channel.configureBlocking(false);
            channel.register(selector, OP_READ, state);
        }
        catch (ClosedChannelException ex)
        {
            // channel already closed (deterministic stream begin & end)
            EndFW endRO = endRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                               .streamId(state.streamId())
                               .build();

            if (!writeBuffer.write(endRO.typeId(), endRO.buffer(), endRO.offset(), endRO.length()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doReset(
        long streamId,
        String handler,
        long handlerRef)
    {
        StreamsLayout layout = layoutsByHandler.get(handler);

        // TODO
        assert layout != null;

        RingBuffer writeBuffer = layout.buffer();

        ResetFW resetRO = resetRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .streamId(streamId)
                                 .build();

        if (!writeBuffer.write(resetRO.typeId(), resetRO.buffer(), resetRO.offset(), resetRO.length()))
        {
            throw new IllegalStateException("could not write to ring buffer");
        }
    }

    private int processRead(SelectionKey selectionKey)
    {
        try
        {
            final WriterState state = (WriterState) selectionKey.attachment();
            final SocketChannel channel = state.channel();
            final long streamId = state.streamId();
            final RingBuffer writeBuffer = state.streamBuffer();

            dataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                  .streamId(streamId);

            // TODO: limit maximum bytes read
            byteBuffer.position(dataRW.payloadOffset());
            int bytesRead = channel.read(byteBuffer);

            if (bytesRead == -1)
            {
                EndFW endRO = endRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                   .streamId(state.streamId())
                                   .build();

                if (!writeBuffer.write(endRO.typeId(), endRO.buffer(), endRO.offset(), endRO.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }

                selectionKey.cancel();
            }
            else if (bytesRead != 0)
            {
                DataFW dataRO = dataRW.payloadLength(bytesRead).build();

                if (!writeBuffer.write(dataRO.typeId(), dataRO.buffer(), dataRO.offset(), dataRO.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }
            }

            return 1;
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        return 1;
    }
}
