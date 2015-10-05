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
package org.kaazing.nuklei.protocol.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

import org.kaazing.nuklei.MessagingNukleus;
import org.kaazing.nuklei.NioSelectorNukleus;
import org.kaazing.nuklei.Nuklei;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferWriter;
import org.kaazing.nuklei.protocol.tcp.command.TcpCloseConnectionCmd;
import org.kaazing.nuklei.protocol.tcp.command.TcpDetachCmd;
import org.kaazing.nuklei.protocol.tcp.command.TcpLocalAttachCmd;
import org.kaazing.nuklei.protocol.tcp.command.TcpRemoteAttachCmd;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 */
public class TcpManager
{
    private static final int MPSC_READ_LIMIT = 10;
    private static final int TCP_READER_COMMAND_QUEUE_SIZE = 1024;
    private static final int TCP_SENDER_COMMAND_QUEUE_SIZE = 1024;

    private final MessagingNukleus messagingNukleus;
    private final NioSelectorNukleus acceptNioSelectorNukleus;
    private final MpscArrayBuffer<Object> tcpReceiverCommandQueue;
    private final MpscArrayBuffer<Object> tcpSenderCommandQueue;
    private final MpscArrayBuffer<Object> tcpManagerCommandQueue;
    private final TcpReceiver tcpReceiver;
    private final TcpSender tcpSender;
    private final Map<Long, TcpAcceptor> localAttachesByIdMap;
    private final Map<Long, TcpConnection> remoteAttachesByIdMap;
    private final MutableDirectBuffer informingBuffer;

    public TcpManager(final MpscArrayBuffer<Object> commandQueue, final AtomicBuffer sendBuffer)
    {
        try
        {
            tcpManagerCommandQueue = commandQueue;
            acceptNioSelectorNukleus = new NioSelectorNukleus(Selector.open());
            tcpReceiverCommandQueue = new MpscArrayBuffer<>(TCP_READER_COMMAND_QUEUE_SIZE);
            tcpSenderCommandQueue = new MpscArrayBuffer<>(TCP_SENDER_COMMAND_QUEUE_SIZE);

            final MessagingNukleus.Builder builder = new MessagingNukleus.Builder()
                .mpscArrayBuffer(commandQueue, this::commandHandler, MPSC_READ_LIMIT)
                .nioSelector(acceptNioSelectorNukleus);

            messagingNukleus = builder.build();

            final NioSelectorNukleus receiveNioSelectorNukleus = new NioSelectorNukleus(Selector.open());
            final NioSelectorNukleus sendNioSelectorNukleus = new NioSelectorNukleus(Selector.open());

            tcpReceiver =
                new TcpReceiver(
                    tcpReceiverCommandQueue,
                    receiveNioSelectorNukleus,
                    tcpManagerCommandQueue,
                    tcpSenderCommandQueue);

            tcpSender =
                new TcpSender(
                    tcpSenderCommandQueue,
                    sendBuffer,
                    sendNioSelectorNukleus,
                    tcpManagerCommandQueue,
                    tcpReceiverCommandQueue);

            localAttachesByIdMap = new HashMap<>();
            remoteAttachesByIdMap = new HashMap<>();
            informingBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BitUtil.SIZE_OF_LONG));
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public void launch(final Nuklei nuklei)
    {
        nuklei.spinUp(messagingNukleus);
        tcpReceiver.launch(nuklei);
        tcpSender.launch(nuklei);
    }

    public void launch(final Nuklei manageNuklei, final Nuklei receiverNuklei, final Nuklei senderNuklei)
    {
        manageNuklei.spinUp(messagingNukleus);
        tcpReceiver.launch(receiverNuklei);
        tcpSender.launch(senderNuklei);
    }

    public void close()
    {
        localAttachesByIdMap.forEach((id, acceptor) -> acceptor.close());
    }

