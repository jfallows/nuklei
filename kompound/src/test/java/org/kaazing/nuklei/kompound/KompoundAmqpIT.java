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
package org.kaazing.nuklei.kompound;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.TestCase.assertTrue;
import static org.junit.rules.RuleChain.outerRule;
import static org.kaazing.nuklei.amqp_1_0.codec.definitions.Role.RECEIVER;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.ATTACH;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.BEGIN;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.CLOSE;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.DETACH;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.DISPOSITION;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.END;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.OPEN;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.TRANSFER;
import static org.kaazing.nuklei.amqp_1_0.codec.transport.Header.AMQP_PROTOCOL;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.nuklei.amqp_1_0.AmqpMikroFactory;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.ReceiverSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Role;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.SenderSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.Message;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.TerminusDurability;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.TerminusExpiryPolicy;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Attach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Begin;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Close;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Detach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Disposition;
import org.kaazing.nuklei.amqp_1_0.codec.transport.End;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Open;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Transfer;
import org.kaazing.nuklei.amqp_1_0.connection.Connection;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionFactory;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionHandler;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionHooks;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionStateMachine;
import org.kaazing.nuklei.amqp_1_0.link.Link;
import org.kaazing.nuklei.amqp_1_0.link.LinkFactory;
import org.kaazing.nuklei.amqp_1_0.link.LinkHandler;
import org.kaazing.nuklei.amqp_1_0.link.LinkHooks;
import org.kaazing.nuklei.amqp_1_0.link.LinkStateMachine;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;
import org.kaazing.nuklei.amqp_1_0.sender.SenderFactory;
import org.kaazing.nuklei.amqp_1_0.sender.TcpSenderFactory;
import org.kaazing.nuklei.amqp_1_0.session.Session;
import org.kaazing.nuklei.amqp_1_0.session.SessionFactory;
import org.kaazing.nuklei.amqp_1_0.session.SessionHandler;
import org.kaazing.nuklei.amqp_1_0.session.SessionHooks;
import org.kaazing.nuklei.amqp_1_0.session.SessionStateMachine;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.Mikro;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;
import org.kaazing.nuklei.kompound.cmd.StartCmd;
import org.kaazing.nuklei.kompound.cmd.StopCmd;
import org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class KompoundAmqpIT
{
    public static final String URI = "tcp://localhost:5672";
    private static final int SEND_BUFFER_SIZE = 1024;

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/k3po/scripts/nuklei/kompound");

    private final TestRule timeout = new DisableOnDebug(new Timeout(1, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    final AtomicBoolean attached = new AtomicBoolean(false);

    private static Link<AmqpTestLink> receiverLink = null;

    private Kompound kompound;

    @After
    public void cleanUp() throws Exception
    {
        if (null != kompound)
        {
            kompound.close();
        }
    }

    @Test
    public void shouldStartUpAndShutdownCorrectly() throws Exception
    {
        final AtomicBoolean started = new AtomicBoolean(false);
        final AtomicBoolean stopped = new AtomicBoolean(false);

        final Kompound.Builder builder = new Kompound.Builder()
            .service(
                URI,
                (header, typeId, buffer, offset, length) ->
                {
                    if (header instanceof StartCmd)
                    {
                        started.lazySet(true);
                    }
                    else if (header instanceof StopCmd)
                    {
                        stopped.lazySet(true);
                    }
                    else if (TcpManagerTypeId.ATTACH_COMPLETED == typeId)
                    {
                        attached.lazySet(true);
                    }
                });

        kompound = Kompound.startUp(builder);
        waitToBeAttached();

        kompound.close();
        kompound = null;

        assertTrue(started.get());
        assertTrue(stopped.get());
    }

    @Test
    @Specification("amqp/connection/connect.and.close")
    public void shouldConnectAndCloseAMQP() throws Exception
    {
        final SenderFactory senderFactory = new TcpSenderFactory(new UnsafeBuffer(ByteBuffer.allocate(SEND_BUFFER_SIZE)));
        final ConnectionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connectionFactory =
                new AmqpTestConnectionFactory();
        final ConnectionHandler<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connectionHandler =
                new ConnectionHandler<AmqpTestConnection, AmqpTestSession, AmqpTestLink>(
                        new AmqpTestSessionFactory(),
                        new SessionHandler<AmqpTestSession, AmqpTestLink>(
                                new AmqpTestLinkFactory(),
                                new LinkHandler<AmqpTestLink>()));

        AmqpMikroFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink> factory =
                new AmqpMikroFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink>();
        Mikro mikro = factory.newMikro(senderFactory, connectionFactory, connectionHandler);

        final Kompound.Builder builder = new Kompound.Builder()
            .service(
                URI,
                (header, typeId, buffer, offset, length) ->
                {
                    switch (typeId)
                    {
                    case TcpManagerTypeId.ATTACH_COMPLETED:
                        attached.lazySet(true);
                        break;
                    case TcpManagerTypeId.NEW_CONNECTION:
                    case TcpManagerTypeId.RECEIVED_DATA:
                    case TcpManagerTypeId.EOF:
                        mikro.onMessage(header, typeId, buffer, offset, length);
                        break;
                    }
                });

        kompound = Kompound.startUp(builder);
        waitToBeAttached();

        // FIXME:  Upgrade to latest K3PO, use k3po.finish()
        k3po.join();
    }

    @Test
    @Specification({ "amqp/queue/create.queue.producer",
                     "amqp/queue/create.queue.consumer"})
    public void shouldTransferMessageFromProducerToConsumer() throws Exception
    {
        final SenderFactory senderFactory = new TcpSenderFactory(new UnsafeBuffer(ByteBuffer.allocate(SEND_BUFFER_SIZE)));
        final ConnectionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connectionFactory =
                new AmqpTestConnectionFactory();
        final ConnectionHandler<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connectionHandler =
                new ConnectionHandler<AmqpTestConnection, AmqpTestSession, AmqpTestLink>(
                        new AmqpTestSessionFactory(),
                        new SessionHandler<AmqpTestSession, AmqpTestLink>(
                                new AmqpTestLinkFactory(),
                                new LinkHandler<AmqpTestLink>()));

        AmqpMikroFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink> factory =
                new AmqpMikroFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink>();
        Mikro mikro = factory.newMikro(senderFactory, connectionFactory, connectionHandler);

        final Kompound.Builder builder = new Kompound.Builder()
            .service(
                URI,
                (header, typeId, buffer, offset, length) ->
                {
                    switch (typeId)
                    {
                    case TcpManagerTypeId.ATTACH_COMPLETED:
                        attached.lazySet(true);
                        break;
                    case TcpManagerTypeId.NEW_CONNECTION:
                    case TcpManagerTypeId.RECEIVED_DATA:
                    case TcpManagerTypeId.EOF:
                        mikro.onMessage(header, typeId, buffer, offset, length);
                        break;
                    }
                });

        kompound = Kompound.startUp(builder);
        waitToBeAttached();

        // FIXME:  Upgrade to latest K3PO, use k3po.finish()
        k3po.join();
    }

    private void waitToBeAttached() throws Exception
    {
        while (!attached.get())
        {
            Thread.sleep(10);
        }
    }

    private class AmqpTestConnection extends Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink>
    {
        // TODO: the JmsConnection stored a javax.jms.Connection object representing the connection to the
        //       broker, is something similar needed here for the connection to the Aeron bus?
//        private TcpConnection connection;
//        private AmqpTcpConnectionFactory connectionFactory;

        public AmqpTestConnection(ConnectionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connectionFactory,
                                  ConnectionStateMachine<AmqpTestConnection, AmqpTestSession, AmqpTestLink> stateMachine,
                                  Sender sender, MutableDirectBuffer reassemblyBuffer)
        {
            super(stateMachine, sender, reassemblyBuffer);
            this.parameter = this;
        }
    }

    private class AmqpTestSession extends Session<AmqpTestSession, AmqpTestLink>
    {
        AmqpTestSession(Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> owner,
                SessionStateMachine<AmqpTestSession, AmqpTestLink> stateMachine)
        {
            super(stateMachine, owner.sender);
            this.parameter = this;
        }
    }

    private class AmqpTestLink extends Link<AmqpTestLink>
    {
        public AmqpTestLink(Session<AmqpTestSession, AmqpTestLink> owner,
                            LinkStateMachine<AmqpTestLink> stateMachine)
        {
            super(stateMachine, owner.sender);
            this.parameter = this;
        }
    }

    private class AmqpTestConnectionFactory implements ConnectionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink>
    {
        @Override
        public Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> newConnection(Sender sender,
                                                                                           MutableDirectBuffer reassemblyBuffer)
        {
            ConnectionStateMachine<AmqpTestConnection, AmqpTestSession, AmqpTestLink> stateMachine =
                    new ConnectionStateMachine<AmqpTestConnection, AmqpTestSession, AmqpTestLink>(
                            new AmqpTestConnectionHooks());
            return new AmqpTestConnection(this, stateMachine, sender, reassemblyBuffer);
        }
    }

    private class AmqpTestSessionFactory implements SessionFactory<AmqpTestConnection, AmqpTestSession, AmqpTestLink>
    {
        @Override
        public Session<AmqpTestSession, AmqpTestLink> newSession(
                Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connection)
        {
            return new AmqpTestSession(connection,
                    new SessionStateMachine<AmqpTestSession, AmqpTestLink>(
                            new AmqpTestSessionHooks()));
        }
    }

    private class AmqpTestLinkFactory implements LinkFactory<AmqpTestSession, AmqpTestLink>
    {
        @Override
        public Link<AmqpTestLink> newLink(Session<AmqpTestSession, AmqpTestLink> session)
        {
            return new AmqpTestLink(session, new LinkStateMachine<AmqpTestLink>(
                    new AmqpTestLinkHooks()));
        }
    }

    private static class AmqpTestConnectionHooks extends ConnectionHooks<AmqpTestConnection, AmqpTestSession, AmqpTestLink>
    {
        public AmqpTestConnectionHooks()
        {
            whenHeaderReceived = AmqpTestConnectionHooks::whenHeaderReceived;
            whenOpenReceived = AmqpTestConnectionHooks::whenOpenReceived;
            whenCloseReceived = AmqpTestConnectionHooks::whenCloseReceived;
        }

        private static void whenHeaderReceived(Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connection,
                                        Header header)
        {
            Sender sender = connection.sender;
            header.wrap(sender.getBuffer(), sender.getOffset(), true)
                  .setProtocol(AMQP_PROTOCOL)
                  .setProtocolID(0x00)
                  .setMajorVersion(0x01)
                  .setMinorVersion(0x00)
                  .setRevisionVersion(0x00);

            connection.send(header);
        }

        private static void whenOpenReceived(Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connection,
                                      Frame frame,
                                      Open open)
        {
            // TODO: Create a connection to the Aeron bus?
            //       broker, is something similar needed here for the connection to the Aeron bus?
//            AmqpTestConnection parameter = connection.parameter;
//            parameter.connection = parameter.connectionFactory.createConnection();

            Sender sender = connection.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                .setDataOffset(2)
                .setType(0)
                .setChannel(0)
                .setPerformative(OPEN);
            open.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                .maxLength(255)
                .setContainerId(WRITE_UTF_8, "")
                .setHostname(WRITE_UTF_8, "")
                .setMaxFrameSize(1048576L);
            frame.bodyChanged();
            connection.send(frame, open);
        }

        private static void whenCloseReceived(Connection<AmqpTestConnection, AmqpTestSession, AmqpTestLink> connection,
                                              Frame frame,
                                              Close close)
        {
            // TODO:  close connection to Aeron bus?
            // AmqpTestConnection parameter = connection.parameter;
            // parameter.connection.close();

            Sender sender = connection.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                  .setDataOffset(2)
                  .setType(0)
                  .setChannel(0)
                  .setPerformative(CLOSE);
            close.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                 .maxLength(255)
                 .clear();
            frame.bodyChanged();
            connection.send(frame, close);
        }
    }

    private static class AmqpTestSessionHooks extends SessionHooks<AmqpTestSession, AmqpTestLink>
    {
        public AmqpTestSessionHooks()
        {
            this.whenBeginReceived = AmqpTestSessionHooks::whenBeginReceived;
            this.whenEndReceived = AmqpTestSessionHooks::whenEndReceived;
        }

        private static void whenBeginReceived(Session<AmqpTestSession, AmqpTestLink> session, Frame frame, Begin begin)
        {
            Sender sender = session.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                  .setDataOffset(2)
                  .setType(0)
                  .setChannel(0)
                  .setPerformative(BEGIN);
            begin.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                 .maxLength(255)
                 .setRemoteChannel(0)
                 .setNextOutgoingId(1)
                 .setIncomingWindow(0)
                 .setOutgoingWindow(0)
                 .setHandleMax(1024);
            frame.bodyChanged();
            session.send(frame, begin);
        }

        private static void whenEndReceived(Session<AmqpTestSession, AmqpTestLink> session, Frame frame, End end)
        {
            Sender sender = session.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                 .setDataOffset(2)
                 .setType(0)
                 .setChannel(0)
                 .setPerformative(END);
            end.wrap(sender.getBuffer(), frame.bodyOffset(), true)
               .clear();
            frame.bodyChanged();
            session.send(frame, end);
        }
    }

    private static final class AmqpTestLinkHooks extends LinkHooks<AmqpTestLink>
    {
        private static Frame sendFrame;
        private static Transfer sendTransfer;

        private AmqpTestLinkHooks()
        {
            whenAttachReceived = AmqpTestLinkHooks::whenAttachReceived;
            whenTransferReceived = AmqpTestLinkHooks::whenTransferReceived;
            whenDetachReceived = AmqpTestLinkHooks::whenDetachReceived;

            sendFrame = new Frame();
            sendTransfer = new Transfer();
        }

        private static void whenAttachReceived(Link<AmqpTestLink> link, Frame frame, Attach attach)
        {
            Role role = Role.RECEIVER;
            if (attach.getRole() == Role.RECEIVER)
            {
                role = Role.SENDER;
                receiverLink = link;
            }

            Sender sender = link.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                  .setDataOffset(2)
                  .setType(0)
                  .setChannel(0)
                  .setPerformative(ATTACH);
            attach.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                  .maxLength(255)
                  .setName(WRITE_UTF_8, "queue://queue-A-> (fcf0809c-6937-11e4-b116-123b93f75cba)")
                  .setHandle(0)
                  .setRole(role)
                  .setSendSettleMode(SenderSettleMode.MIXED)
                  .setReceiveSettleMode(ReceiverSettleMode.FIRST);
            attach.getSource()
                  .setDescriptor()
                  .maxLength(255)
                  .setAddress(WRITE_UTF_8, "queue://queue-A")
                  .setDurable(TerminusDurability.NONE)
                  .setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
            attach.getTarget()
                  .setDescriptor()
                  .maxLength(255)
                  .setAddress(WRITE_UTF_8, "29d3bfd4-6938-11e4-b116-123b93f75cba");
            frame.bodyChanged();
            link.send(frame, attach);
        }

        private static void whenDetachReceived(Link<AmqpTestLink> link, Frame frame, Detach detach)
        {
            Sender sender = link.sender;
            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                  .setDataOffset(2)
                  .setType(0)
                  .setChannel(0)
                  .setPerformative(DETACH);
            detach.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                  .maxLength(255)
                  .setHandle(0)
                  .setClosed(true);
            frame.bodyChanged();
            link.send(frame, detach);
        }

        private static void whenTransferReceived(Link<AmqpTestLink> link, Frame frame, Transfer transfer)
        {
            // TODO: This should settle only when required based on the incoming settled state
            Sender sender = link.sender;

            frame.wrap(sender.getBuffer(), sender.getOffset(), true)
                 .setDataOffset(2)
                 .setType(0)
                 .setChannel(0)
                 .setPerformative(DISPOSITION);
            Disposition disposition = Disposition.LOCAL_REF.get();
            disposition.wrap(sender.getBuffer(), frame.bodyOffset(), true)
                       .setRole(RECEIVER)
                       .setFirst(0)
                       .setLast(0)
                       .setSettled(true);
            frame.bodyChanged();
            link.send(frame, disposition);

            // send transfer to other attached session
            Message message = transfer.getMessage();

            // get the string out
            String messageString = message.getValue(READ_UTF_8);

            // find the target session, get sendBuffer for said session, new Transfer frame to that session with
            // just decoded Message body
            sendFrame.wrap(sender.getBuffer(), sender.getOffset(), true)
                     .setDataOffset(2)
                     .setType(0)
                     .setChannel(0)
                     .setPerformative(TRANSFER);
            sendTransfer.wrap(sender.getBuffer(), sendFrame.bodyOffset(), true)
                        .setHandle(0)
                        .setDeliveryId(0)
                        .setDeliveryTag(WRITE_UTF_8, "\0")
                        .setMessageFormat(0)
                        .setSettled(true);
            sendTransfer.getMessage()
                        .setDescriptor(0x77L)
                        .setValue(WRITE_UTF_8, messageString);
            sendFrame.setLength(sendTransfer.limit() - sendFrame.offset());
            receiverLink.send(sendFrame, sendTransfer);
        }
    }

    private static final MutableDirectBufferMutator<String> WRITE_UTF_8 = newMutator(UTF_8);

    public static final MutableDirectBufferMutator<String> newMutator(final Charset charset)
    {
        return new MutableDirectBufferMutator<String>()
        {
            private final CharsetEncoder encoder = charset.newEncoder();
            private final int maxBytesPerChar = (int) encoder.maxBytesPerChar();

            @Override
            public int mutate(Mutation mutation, MutableDirectBuffer buffer, String value)
            {
                int offset = mutation.maxOffset(value.length() * maxBytesPerChar);
                ByteBuffer buf = buffer.byteBuffer();
                ByteBuffer out = buf != null ? buf.duplicate() : ByteBuffer.wrap(buffer.byteArray());
                out.position(offset);
                encoder.reset();
                encoder.encode(CharBuffer.wrap(value), out, true);
                encoder.flush(out);
                return out.position() - offset;
            }

        };
    }

    private static final DirectBufferAccessor<String> READ_UTF_8 = newAccessor(UTF_8);

    public static final DirectBufferAccessor<String> newAccessor(final Charset charset)
    {
        return new DirectBufferAccessor<String>()
        {
            private final CharsetDecoder decoder = charset.newDecoder();

            @Override
            public String access(DirectBuffer buffer, int offset, int size)
            {
                ByteBuffer bb = ByteBuffer.allocate(size);
                buffer.getBytes(offset, bb, size);
                bb.flip();
                try
                {
                    CharBuffer charBuffer = decoder.decode(bb);
                    return charBuffer.toString();
                }
                catch (CharacterCodingException ex)
                {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
