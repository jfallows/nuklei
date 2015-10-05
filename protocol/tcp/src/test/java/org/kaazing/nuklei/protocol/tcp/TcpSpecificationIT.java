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

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.RuleChain.outerRule;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
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

import uk.co.real_logic.agrona.BitUtil;
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

    private final MpscArrayBuffer<Object> managerCommandQueue = new MpscArrayBuffer<>(MANAGER_COMMAND_QUEUE_SIZE);
    private final AtomicBuffer managerSendBuffer = new UnsafeBuffer(ByteBuffer.allocate(MANAGER_SEND_BUFFER_SIZE));

    private final AtomicBuffer receiveBuffer = new UnsafeBuffer(ByteBuffer.allocate(RECEIVE_BUFFER_SIZE));
    private final MpscRingBufferReader receiver = new MpscRingBufferReader(receiveBuffer);
    private final AtomicBuffer sendAtomicBuffer = new UnsafeBuffer(ByteBuffer.allocate(SEND_BUFFER_SIZE));

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

    @After
    public void tearDown() throws Exception
    {
        if (dedicatedNuklei != null)
        {
            dedicatedNuklei.stop();
        }

        if (tcpManager != null)
        {
            tcpManager.close();
        }
    }

    @Test
    @Specification("establish.connection/tcp.client")
    public void establishConnectionFromClient() throws Exception
    {
        // Start a listener, expect the client to connect
        tcpManager.launch(dedicatedNuklei);

        long attachId = tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // Expect the port to be properly bound
        expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);

        // start k3po so connects happen
        k3po.start();

        // Expect the client to connect
        expectMessage(TcpManagerTypeId.NEW_CONNECTION, (Long) null);

        k3po.finish();
    }

    @Test
    @Specification("establish.connection/tcp.server")
    public void establishConnectionToServer() throws Exception
    {
        // configure the manager to connect to the expected server port
        try
        {
            tcpManager.launch(dedicatedNuklei);

            long attachId = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId);
        }
        catch (UnknownHostException ex)
        {
            throw new RuntimeException("Error getting address for localhost", ex);
        }

        k3po.finish();
    }

    @Test
    @Specification("bidirectional.data/tcp.client")
    public void bidirectionDataFlowWithClient() throws Exception
    {
        // Start a listener, expect the client to connect
        tcpManager.launch(dedicatedNuklei);

        long attachId = tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // Expect the port to be properly bound
        expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);

        // start k3po so connects happen
        k3po.start();

        // Expect the client to connect
        long[] connectionId = new long[1];
        expectMessage(TcpManagerTypeId.NEW_CONNECTION, connectionId);

        // as per rupert script, receive "client data 1"
        expectMessage(TcpManagerTypeId.RECEIVED_DATA, connectionId[0], "client data 1");

        // as per rupert script, send "server data 1"
        byte[] toSend = "server data 1".getBytes(Charset.forName("UTF-8"));
        sendAtomicBuffer.putLong(0, connectionId[0]); // set connection ID
        sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
        tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

        // as per rupert script, receive "client data 2"
        expectMessage(TcpManagerTypeId.RECEIVED_DATA, connectionId[0], "client data 2");

        // as per rupert script, send "server data 2"
        toSend = "server data 2".getBytes(Charset.forName("UTF-8"));
        sendAtomicBuffer.putLong(0, connectionId[0]); // set connection ID
        sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
        tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

        k3po.finish();
    }

    @Test
    @Specification("bidirectional.data/tcp.server")
    public void bidirectionDataFlowWithServer() throws Exception
    {
        // configure the manager to connect to the expected server port
        try
        {
            tcpManager.launch(dedicatedNuklei);
            long attachId = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId);

            // as per rupert script, send "client data 1"
            byte[] toSend = "client data 1".getBytes(Charset.forName("UTF-8"));
            sendAtomicBuffer.putLong(0, attachId); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

            // as per rupert script, receive "server data 1"
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, attachId, "server data 1");

            // as per rupert script, send "client data 2"
            toSend = "client data 2".getBytes(Charset.forName("UTF-8"));
            sendAtomicBuffer.putLong(0, attachId); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

            // receive "server data 2"
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, attachId, "server data 2");
        }
        catch (UnknownHostException ex)
        {
            throw new RuntimeException("Error getting address for localhost", ex);
        }

        k3po.finish();
    }

    @Test
    @Specification("client.close/tcp.client")
    public void shouldReceiveConnectionThenCloseFromClient() throws Exception
    {
        // Start a listener, expect the client to connect
        tcpManager.launch(dedicatedNuklei);

        long attachId = tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // Expect the port to be properly bound
        expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);

        // start k3po so connects happen
        k3po.start();

        // Expect the client to connect
        long[] connectionId = new long[1];
        expectMessage(TcpManagerTypeId.NEW_CONNECTION, connectionId);

        // Expect the client to close
        expectMessage(TcpManagerTypeId.EOF, connectionId[0]);

        k3po.finish();
    }

    @Test
    @Specification("client.close/tcp.server")
    public void shouldConnectThenClose() throws Exception
    {
        k3po.start();

        // configure the manager to connect to the expected server port
        try
        {
            tcpManager.launch(dedicatedNuklei);
            long attachId = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId);

            // as per rupert script, send close
            tcpManagerProxy.closeConnection(attachId, true);
        }
        catch (UnknownHostException ex)
        {
            throw new RuntimeException("Error getting address for localhost", ex);
        }

        k3po.finish();
    }

    @Test
    @Specification("client.sent.data/tcp.client")
    public void shouldReceiveDataFromClient() throws Exception
    {
        // Start a listener, expect the client to connect
        tcpManager.launch(dedicatedNuklei);

        long attachId = tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // Expect the port to be properly bound
        expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);

        // start k3po so connects happen
        k3po.start();

        // Expect the client to connect
        long[] connectionId = new long[1];
        expectMessage(TcpManagerTypeId.NEW_CONNECTION, connectionId);

        // as per rupert script, receive "client data"
        expectMessage(TcpManagerTypeId.RECEIVED_DATA, connectionId[0], "client data");

        k3po.finish();
    }

    @Test
    @Specification("client.sent.data/tcp.server")
    public void shouldSendDataToServer() throws Exception
    {
        // configure the manager to connect to the expected server port
        try
        {
            tcpManager.launch(dedicatedNuklei);
            long attachId = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId);

            // as per rupert script, send "client data 1"
            byte[] toSend = "client data".getBytes(Charset.forName("UTF-8"));
            sendAtomicBuffer.putLong(0, attachId); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);
        }
        catch (UnknownHostException ex)
        {
            throw new RuntimeException("Error getting address for localhost", ex);
        }

        k3po.finish();
    }

    @Test
    @Specification("concurrent.connections/tcp.client")
    public void shouldReceiveDataFromMultipleClients() throws Exception
    {
        // Start a listener, expect the client to connect
        tcpManager.launch(dedicatedNuklei);

        long attachId = tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // Expect the port to be properly bound
        expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);

        // start k3po so connects happen
        k3po.start();

        processMessages(3); // expect 3 connections

        k3po.finish();
    }

    @Test
    @Specification("concurrent.connections/tcp.server")
    public void shouldSendDataFromMultipleClients() throws Exception
    {
        // configure the manager to connect to the expected server port
        try
        {
            tcpManager.launch(dedicatedNuklei);

            // create 3 connections
            long attachId1 = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId1);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId1);

            long attachId2 = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId2);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId2);

            long attachId3 = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId3);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId3);

            Set<Long> expectedAttachIds = new HashSet<Long>();
            expectedAttachIds.add(attachId1);
            expectedAttachIds.add(attachId2);
            expectedAttachIds.add(attachId3);

            // send Hello from each client
            byte[] toSend = "Hello".getBytes(Charset.forName("UTF-8"));
            sendAtomicBuffer.putLong(0, attachId1); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

            sendAtomicBuffer.putLong(0, attachId2); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

            sendAtomicBuffer.putLong(0, attachId3); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

            // On each client expect Hello back
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, expectedAttachIds, "Hello");
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, expectedAttachIds, "Hello");
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, expectedAttachIds, "Hello");

            // send Goodbye from each client
            toSend = "Goodbye".getBytes(Charset.forName("UTF-8"));
            sendAtomicBuffer.putLong(0, attachId1); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

            sendAtomicBuffer.putLong(0, attachId2); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

            sendAtomicBuffer.putLong(0, attachId3); // set connection ID
            sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
            tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

            // On each client expect Goodbye back
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, expectedAttachIds, "Goodbye");
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, expectedAttachIds, "Goodbye");
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, expectedAttachIds, "Goodbye");

            // close each client
            tcpManagerProxy.closeConnection(attachId1, true);
            tcpManagerProxy.closeConnection(attachId2, true);
            tcpManagerProxy.closeConnection(attachId3, true);
        }
        catch (UnknownHostException ex)
        {
            throw new RuntimeException("Error getting address for localhost", ex);
        }

        k3po.finish();
    }

    @Test
    @Specification("server.close/tcp.client")
    public void shouldCloseConnectionToClient() throws Exception
    {
        // Start a listener, expect the client to connect
        tcpManager.launch(dedicatedNuklei);

        long attachId = tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // Expect the port to be properly bound
        expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);

        // start k3po so connects happen
        k3po.start();

        // Expect the client to connect
        long[] connectionId = new long[1];
        expectMessage(TcpManagerTypeId.NEW_CONNECTION, connectionId);

        tcpManagerProxy.closeConnection(connectionId[0], true);

        k3po.finish();
    }

    @Test
    @Specification("server.close/tcp.server")
    public void shouldReceiveCloseFromServer() throws Exception
    {
        // configure the manager to connect to the expected server port
        try
        {
            tcpManager.launch(dedicatedNuklei);

            long attachId = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId);
            expectMessage(TcpManagerTypeId.EOF, attachId);
        }
        catch (UnknownHostException ex)
        {
            throw new RuntimeException("Error getting address for localhost", ex);
        }

        k3po.finish();
    }

    @Test
    @Specification("server.sent.data/tcp.client")
    public void shouldSendDataToClient() throws Exception
    {
        // Start a listener, expect the client to connect
        tcpManager.launch(dedicatedNuklei);

        long attachId = tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // Expect the port to be properly bound
        expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);

        // start k3po so connects happen
        k3po.start();

        // Expect the client to connect
        long[] connectionId = new long[1];
        expectMessage(TcpManagerTypeId.NEW_CONNECTION, connectionId);

        // as per rupert script, send "server data"
        byte[] toSend = "server data".getBytes(Charset.forName("UTF-8"));
        sendAtomicBuffer.putLong(0, connectionId[0]); // set connection ID
        sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, toSend);
        tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + toSend.length);

        k3po.finish();
    }

    @Test
    @Specification("server.sent.data/tcp.server")
    public void shouldReceiveDataFromServer() throws Exception
    {
        // configure the manager to connect to the expected server port
        try
        {
            tcpManager.launch(dedicatedNuklei);
            long attachId = tcpManagerProxy.attach(
                    0, InetAddress.getByName("0.0.0.0"), CONNECT_PORT, InetAddress.getLoopbackAddress(), receiveBuffer);

            // Expect the connection to succeed as an attach/new connection
            expectMessage(TcpManagerTypeId.ATTACH_COMPLETED, attachId);
            expectMessage(TcpManagerTypeId.NEW_CONNECTION, attachId);

            // as per rupert script, expect "server data"
            expectMessage(TcpManagerTypeId.RECEIVED_DATA, attachId, "server data");
        }
        catch (UnknownHostException ex)
        {
            throw new RuntimeException("Error getting address for localhost", ex);
        }

        k3po.finish();
    }

    private void expectMessage(int messageType, Long expectedAttachId)
    {
        Set<Long> expectedAttachIds = ((expectedAttachId == null) ?
                null : Collections.singleton(expectedAttachId));
        expectMessage(messageType, expectedAttachIds, null, null);
    }

    private void expectMessage(int messageType, long[] connectionId)
    {
        expectMessage(messageType, null, connectionId, null);
    }

    private void expectMessage(int messageType, Long connectionId, String message)
    {
        expectMessage(messageType, Collections.singleton(connectionId), null, message);
    }

    private void expectMessage(int messageType, Set<Long> expectedAttachId, String message)
    {
        expectMessage(messageType, expectedAttachId, null, message);
    }

    private void expectMessage(int messageType, Set<Long> expectedAttachIds, long[] connectionId, String message)
    {
        int messages = readOneMessage((typeId, buffer, offset, length) ->
        {
            assertThat(typeId, is(messageType));
            if (expectedAttachIds != null)
            {
                long actualAttachId = buffer.getLong(offset);
                assertThat("Received attachId: " + actualAttachId + " which was not in the expected list: " +
                        expectedAttachIds, expectedAttachIds.contains(actualAttachId));
            }
            else if (connectionId != null)
            {
                connectionId[0] = buffer.getLong(offset);
            }

            if (message != null)
            {
                byte[] receivedMessageBytes = new byte[length - BitUtil.SIZE_OF_LONG];
                buffer.getBytes(offset + BitUtil.SIZE_OF_LONG, receivedMessageBytes, 0, length - BitUtil.SIZE_OF_LONG);
                String receivedMessage = new String(receivedMessageBytes);
                assertThat(format("Received: '%s' which does not match expected message: '%s'", receivedMessage, message),
                        message.equals(receivedMessage));
            }
        });
        assertThat(messages, is(1));
    }

    private void processMessages(int connectionCount)
    {
        final AtomicInteger closedConnections = new AtomicInteger(0);
        while (closedConnections.get() < connectionCount)
        {
            readOneMessage((typeId, buffer, offset, length) ->
            {
                if (typeId == TcpManagerTypeId.NEW_CONNECTION)
                {
                    // do we need to track the connection id?  This is request/response
                    // so I don't think so as the new message will contain the ID
                    // that the response needs to be written on
                }
                else if (typeId == TcpManagerTypeId.EOF)
                {
                    closedConnections.incrementAndGet();
                }
                else if (typeId == TcpManagerTypeId.RECEIVED_DATA)
                {
                    long connectionId = buffer.getLong(offset);

                    byte[] receivedMessageBytes = new byte[length - BitUtil.SIZE_OF_LONG];
                    buffer.getBytes(offset + BitUtil.SIZE_OF_LONG, receivedMessageBytes, 0, length - BitUtil.SIZE_OF_LONG);

                    // as per rupert script, send "client data 1"
                    sendAtomicBuffer.putLong(0, connectionId); // set connection ID
                    sendAtomicBuffer.putBytes(BitUtil.SIZE_OF_LONG, receivedMessageBytes);
                    tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + receivedMessageBytes.length);
                }
            });
        }
    }

    private int readOneMessage(final MpscRingBufferReader.ReadHandler handler)
    {
        int messages;
        while ((messages = receiver.read(handler, 1)) == 0)
        {
            Thread.yield();
            if (Thread.currentThread().isInterrupted())
            {
                throw new RuntimeException("thread is interrupted trying to read one message");
            }
        }
        return messages;
    }
}