    private void commandHandler(final Object obj)
    {
        if (obj instanceof TcpLocalAttachCmd)
        {
            final TcpLocalAttachCmd cmd = (TcpLocalAttachCmd) obj;

            final MpscRingBufferWriter receiveWriter = new MpscRingBufferWriter(cmd.receiveBuffer());

            final TcpAcceptor acceptor =
                new TcpAcceptor(
                    cmd.port(),
                    cmd.addresses(),
                    cmd.id(),
                    receiveWriter,
                    acceptNioSelectorNukleus,
                    tcpReceiverCommandQueue,
                    tcpSenderCommandQueue,
                    tcpManagerCommandQueue);

            localAttachesByIdMap.put(cmd.id(), acceptor);
            informOfAttachStatus(receiveWriter, TcpManagerTypeId.ATTACH_COMPLETED, cmd.id());
        }
        else if (obj instanceof TcpRemoteAttachCmd)
        {
            final TcpRemoteAttachCmd cmd = (TcpRemoteAttachCmd) obj;

            final MpscRingBufferWriter receiverWriter = new MpscRingBufferWriter(cmd.receiveBuffer());

            final TcpConnection connection = new TcpConnection(
                cmd.id(),
                cmd.localAddress(),
                receiverWriter);

            remoteAttachesByIdMap.put(cmd.id(), connection);
            try
            {
                if (connection.channel().connect(cmd.remoteAddress()))
                {
                    onConnect(connection);
                }
                else
                {
                    acceptNioSelectorNukleus.register(
                        connection.channel(), SelectionKey.OP_CONNECT, composeConnector(connection));
                }
            }
            catch (final IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }
        else if (obj instanceof TcpDetachCmd)
        {
            final TcpDetachCmd cmd = (TcpDetachCmd) obj;
            final TcpAcceptor acceptor = localAttachesByIdMap.remove(cmd.id());

            if (null != acceptor)
            {
                acceptor.close();
                informOfAttachStatus(acceptor.receiveWriter(), TcpManagerTypeId.DETACH_COMPLETED, acceptor.id());
            }
        }
        else if (obj instanceof TcpCloseConnectionCmd)
        {
            final TcpCloseConnectionCmd cmd = (TcpCloseConnectionCmd) obj;

            if (!tcpReceiverCommandQueue.write(cmd))
            {
                throw new IllegalStateException("could not write to command queue");
            }

            if (cmd.isImmediate())
            {
                if (!tcpSenderCommandQueue.write(cmd))
                {
                    throw new IllegalStateException("could not write to command queue");
                }
            }
        }
        else if (obj instanceof TcpConnection)
        {
            final TcpConnection connection = (TcpConnection) obj;

            if (connection.hasSenderClosed() && connection.hasReceiverClosed() && !connection.isClosed())
            {
                connection.close();
            }
        }
    }

    private void informOfAttachStatus(final MpscRingBufferWriter writer, final int status, final long id)
    {
        informingBuffer.putLong(0, id);

        if (!writer.write(TcpManagerTypeId.ATTACH_COMPLETED, informingBuffer, 0, BitUtil.SIZE_OF_LONG))
        {
            throw new IllegalStateException("could not write to receive buffer");
        }
    }

    private IntSupplier composeConnector(final TcpConnection connection)
    {
        return () -> onConnect(connection);
    }

    private int onConnect(final TcpConnection connection)
    {
        try
        {
            connection.channel().finishConnect();
            informOfAttachStatus(connection.receiveWriter(), TcpManagerTypeId.ATTACH_COMPLETED, connection.id());

            // pass transport off to other nukleus' to process.
            // First to sender then will be passed to receiver
            if (!tcpSenderCommandQueue.write(connection))
            {
                throw new IllegalStateException("could not write to command queue");
            }

            // remove from the map, treated as a normal connection hereafter
            remoteAttachesByIdMap.remove(connection.id());
        }
        catch (final IOException ex)
        {
            throw new RuntimeException(ex);
        }

        return 1;
    }

}
