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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.RuleChain.outerRule;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.nuklei.DedicatedNuklei;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferReader;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * Basic tests using the k3po/specification.tcp project to validate
 * TCP functionality
 */
public class TcpSpecificationIT
{
    private static final int MANAGER_COMMAND_QUEUE_SIZE = 1024;
    private static final int MANAGER_SEND_BUFFER_SIZE = 64*1024 + MpscRingBuffer.STATE_TRAILER_SIZE;
    private static final int RECEIVE_BUFFER_SIZE = 64*1024 + MpscRingBuffer.STATE_TRAILER_SIZE;
    private static final int PORT = 8080;
    private static final int CONNECT_PORT = 8080;
    private static final int SEND_BUFFER_SIZE = 1024;
    private static final int MAGIC_PAYLOAD_INT = 8;

    private final MpscArrayBuffer<Object> managerCommandQueue = new MpscArrayBuffer<>(MANAGER_COMMAND_QUEUE_SIZE);
    private final AtomicBuffer managerSendBuffer = new UnsafeBuffer(ByteBuffer.allocate(MANAGER_SEND_BUFFER_SIZE));

    private final AtomicBuffer receiveBuffer = new UnsafeBuffer(ByteBuffer.allocate(RECEIVE_BUFFER_SIZE));
    private final MpscRingBufferReader receiver = new MpscRingBufferReader(receiveBuffer);
    private final ByteBuffer sendChannelBuffer = ByteBuffer.allocate(SEND_BUFFER_SIZE).order(ByteOrder.nativeOrder());
    private final AtomicBuffer sendAtomicBuffer = new UnsafeBuffer(ByteBuffer.allocate(SEND_BUFFER_SIZE));
    private final ByteBuffer receiveChannelBuffer = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE).order(ByteOrder.nativeOrder());

    private TcpManager tcpManager;
    private TcpManagerProxy tcpManagerProxy;
    private DedicatedNuklei dedicatedNuklei;

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/tcp/rfc793");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Before
    public void setUp() throws Exception
    {
        tcpManager = new TcpManager(managerCommandQueue, managerSendBuffer);
        tcpManagerProxy = new TcpManagerProxy(managerCommandQueue, managerSendBuffer);
        dedicatedNuklei = new DedicatedNuklei("TCP-manager-dedicated");
    }

    @Test
    @Specification
    ({
        "establish.connection/tcp.client"
    })
    public void establishConnectionFromClient() throws Exception
    {
        final AtomicBoolean isListening = new AtomicBoolean(false);
        Thread testThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // Start a listener, expect the client to connect
                tcpManager.launch(dedicatedNuklei);

                long attachId = tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);
                int messages = 0;
                while (messages == 0)
                {
                    messages = receiver.read((typeId, buffer, offset, length) ->
                    {
                        assertThat(typeId, is(TcpManagerTypeId.ATTACH_COMPLETED));
                        assertThat(buffer.getLong(offset), is(attachId));
                    },
                    1);

                    if (messages == 0)
                    {
                        Thread.yield();
                    }
                }
                assertThat(messages, is(1));

                isListening.lazySet(true); // to indicate the port is now bound

                messages = 0;
                while (messages == 0)
                {
                    messages = receiver.read((typeId, buffer, offset, length) ->
                    {
                        assertThat(typeId, is(TcpManagerTypeId.NEW_CONNECTION));
                    },
                    1);

                    if (messages == 0)
                    {
                        Thread.yield();
                    }
                }
                assertThat(messages, is(1));
            }
        },
        "establishConnectionFromClient-thread");

        testThread.start();

        while (!isListening.get())
        {
            Thread.yield(); // busy spin to give test thread a chance to bind
        }

        k3po.join();
    }

    @Test
    @Specification("establish.connection/tcp.server")
    public void establishConnectionToServer() throws Exception
    {
        Thread testThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // configure the manager to connect to the expected server port
                try
                {
                    tcpManager.launch(dedicatedNuklei);

                    long attachId = tcpManagerProxy.attach(
                            0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

                    int messages = 0;
                    while (messages == 0)
                    {
                        messages = receiver.read((typeId, buffer, offset, length) ->
                        {
                            assertThat(typeId, is(TcpManagerTypeId.ATTACH_COMPLETED));
                            assertThat(buffer.getLong(offset), is(attachId));
                        },
                        1);

                        if (messages == 0)
                        {
                            Thread.yield();
                        }
                    }
                    assertThat(messages, is(1));

                    messages = 0;
                    while (messages == 0)
                    {
                        messages = receiver.read((typeId, buffer, offset, length) ->
                        {
                            assertThat(typeId, is(TcpManagerTypeId.NEW_CONNECTION));
                            assertThat(buffer.getLong(offset), is(attachId));
                        },
                        1);

                        if (messages == 0)
                        {
                            Thread.yield();
                        }
                    }
                    assertThat(messages, is(1));

                    tcpManagerProxy.detach(attachId);
                }
                catch (UnknownHostException ex)
                {
                    throw new RuntimeException("Error getting address for localhost", ex);
                }
            }
        },
        "establishConnectionToServer-thread");

        testThread.start();

        k3po.join();

        testThread.join();
    }
}
