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
package org.kaazing.nuklei.amqp_1_0.tidy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.kaazing.nuklei.BitUtil.SIZE_OF_LONG;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.CLOSE;
import static org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative.OPEN;
import static org.kaazing.nuklei.amqp_1_0.connection.ConnectionState.CLOSE_SENT;
import static org.kaazing.nuklei.amqp_1_0.connection.ConnectionState.END;
import static org.kaazing.nuklei.amqp_1_0.connection.ConnectionState.OPENED;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Close;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Open;
import org.kaazing.nuklei.amqp_1_0.connection.Connection;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.function.AtomicBufferMutator;

public final class ConnectionStateProcessor {
    
    public static void sendHeader(final Connection connection, final Header header) {
        connection.sender.send(header.limit());
        connection.stateMachine.sent(connection, header);
    }

    private static int receiveOpen(Connection connection, AtomicBuffer buffer, int offset, int limit) {
        final Open open = Open.LOCAL_REF.get();
        open.wrap(buffer, offset);
        if (limit < open.limit()) {
            connection.state = END;
            return offset;
        }
        limit = open.limit();
        // TODO: validate OPEN (channel == 0, etc)
        
        switch (connection.state) {
        case HEADER_EXCHANGED:
            sendOpen(connection);
            connection.state = OPENED;
            break;
        case OPEN_SENT:
            connection.state = OPENED;
            break;
        case CLOSE_PIPE:
            sendOpen(connection);
            connection.state = CLOSE_SENT;
            break;
        default:
            connection.state = END;
            return offset;
        }
        
        return limit;
    }

    private static int receiveClose(Connection connection, AtomicBuffer buffer, int offset, int limit) {
        final Close close = Close.LOCAL_REF.get();
        close.wrap(buffer, offset);
        if (close.limit() > limit) {
            throw new IllegalArgumentException("fragmentation not yet implemented");
        }
        // TODO: validate CLOSE (channel == 0, etc)
        limit = close.limit();

        switch (connection.state) {
        case OPENED:
            sendClose(connection);
            connection.state = END;
            break;
        case CLOSE_SENT:
            connection.state = END;
            break;
        default:
            connection.state = END;
            return offset;
        }
        
        return limit;
    }

    private static void sendOpen(Connection connection) {
        Sender sender = connection.sender;
        final Frame frame = Frame.LOCAL_REF.get();
        final Open open = Open.LOCAL_REF.get();

        // @formatter:off
        frame.wrap(sender.sendBuffer, sender.sendBufferOffset)
             .setDataOffset(2)
             .setType(0)
             .setChannel(0)
             .setPerformative(OPEN);
        open.wrap(sender.sendBuffer, frame.limit())
            .maxLength(255)
            .setContainerId(WRITE_UTF_8, "")
            .setHostname(WRITE_UTF_8, "")
            .setMaxFrameSize(1048576L);
        frame.setLength(open.limit() - frame.offset());
        // @formatter:on
        sender.send(open.limit());
    }

    private static void sendClose(Connection connection) {
        Sender sender = connection.sender;
        final Frame frame = Frame.LOCAL_REF.get();
        final Close close = Close.LOCAL_REF.get();

        // @formatter:off
        frame.wrap(sender.sendBuffer, SIZE_OF_LONG)
             .setDataOffset(2)
             .setType(0)
             .setChannel(0)
             .setPerformative(CLOSE);
        close.wrap(sender.sendBuffer, frame.limit())
             .maxLength(255)
             .clear();
        frame.setLength(close.limit() - frame.offset());
        // @formatter:on
        sender.send(close.limit());
    }

    private static int whenEnd(Connection connection, AtomicBuffer buffer, int offset, int limit) {
        connection.sender.close(false);
        return offset;
    }

    private static boolean endWhenHeaderInvalid(Connection connection, Header header) {
        if (header.getProtocol() != Header.AMQP_PROTOCOL ||
            header.getProtocolID() != 0x00 || // TODO: SASL 0x03
            header.getMajorVersion() != 0x01 ||
            header.getMinorVersion() != 0x00 ||
            header.getRevisionVersion() != 0x00) {
            connection.state = END;
            return true;
        }
        
        return false;
    }
    
    private static final AtomicBufferMutator<String> WRITE_UTF_8 = newMutator(UTF_8);

    public static final AtomicBufferMutator<String> newMutator(final Charset charset) {
        return new AtomicBufferMutator<String>() {
            private final CharsetEncoder encoder = charset.newEncoder();
            private final int maxBytesPerChar = (int) encoder.maxBytesPerChar();
    
            @Override
            public int mutate(Mutation mutation, AtomicBuffer buffer, String value) {
                int offset = mutation.maxOffset(value.length() * maxBytesPerChar);
                ByteBuffer buf = buffer.byteBuffer();
                ByteBuffer out = buf != null ? buf.duplicate() : ByteBuffer.wrap(buffer.array());
                out.position(offset);
                encoder.reset();
                encoder.encode(CharBuffer.wrap(value), out, true);
                encoder.flush(out);
                return out.position() - offset;
            }

        };
    }
}
