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
package org.kaazing.nuklei.amqp_1_0.connection;

import static org.junit.Assert.assertSame;
import static org.kaazing.nuklei.amqp_1_0.codec.transport.Header.AMQP_PROTOCOL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.Test;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Close;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Open;
import org.kaazing.nuklei.amqp_1_0.function.FrameConsumer;
import org.kaazing.nuklei.amqp_1_0.function.HeaderConsumer;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;
import org.kaazing.nuklei.concurrent.AtomicBuffer;

public class ConnectionStateMachineTest {

    private final ConnectionHooks connectionHooks = new ConnectionHooks();
    private final ConnectionStateMachine stateMachine = new ConnectionStateMachine(connectionHooks);
    private final Connection connection = new Connection(stateMachine, mock(Sender.class), new AtomicBuffer(new byte[0]));
    private final Header header = Header.LOCAL_REF.get().wrap(new AtomicBuffer(new byte[Header.SIZEOF_HEADER]), 0);
    private final Frame frame = Frame.LOCAL_REF.get().wrap(new AtomicBuffer(new byte[64]), 0);
    private final Open open = Open.LOCAL_REF.get();
    private final Close close = Close.LOCAL_REF.get();

    @Test
    @SuppressWarnings("unchecked")
    public void shouldInitializeInStartState() {
        connectionHooks.whenInitialized = mock(Consumer.class);

        stateMachine.start(connection);

        assertSame(ConnectionState.START, connection.state);
        
        verify(connectionHooks.whenInitialized).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromStartToHeaderReceivedWhenReceiveHeader() {
        connectionHooks.whenHeaderReceived = mock(HeaderConsumer.class);
        connection.state = ConnectionState.START;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.HEADER_RECEIVED, connection.state);

        verify(connectionHooks.whenHeaderReceived).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromStartToHeaderSentWhenSendHeader() {
        connectionHooks.whenHeaderSent = mock(HeaderConsumer.class);
        connection.state = ConnectionState.START;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.HEADER_SENT, connection.state);

        verify(connectionHooks.whenHeaderSent).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToHeaderExchangedWhenReceiveHeader() {
        connectionHooks.whenHeaderReceived = mock(HeaderConsumer.class);

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        connection.state = ConnectionState.HEADER_SENT;
        connection.headerSent = header.buffer().getLong(header.offset());
        
        stateMachine.received(connection, header);

        assertSame(ConnectionState.HEADER_EXCHANGED, connection.state);

        verify(connectionHooks.whenHeaderReceived).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToEndWhenReceiveInconsistentHeader() {
        connectionHooks.whenHeaderReceivedNotEqualSent = mock(HeaderConsumer.class);

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        connection.state = ConnectionState.HEADER_SENT;
        connection.headerSent = 0L;
        
        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenHeaderReceivedNotEqualSent).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToOpenPipeWhenSendOpen() {
        connectionHooks.whenOpenSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.HEADER_SENT;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.OPEN);
        open.wrap(frame.buffer(), frame.bodyOffset())
            .maxLength(255)
            .setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.OPEN_PIPE, connection.state);

        verify(connectionHooks.whenOpenSent).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderReceivedToHeaderExchangedWhenSendHeader() {
        connectionHooks.whenHeaderSent = mock(HeaderConsumer.class);

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        connection.state = ConnectionState.HEADER_RECEIVED;
        connection.headerReceived = header.buffer().getLong(header.offset());
        
        stateMachine.sent(connection, header);

        assertSame(ConnectionState.HEADER_EXCHANGED, connection.state);

        verify(connectionHooks.whenHeaderSent).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderReceivedToEndWhenSendInconsistentHeader() {
        connectionHooks.whenHeaderSentNotEqualReceived = mock(HeaderConsumer.class);

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        connection.state = ConnectionState.HEADER_RECEIVED;
        connection.headerSent = 0L;
        
        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenHeaderSentNotEqualReceived).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToOpenReceivedWhenReceiveOpen() {
        connectionHooks.whenOpenReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.OPEN);
        open.wrap(frame.buffer(), frame.bodyOffset())
            .maxLength(255)
            .setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.OPEN_RECEIVED, connection.state);

        verify(connectionHooks.whenOpenReceived).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToOpenSentWhenSendOpen() {
        connectionHooks.whenOpenSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.OPEN);
        open.wrap(frame.buffer(), frame.bodyOffset())
            .maxLength(255)
            .setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.OPEN_SENT, connection.state);

