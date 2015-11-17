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

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public class WriterInfo
{
    private final long bindingRef;
    private final long streamId;
    private final SocketChannel channel;
    private final ByteBuffer sendBuffer;

    public WriterInfo(
        long bindingRef,
        long streamId,
        SocketChannel channel,
        RingBuffer readBuffer)
    {
        this.bindingRef = bindingRef;
        this.streamId = streamId;
        this.channel = channel;

        AtomicBuffer sendAtomicBuffer = readBuffer.buffer();
        byte[] sendByteArray = sendAtomicBuffer.byteArray();
        this.sendBuffer = (sendByteArray != null) ? ByteBuffer.wrap(sendByteArray) : sendAtomicBuffer.byteBuffer().duplicate();
    }

    public long bindingRef()
    {
        return this.bindingRef;
    }

    public long streamId()
    {
        return this.streamId;
    }

    public SocketChannel channel()
    {
        return channel;
    }

    public ByteBuffer sendBuffer()
    {
        return sendBuffer;
    }

    @Override
    public String toString()
    {
        return String.format("[streamId=%d, channel=%s]", streamId(), channel());
    }

}
