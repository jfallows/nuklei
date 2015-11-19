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
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataFW;
import org.kaazing.nuklei.tcp.internal.types.stream.EndFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.nio.TransportPoller;

public final class Reader extends TransportPoller implements Nukleus, Consumer<ReaderCommand>
{
    private static final int MAX_RECEIVE_LENGTH = 1024; // TODO: Configuration and Context

    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final OneToOneConcurrentArrayQueue<ReaderCommand> commandQueue;
    private final ByteBuffer byteBuffer;
    private final AtomicBuffer atomicBuffer;

    public Reader(Context context)
    {
        this.commandQueue = context.readerCommandQueue();
        this.byteBuffer = allocateDirect(MAX_RECEIVE_LENGTH).order(nativeOrder());
        this.atomicBuffer = new UnsafeBuffer(byteBuffer.duplicate());
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
                ReaderState state = (ReaderState) key.attachment();
                state.channel().shutdownInput();
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

    public void doRegister(long referenceId, long connectionId, SocketChannel channel, RingBuffer streamBuffer)
    {
        ReaderState state = new ReaderState(connectionId, channel, streamBuffer);

        BeginFW beginRO = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .streamId(state.streamId())
                                 .referenceId(referenceId)
                                 .build();

        streamBuffer.write(beginRO.typeId(), beginRO.buffer(), beginRO.offset(), beginRO.remaining());

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

            if (!streamBuffer.write(endRO.typeId(), endRO.buffer(), endRO.offset(), endRO.remaining()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }
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
            final ReaderState state = (ReaderState) selectionKey.attachment();
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

                if (!writeBuffer.write(endRO.typeId(), endRO.buffer(), endRO.offset(), endRO.remaining()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }

                selectionKey.cancel();
            }
            else if (bytesRead != 0)
            {
                DataFW dataRO = dataRW.payloadLength(bytesRead).build();

                if (!writeBuffer.write(dataRO.typeId(), dataRO.buffer(), dataRO.offset(), dataRO.remaining()))
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
