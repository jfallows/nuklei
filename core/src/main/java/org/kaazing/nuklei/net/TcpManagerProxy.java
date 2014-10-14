/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
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

package org.kaazing.nuklei.net;

import org.kaazing.nuklei.BitUtil;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferWriter;
import org.kaazing.nuklei.net.command.TcpCloseConnectionCmd;
import org.kaazing.nuklei.net.command.TcpDetachCmd;
import org.kaazing.nuklei.net.command.TcpLocalAttachCmd;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Interface for sending commands to a {@link TcpManager}
 */
public class TcpManagerProxy
{
    private final MpscArrayBuffer<Object> commandQueue;
    private final AtomicBuffer sendBuffer;
    private final MpscRingBufferWriter sendWriter;
    private final ThreadLocal<AtomicBuffer> threadLocalBuffer = ThreadLocal.withInitial(
        () -> new AtomicBuffer(ByteBuffer.allocateDirect(BitUtil.SIZE_OF_LONG)));

    public TcpManagerProxy(final MpscArrayBuffer<Object> commandQueue, final AtomicBuffer sendBuffer)
    {
        this.commandQueue = commandQueue;
        this.sendBuffer = sendBuffer;
        this.sendWriter = new MpscRingBufferWriter(sendBuffer);
    }

    /**
     * Local Attach
     *
     * @param port to bind to
     * @param addresses to bind to
     * @param receiveBuffer to place received data from connections
     * @return id to use for {@link #detach(long)}
     */
    public long attach(
        final int port,
        final InetAddress[] addresses,
        final AtomicBuffer receiveBuffer)
    {
        final long id = commandQueue.nextId();
        final TcpLocalAttachCmd cmd = new TcpLocalAttachCmd(port, id, addresses, receiveBuffer);

        if (!commandQueue.write(cmd))
        {
            throw new IllegalStateException("could not write command");
        }

        return id;
    }

    /**
     * Detach (local or remote)
     *
     * @param attachId of the attach command
     */
    public void detach(final long attachId)
    {
        final TcpDetachCmd cmd = new TcpDetachCmd(attachId);

        if (!commandQueue.write(cmd))
        {
            throw new IllegalStateException("could not write command");
        }
    }

    /**
     * Send a message from the given {@link TcpManager}.
     *
     * The message must be formatted to begin with a long that contains the connection id
     * followed by the actual data itself.
     *
     * @param buffer containing the message and connection id
     * @param offset with the buffer that the connection id starts
     * @param length of the connection id plus the data
     */
    public void send(final AtomicBuffer buffer, final int offset, final int length)
    {
        if (!sendWriter.write(TcpManagerTypeId.SEND_DATA, buffer, offset, length))
        {
            throw new IllegalStateException("could not write to send buffer");
        }
    }

    /**
     * Direct interface for sending events to the {@link TcpSender}.
     *
     * @param typeId of the event
     * @param buffer for the event contents
     * @param offset within the buffer for the contents
     * @param length of the event contents in bytes
     * @return success or failure
     */
    public boolean write(final int typeId, final AtomicBuffer buffer, final int offset, final int length)
    {
        return sendWriter.write(typeId, buffer, offset, length);
    }

    /**
     * Close existing connection.
     *
     * @param connectionId to close
     * @param isImmediate should flush remaining sent data or not
     */
    public void closeConnection(final long connectionId, final boolean isImmediate)
    {
        final TcpCloseConnectionCmd cmd = new TcpCloseConnectionCmd(connectionId, isImmediate);

        if (!commandQueue.write(cmd))
        {
            throw new IllegalStateException("could not write command");
        }

        if (!isImmediate)
        {
            final AtomicBuffer buffer = threadLocalBuffer.get();

            buffer.putLong(0, connectionId);
            if (!sendWriter.write(TcpManagerTypeId.CLOSE_CONNECTION, buffer, 0, BitUtil.SIZE_OF_LONG))
            {
                throw new IllegalStateException("could not write to send buffer");
            }
        }
    }

    /*
     * variants
     * - remote attach (connect), no local bind, array buffer
     * - remote attach (connect), local bind, array buffer
     * - remote attach (connect), no local bind, ring buffer
     * - remote attach (connect), local bind, ring buffer
     *
     * - local attach (onAcceptable), array buffer
     * - local attach (onAcceptable), ring buffer
     */

}
