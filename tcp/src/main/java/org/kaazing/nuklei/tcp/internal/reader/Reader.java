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
package org.kaazing.nuklei.tcp.internal.reader;

import static org.kaazing.nuklei.tcp.internal.types.stream.Types.TYPE_ID_BEGIN;
import static org.kaazing.nuklei.tcp.internal.types.stream.Types.TYPE_ID_DATA;
import static org.kaazing.nuklei.tcp.internal.types.stream.Types.TYPE_ID_END;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataFW;
import org.kaazing.nuklei.tcp.internal.types.stream.EndFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.ArrayUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.nio.TransportPoller;

public final class Reader extends TransportPoller implements Nukleus, Consumer<ReaderCommand>
{
    private final BeginFW beginRO = new BeginFW();
    private final EndFW endRO = new EndFW();
    private final DataFW dataRO = new DataFW();

    private final ConductorProxy.FromReader conductorProxy;
    private final OneToOneConcurrentArrayQueue<ReaderCommand> commandQueue;
    private final Long2ObjectHashMap<ReaderState> stateByStreamId;
    private final Function<String, File> streamsFile;
    private final int streamsCapacity;
    private final Map<String, StreamsLayout> layoutsByHandler;

    private RingBuffer[] streamBuffers;

    public Reader(Context context)
    {
        this.conductorProxy = new ConductorProxy.FromReader(context);
        this.commandQueue = context.readerCommandQueue();
        this.stateByStreamId = new Long2ObjectHashMap<>();
        this.streamBuffers = new RingBuffer[0];
        this.streamsFile = context.captureStreamsFile();
        this.streamsCapacity = context.streamsCapacity();
        this.layoutsByHandler = new HashMap<>();
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        selector.selectNow();
        weight += selectedKeySet.forEach(this::processWrite);
        weight += commandQueue.drain(this);

        for (int i=0; i < streamBuffers.length; i++)
        {
            weight += streamBuffers[i].read(this::handleRead);
        }

        return weight;
    }

    @Override
    public String name()
    {
        return "writer";
    }

    @Override
    public void close()
    {
        stateByStreamId.values().forEach((state) -> {
            try
            {
                state.channel().shutdownOutput();
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
    public void accept(ReaderCommand command)
    {
        command.execute(this);
    }

    public void doCapture(
        long correlationId,
        String handler)
    {
        StreamsLayout layout = layoutsByHandler.get(handler);
        if (layout != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                StreamsLayout newLayout = new StreamsLayout.Builder().streamsFile(streamsFile.apply(handler))
                                                                     .streamsCapacity(streamsCapacity)
                                                                     .createFile(true)
                                                                     .build();

                layoutsByHandler.put(handler, newLayout);

                streamBuffers = ArrayUtil.add(streamBuffers, newLayout.buffer());

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
        String handler)
    {
        StreamsLayout oldLayout = layoutsByHandler.remove(handler);
        if (oldLayout == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                streamBuffers = ArrayUtil.remove(streamBuffers, oldLayout.buffer());
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

    public void doRegister(
        String handler,
        long handlerRef,
        long streamId,
        SocketChannel channel)
    {
        StreamsLayout layout = layoutsByHandler.get(handler);
        assert layout != null;

        RingBuffer streamBuffer = layout.buffer();

        ReaderState state = new ReaderState(handlerRef, streamId, streamBuffer, channel);

        // TODO: BiInt2ObjectMap needed or already sufficiently unique?
        stateByStreamId.put(state.streamId(), state);
    }

    private void handleRead(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case TYPE_ID_BEGIN:
            beginRO.wrap(buffer, index, index + length);
            ReaderState newState = stateByStreamId.get(beginRO.streamId());
            if (newState == null)
            {
                throw new IllegalStateException("stream not found: " + beginRO.streamId());
            }
            break;

        case TYPE_ID_DATA:
            dataRO.wrap(buffer, index, index + length);

            ReaderState state = stateByStreamId.get(dataRO.streamId());
            if (state == null)
            {
                throw new IllegalStateException("stream not found: " + dataRO.streamId());
            }

            try
            {
                SocketChannel channel = state.channel();
                ByteBuffer writeBuffer = state.writeBuffer();
                writeBuffer.limit(dataRO.limit());
                writeBuffer.position(dataRO.payloadOffset());

                // send buffer underlying buffer for read buffer
                final int totalBytes = writeBuffer.remaining();
                final int bytesWritten = channel.write(writeBuffer);

                if (bytesWritten < totalBytes)
                {
                    // TODO: support partial writes
                    throw new IllegalStateException("partial write: " + bytesWritten + "/" + totalBytes);
                }
            }
            catch (IOException ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
            break;

        case TYPE_ID_END:
            endRO.wrap(buffer, index, index + length);

            ReaderState oldState = stateByStreamId.remove(endRO.streamId());
            if (oldState == null)
            {
                throw new IllegalStateException("stream not found: " + endRO.streamId());
            }

            try
            {
                SocketChannel channel = oldState.channel();
                channel.close();
            }
            catch (IOException ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
            break;
        }
    }

    private int processWrite(SelectionKey selectionKey)
    {
        // fulfill partial writes (flow control?)
        return 1;
    }
}
