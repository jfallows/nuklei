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

package org.kaazing.nuklei.tcp.internal.reader;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginRW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataRW;
import org.kaazing.nuklei.tcp.internal.types.stream.EndRW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.nio.TransportPoller;

public final class Reader extends TransportPoller implements Nukleus, Consumer<ReaderCommand>
{
    private static final int MAX_RECEIVE_LENGTH = 1024; // TODO: Configuration and Context

    private final BeginRW beginRW = new BeginRW();
    private final DataRW dataRW = new DataRW();
    private final EndRW endRW = new EndRW();

    private final OneToOneConcurrentArrayQueue<ReaderCommand> commandQueue;
    private final ByteBuffer byteBuffer;
    private final AtomicBuffer atomicBuffer;

    public Reader(Context context)
    {
        this.commandQueue = context.readerCommandQueue();
        this.byteBuffer = allocateDirect(MAX_RECEIVE_LENGTH).order(nativeOrder());
        this.atomicBuffer = new UnsafeBuffer(byteBuffer);
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
    public void accept(ReaderCommand command)
    {
        command.execute(this);
    }

    public void doRegister(long bindingRef, long connectionId, SocketChannel channel, RingBuffer inputBuffer)
    {
        try
        {
            ReaderInfo info = new ReaderInfo(connectionId, channel, inputBuffer);
            channel.configureBlocking(false);
            channel.register(selector, OP_READ, info);

            beginRW.wrap(atomicBuffer, 0)
                   .streamId(info.streamId())
                   .bindingRef(bindingRef);

            inputBuffer.write(beginRW.typeId(), beginRW.buffer(), beginRW.offset(), beginRW.remaining());
        }
        catch (ClosedChannelException ex)
        {
            // channel already closed
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    private int processRead(SelectionKey selectionKey)
    {
        try
        {
            final ReaderInfo info = (ReaderInfo) selectionKey.attachment();
            final SocketChannel channel = info.channel();
            final long streamId = info.streamId();
            final RingBuffer writeBuffer = info.ringBuffer();

            dataRW.wrap(atomicBuffer, 0)
                  .streamId(streamId);

            int readableBytes = channel.read(byteBuffer);

            if (readableBytes == -1)
            {
                endRW.wrap(atomicBuffer, 0)
                     .streamId(streamId);

                if (!writeBuffer.write(endRW.typeId(), endRW.buffer(), endRW.offset(), endRW.remaining()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }

                selectionKey.cancel();
            }
            else
            {
                dataRW.remaining(readableBytes);

                if (!writeBuffer.write(dataRW.typeId(), dataRW.buffer(), dataRW.offset(), dataRW.remaining()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }
            }
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        return 1;
    }
}
