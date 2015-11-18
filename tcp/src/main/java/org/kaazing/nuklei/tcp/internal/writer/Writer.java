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

import static org.kaazing.nuklei.tcp.internal.types.stream.BeginType.BEGIN_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.stream.DataType.DATA_TYPE_ID;
import static org.kaazing.nuklei.tcp.internal.types.stream.EndType.END_TYPE_ID;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginRO;
import org.kaazing.nuklei.tcp.internal.types.stream.DataRO;
import org.kaazing.nuklei.tcp.internal.types.stream.EndRO;

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
    private final OneToOneConcurrentArrayQueue<WriterCommand> commandQueue;
    private final Long2ObjectHashMap<WriterInfo> infosByStreamId;
    private final MessageHandler readHandler;
    private RingBuffer[] streamBuffers;
    private final BeginRO beginRO = new BeginRO();
    private final EndRO endRO = new EndRO();
    private final DataRO dataRO = new DataRO();

    public Writer(Context context)
    {
        this.commandQueue = context.writerCommandQueue();
        this.infosByStreamId = new Long2ObjectHashMap<>();
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
        WriterInfo info = new WriterInfo(bindingRef, connectionId, channel, streamBuffer);

        // TODO: BiInt2ObjectMap needed or already sufficiently unique?
        infosByStreamId.put(info.streamId(), info);

        // TODO: prevent duplicate adds
        streamBuffers = ArrayUtil.add(streamBuffers, streamBuffer);
    }

    private void handleRead(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case BEGIN_TYPE_ID:
            beginRO.wrap(buffer, index, index + length);
            WriterInfo newInfo = infosByStreamId.get(beginRO.streamId());
            if (newInfo == null)
            {
                throw new IllegalStateException("stream not found: " + beginRO.streamId());
            }
            break;

        case DATA_TYPE_ID:
            dataRO.wrap(buffer, index, index + length);

            WriterInfo info = infosByStreamId.get(dataRO.streamId());
            if (info == null)
            {
                throw new IllegalStateException("stream not found: " + dataRO.streamId());
            }

            try
            {
                SocketChannel channel = info.channel();
                ByteBuffer sendBuffer = info.sendBuffer();
                sendBuffer.limit(dataRO.limit());
                sendBuffer.position(dataRO.payloadOffset());

                // send buffer underlying buffer for read buffer
                final int total = sendBuffer.remaining();
                final int sent = channel.write(sendBuffer);

                if (sent < total)
                {
                    // TODO: support partial writes
                    throw new IllegalStateException("partial write: " + sent + "/" + length);
                }
            }
            catch (IOException ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
            break;

        case END_TYPE_ID:
            endRO.wrap(buffer, index, index + length);

            WriterInfo oldInfo = infosByStreamId.remove(endRO.streamId());
            if (oldInfo == null)
            {
                throw new IllegalStateException("stream not found: " + endRO.streamId());
            }

            try
            {
                SocketChannel channel = oldInfo.channel();
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
