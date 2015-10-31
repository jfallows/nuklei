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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.reader;

import static java.nio.channels.SelectionKey.OP_READ;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginRW;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.nio.TransportPoller;

public final class Reader extends TransportPoller implements Nukleus, Consumer<ReaderCommand>
{
    private static final int SEND_BUFFER_CAPACITY = 1024; // TODO: Configuration and Context

    private final BeginRW beginRW = new BeginRW();

    private final OneToOneConcurrentArrayQueue<ReaderCommand> commandQueue;
    private final AtomicBuffer sendBuffer;

    public Reader(Context context)
    {
        this.commandQueue = context.readerCommandQueue();
        this.sendBuffer = new UnsafeBuffer(new byte[SEND_BUFFER_CAPACITY]);
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

    public void doRegister(long bindingRef, long connectionId, SocketChannel channel, RingBuffer writeBuffer)
    {
        try
        {
            ReaderInfo info = new ReaderInfo(connectionId, channel, writeBuffer);
            channel.register(selector, OP_READ, info);

            beginRW.wrap(sendBuffer, 0)
                   .bindingRef(bindingRef)
                   .connectionId(connectionId);

            writeBuffer.write(beginRW.type(), beginRW.buffer(), beginRW.offset(), beginRW.remaining());
        }
        catch (ClosedChannelException e)
        {
            // channel already closed
        }
    }

    private int processRead(SelectionKey selectionKey)
    {
        // TODO
        return 1;
    }
}
