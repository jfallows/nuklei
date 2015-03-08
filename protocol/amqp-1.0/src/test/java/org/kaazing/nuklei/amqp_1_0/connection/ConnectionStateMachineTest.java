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
import static org.mockito.Mockito.never;
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

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class ConnectionStateMachineTest
{

    private final ConnectionHooks<Void, Void, Void> connectionHooks = new ConnectionHooks<>();
    private final ConnectionStateMachine<Void, Void, Void> stateMachine = new ConnectionStateMachine<>(connectionHooks);
    private final Connection<Void, Void, Void> connection = new Connection<>(stateMachine, mock(Sender.class),
            new UnsafeBuffer(new byte[0]));
    private final Header header =
            Header.LOCAL_REF.get().wrap(new UnsafeBuffer(new byte[Header.SIZEOF_HEADER]), 0, true);
    private final Frame frame = Frame.LOCAL_REF.get().wrap(new UnsafeBuffer(new byte[64]), 0, true);
    private final Open open = Open.LOCAL_REF.get();
    private final Close close = Close.LOCAL_REF.get();

    @Test
    @SuppressWarnings("unchecked")
    public void shouldInitializeInStart()
    {
        connectionHooks.whenInitialized = mock(Consumer.class);

        stateMachine.start(connection);

        assertSame(ConnectionState.START, connection.state);

        verify(connectionHooks.whenInitialized).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromStartToHeaderReceivedWhenReceiveHeader()
    {
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
    public void shouldTransitionFromStartToHeaderSentWhenSendHeader()
    {
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
    public void shouldTransitionFromStartToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.START;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromStartToEndReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.START;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromStartToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.START;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromStartToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.START;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromStartToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.START;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_SENT;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToHeaderExchangedWhenReceiveHeader()
    {
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
    public void shouldTransitionFromHeaderSentToEndWhenReceiveInconsistentHeader()
    {
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
    public void shouldTransitionFromHeaderSentToOpenPipeWhenSendOpen()
    {
        connectionHooks.whenOpenSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.HEADER_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.OPEN_PIPE, connection.state);

        verify(connectionHooks.whenOpenSent).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToEndWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderSentToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_SENT;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderReceivedToHeaderExchangedWhenSendHeader()
    {
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
    public void shouldTransitionFromHeaderReceivedToEndWhenSendInconsistentHeader()
    {
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
    public void shouldTransitionFromHeaderReceivedToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_RECEIVED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderReceivedToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderReceivedToEndWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderReceivedToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderReceivedToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderReceivedToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_RECEIVED;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToOpenSentWhenSendOpen()
    {
        connectionHooks.whenOpenSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.OPEN_SENT, connection.state);

        verify(connectionHooks.whenOpenSent).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToOpenReceivedWhenReceiveOpen()
    {
        connectionHooks.whenOpenReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.OPEN_RECEIVED, connection.state);

        verify(connectionHooks.whenOpenReceived).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromHeaderExchangedToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.HEADER_EXCHANGED;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_PIPE;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToOpenSentWhenReceiveHeader()
    {
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
    public void shouldTransitionFromOpenPipeToEndWhenReceiveInconsistentHeader()
    {
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
    public void shouldTransitionFromOpenPipeToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToEndWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToOpenClosePipeWhenSendClose()
    {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        connection.state = ConnectionState.OPEN_PIPE;

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.OPEN_CLOSE_PIPE, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenPipeToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_PIPE;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenClosePipeToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_CLOSE_PIPE;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenClosePipeToClosePipeWhenReceiveHeader()
    {
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
    public void shouldTransitionFromOpenClosePipeToEndWhenReceiveInconsistentHeader()
    {
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
    public void shouldTransitionFromOpenClosePipeToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_CLOSE_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenClosePipeToEndWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_CLOSE_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenClosePipeToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_CLOSE_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenClosePipeToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_CLOSE_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenClosePipeToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_CLOSE_PIPE;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromClosePipeToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_PIPE;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromClosePipeToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_PIPE;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromClosePipeToCloseSentWhenReceiveOpen()
    {
        connectionHooks.whenOpenReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.CLOSE_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.CLOSE_SENT, connection.state);

        verify(connectionHooks.whenOpenReceived).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromClosePipeToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromClosePipeToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromClosePipeToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_PIPE;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromClosePipeToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_PIPE;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenSentToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_SENT;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenSentToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_SENT;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenSentToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenSentToOpenedWhenReceiveOpen()
    {
        connectionHooks.whenOpenReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPEN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.OPENED, connection.state);

        verify(connectionHooks.whenOpenReceived).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenSentToClosePipeWhenSendClose()
    {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPEN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.CLOSE_PIPE, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenSentToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenSentToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_SENT;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenReceivedToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_RECEIVED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenReceivedToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_RECEIVED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenReceivedToOpenedWhenSendOpen()
    {
        connectionHooks.whenOpenSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPEN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.OPENED, connection.state);

        verify(connectionHooks.whenOpenSent).accept(connection, frame, open);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenReceivedToEndWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenReceivedToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenReceivedToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenReceivedToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPEN_RECEIVED;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPENED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPENED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPENED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToEndWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPENED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToCloseSentWhenSendClose()
    {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPENED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.CLOSE_SENT, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToDiscardingWhenSendCloseOnError()
    {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPENED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);
        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToCloseReceivedWhenReceiveClose()
    {
        connectionHooks.whenCloseReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.OPENED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.CLOSE_RECEIVED, connection.state);

        verify(connectionHooks.whenCloseReceived).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromOpenedToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.OPENED;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseSentToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_SENT;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseSentToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_SENT;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseSentToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseSentToEndWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseSentToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseSentToEndWhenReceiveClose()
    {
        connectionHooks.whenCloseReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.CLOSE_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenCloseReceived).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseSentToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_SENT;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.DISCARDING;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.DISCARDING;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToDiscardingWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError, never()).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToEndWhenSendClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToEndWhenReceiveClose()
    {
        connectionHooks.whenCloseReceived = mock(FrameConsumer.class);
        connection.state = ConnectionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenCloseReceived).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.DISCARDING;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError, never()).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseReceivedToEndWhenSendHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_RECEIVED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.sent(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseReceivedToEndWhenReceiveHeader()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_RECEIVED;

        header.setProtocol(AMQP_PROTOCOL);
        header.setProtocolID(0x00);
        header.setMajorVersion(0x01);
        header.setMinorVersion(0x00);
        header.setRevisionVersion(0x00);

        stateMachine.received(connection, header);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseReceivedToEndWhenSendOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.sent(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseReceivedToEndWhenReceiveOpen()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.OPEN);
        open.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setContainerId(null);
        frame.bodyChanged();

        stateMachine.received(connection, frame, open);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseReceivedToEndWhenSendClose()
    {
        connectionHooks.whenCloseSent = mock(FrameConsumer.class);
        connection.state = ConnectionState.CLOSE_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenCloseSent).accept(connection, frame, close);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseReceivedToEndWhenReceiveClose()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.CLOSE);
        close.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(connection, frame, close);

        assertSame(ConnectionState.END, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromCloseReceivedToDiscardingWhenError()
    {
        connectionHooks.whenError = mock(Consumer.class);
        connection.state = ConnectionState.CLOSE_RECEIVED;

        stateMachine.error(connection);

        assertSame(ConnectionState.DISCARDING, connection.state);

        verify(connectionHooks.whenError).accept(connection);
    }

}