        verify(connectionHooks.whenOpenSent).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToOpenSentWhenReceiveHeader() {
        connectionHooks.whenHeaderReceived = mock(HeaderConsumer.class);

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        connection.state = ConnectionState.OPEN_PIPE;
        connection.headerSent = header.buffer().getLong(header.offset());
        
        stateMachine.received(connection, header);

        assertSame(ConnectionState.OPEN_SENT, connection.state);

        verify(connectionHooks.whenHeaderReceived).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToEndWhenReceiveInconsistentHeader() {
        connectionHooks.whenHeaderReceivedNotEqualSent = mock(HeaderConsumer.class);

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        connection.state = ConnectionState.OPEN_PIPE;
        connection.headerSent = 0L;
        
        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenHeaderReceivedNotEqualSent).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToOpenClosePipeWhenSendClose() {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);

        frame.setChannel(0x00)
        .setDataOffset(0x02)
        .setType(0x01)
        .setPerformative(Performative.CLOSE);
        close.wrap(frame.buffer(), frame.bodyOffset())
             .maxLength(255)
             .clear();
        frame.bodyChanged();

        connection.state = ConnectionState.OPEN_PIPE;
        
        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.OPEN_CLOSE_PIPE, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenClosePipeToClosePipeWhenReceiveHeader() {
        connectionHooks.whenHeaderReceived = mock(HeaderConsumer.class);

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        connection.state = ConnectionState.OPEN_CLOSE_PIPE;
        connection.headerSent = header.buffer().getLong(header.offset());
        
        stateMachine.received(connection, header);

        assertSame(ConnectionState.CLOSE_PIPE, connection.state);

        verify(connectionHooks.whenHeaderReceived).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenClosePipeToEndWhenReceiveInconsistentHeader() {
        connectionHooks.whenHeaderReceivedNotEqualSent = mock(HeaderConsumer.class);

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        connection.state = ConnectionState.OPEN_CLOSE_PIPE;
        connection.headerSent = 0L;
        
        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenHeaderReceivedNotEqualSent).accept(connection, header);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromClosePipeToCloseSentWhenReceiveOpen() {
        connectionHooks.whenOpenReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.CLOSE_PIPE;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.OPEN);
        open.wrap(frame.buffer(), frame.bodyOffset())
            .maxLength(255)
            .setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.CLOSE_SENT, connection.state);

        verify(connectionHooks.whenOpenReceived).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenSentToOpenedWhenReceiveOpen() {
        connectionHooks.whenOpenReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPEN_SENT;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.OPEN);
        open.wrap(frame.buffer(), frame.bodyOffset())
            .maxLength(255)
            .setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.OPENED, connection.state);

        verify(connectionHooks.whenOpenReceived).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenReceivedToOpenedWhenSendOpen() {
        connectionHooks.whenOpenSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPEN_RECEIVED;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.OPEN);
        open.wrap(frame.buffer(), frame.bodyOffset())
            .maxLength(255)
            .setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.OPENED, connection.state);

        verify(connectionHooks.whenOpenSent).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToCloseReceivedWhenReceiveClose() {
        connectionHooks.whenCloseReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPENED;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.CLOSE);
        close.wrap(frame.buffer(), frame.bodyOffset())
             .maxLength(255)
             .clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.CLOSE_RECEIVED, connection.state);

        verify(connectionHooks.whenCloseReceived).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToCloseSentWhenSendClose() {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPENED;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.CLOSE);
        close.wrap(frame.buffer(), frame.bodyOffset())
             .maxLength(255)
             .clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.CLOSE_SENT, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToDiscardingWhenSendCloseOnError() {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPENED;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.CLOSE);
        close.wrap(frame.buffer(), frame.bodyOffset())
             .maxLength(255)
             .clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);
        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseSentToEndWhenReceiveClose() {
        connectionHooks.whenCloseReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.CLOSE_SENT;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.CLOSE);
        close.wrap(frame.buffer(), frame.bodyOffset())
             .maxLength(255)
             .clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenCloseReceived).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseReceivedToEndWhenSendClose() {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.CLOSE_RECEIVED;

        frame.setChannel(0x00)
             .setDataOffset(0x02)
             .setType(0x01)
             .setPerformative(Performative.CLOSE);
        close.wrap(frame.buffer(), frame.bodyOffset())
             .maxLength(255)
             .clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
    }

}