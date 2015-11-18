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

package org.kaazing.nuklei.tcp.internal.writer;

import static org.kaazing.nuklei.tcp.internal.types.stream.BeginFW.BEGIN_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.stream.DataFW.DATA_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.stream.EndFW.END_TYPE_ID;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataFW;
import org.kaazing.nuklei.tcp.internal.types.stream.EndFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.ArrayUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.nio.TransportPoller;

public final class Writer extends TransportPoller implements Nukleus, Consumer<WriterCommand>
{
    private final BeginFW beginRO = new BeginFW();
    private final EndFW endRO = new EndFW();
    private final DataFW dataRO = new DataFW();

    private final OneToOneConcurrentArrayQueue<WriterCommand> commandQueue;
    private final Long2ObjectHashMap<WriterState> stateByStreamId;
    private final MessageHandler readHandler;
    private RingBuffer[] streamBuffers;

    public Writer(Context context)
    {
        this.commandQueue = context.writerCommandQueue();
        this.stateByStreamId = new Long2ObjectHashMap<>();
        this.streamBuffers = new RingBuffer[0];
        this.readHandler = this::handleRead;
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
            weight += streamBuffers[i].read(readHandler);
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

        super.close();
    }

    @Override
    public void accept(WriterCommand command)
    {
        command.execute(this);
    }

    public void doRegister(
        long bindingRef,
        long connectionId,
        SocketChannel channel,
        RingBuffer streamBuffer)
    {
        WriterState state = new WriterState(bindingRef, connectionId, channel, streamBuffer);

        // TODO: BiInt2ObjectMap needed or already sufficiently unique?
        stateByStreamId.put(state.streamId(), state);

        // TODO: prevent duplicate adds
        streamBuffers = ArrayUtil.add(streamBuffers, streamBuffer);
    }

    private void handleRead(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case BEGIN_TYPE_ID:
            beginRO.wrap(buffer, index, index + length);
            WriterState newState = stateByStreamId.get(beginRO.streamId());
            if (newState == null)
            {
                throw new IllegalStateException("stream not found: " + beginRO.streamId());
            }
            break;

        case DATA_TYPE_ID:
            dataRO.wrap(buffer, index, index + length);

            WriterState state = stateByStreamId.get(dataRO.streamId());
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

        case END_TYPE_ID:
            endRO.wrap(buffer, index, index + length);

            WriterState oldState = stateByStreamId.remove(endRO.streamId());
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
