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

package org.kaazing.nuklei.protocol.tcp;

import org.kaazing.nuklei.NioSelectorNukleus;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferWriter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.IntSupplier;

/**
 */
public class TcpAcceptor
{
    private static final InetAddress[] WILDCARD_ADDRESS = new InetAddress[1];

    private final long id;
    private final TcpInterfaceAcceptor[] acceptors;
    private final MpscRingBufferWriter receiveWriter;
    private final NioSelectorNukleus selectorNukleus;
    private final MpscArrayBuffer<Object> tcpReaderCommandQueue;
    private final MpscArrayBuffer<Object> tcpSenderCommandQueue;
    private final MpscArrayBuffer<Object> tcpManagerCommandQueue;

    public TcpAcceptor(
        final int port,
        InetAddress[] interfaces,
        final long id,
        final MpscRingBufferWriter receiveWriter,
        final NioSelectorNukleus selectorNukleus,
        final MpscArrayBuffer<Object> tcpReaderCommandQueue,
        final MpscArrayBuffer<Object> tcpSenderCommandQueue,
        final MpscArrayBuffer<Object> tcpManagerCommandQueue)
    {
        this.id = id;
        this.receiveWriter = receiveWriter;
        this.selectorNukleus = selectorNukleus;
        this.tcpReaderCommandQueue = tcpReaderCommandQueue;
        this.tcpSenderCommandQueue = tcpSenderCommandQueue;
        this.tcpManagerCommandQueue = tcpManagerCommandQueue;

        try
        {
            if (interfaces.length == 0)
            {
                interfaces = WILDCARD_ADDRESS;
            }
            acceptors = new TcpInterfaceAcceptor[interfaces.length];

            for (int i = 0; i < acceptors.length; i++)
            {
                final ServerSocketChannel acceptor = ServerSocketChannel.open();
                acceptor.bind(new InetSocketAddress(interfaces[i], port));
                acceptor.configureBlocking(false);

                acceptors[i] = new TcpInterfaceAcceptor(acceptor);
                selectorNukleus.register(
                    acceptors[i].acceptor(), SelectionKey.OP_ACCEPT, composeAcceptor(acceptors[i]));
            }
        }
        catch (final Exception ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    public long id()
    {
        return id;
    }

    public MpscRingBufferWriter receiveWriter()
    {
        return receiveWriter;
    }

    public void close()
    {
        for (final TcpInterfaceAcceptor acceptor : acceptors)
        {
            selectorNukleus.cancel(acceptor.acceptor(), SelectionKey.OP_ACCEPT);
            acceptor.close();
        }
        try
        {
            // Call select(Now) so canceled selector keys are removed from the selector. This is
            // necessary on some platforms (Windows) to allow the ServerSocketChannel to be fully unbound.
            selectorNukleus.selector.selectNow();
        }
        catch (IOException e)
        {
            // Nothing useful we can do here
        }
    }

    private int onAcceptable(final SocketChannel channel)
    {
        final long id = tcpManagerCommandQueue.nextId();

        try
        {
            channel.configureBlocking(false);
        }
        catch (final Exception ex)
        {
            ex.printStackTrace();  // TODO: temporary
        }

        final TcpConnection connection = new TcpConnection(channel, id, receiveWriter);

        // pass transport off to other nukleus' to process.
        // First to sender then will be passed to receiver
        if (!tcpSenderCommandQueue.write(connection))
        {
            throw new IllegalStateException("could not write to command queue");
        }

        return 1;
    }

    private IntSupplier composeAcceptor(final TcpInterfaceAcceptor acceptor)
    {
        return () ->
        {
            try
            {
                return onAcceptable(acceptor.acceptor().accept());
            }
            catch (final Exception ex)
            {
                ex.printStackTrace();  // TODO: temporary
            }

            return 0;
        };
    }

    private static class TcpInterfaceAcceptor
    {
        final ServerSocketChannel acceptor;

        TcpInterfaceAcceptor(final ServerSocketChannel acceptor)
        {
            this.acceptor = acceptor;
        }

        public void close()
        {
            try
            {
                acceptor.close();
            }
            catch (final Exception ex)
            {
                throw new IllegalStateException(ex);
            }
        }

        public ServerSocketChannel acceptor()
        {
            return acceptor;
        }
    }
}
