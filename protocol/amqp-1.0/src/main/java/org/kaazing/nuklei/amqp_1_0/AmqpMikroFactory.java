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
package org.kaazing.nuklei.amqp_1_0;

import static org.kaazing.nuklei.FlyweightBE.int32Get;
import static org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId.EOF;
import static org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId.NEW_CONNECTION;
import static org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId.RECEIVED_DATA;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_INT;

import java.nio.ByteBuffer;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.connection.Connection;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionFactory;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionHandler;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;
import org.kaazing.nuklei.amqp_1_0.sender.SenderFactory;
import org.kaazing.nuklei.function.AlignedMikro.StorageSupplier;
import org.kaazing.nuklei.function.Mikro;
import org.kaazing.nuklei.function.StatefulMikro;
import org.kaazing.nuklei.protocol.tcp.TcpManagerHeadersDecoder;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class AmqpMikroFactory<C, S, L>
{

    public Mikro newMikro(SenderFactory senderFactory,
                          ConnectionFactory<C, S, L> connectionFactory,
                          ConnectionHandler<C, S, L> connectionHandler)
    {

        AmqpMikro<C, S, L> mikro = new AmqpMikro<>(connectionHandler);

        StorageSupplier<Connection<C, S, L>> storage = (connection) -> (connection != null) ?
                connection.reassemblyStorage
                : null;

        StatefulMikro<Connection<C, S, L>> stateful = mikro.alignedBy(storage, AmqpMikroFactory::alignLength);

        AmqpConnectionState connectionState = new AmqpConnectionState(connectionFactory, senderFactory);

        return stateful.statefulBy(connectionState::lifecycle);
    }

    private static <C, S, L> int alignLength(Connection<C, S, L> connection,
                                             Object header,
                                             int typeId,
                                             DirectBuffer buffer,
                                             int offset,
                                             int length)
    {
        switch (typeId)
        {
        case RECEIVED_DATA:
            switch (connection.state)
            {
            case START:
            case HEADER_SENT:
                if (length >= Header.SIZEOF_HEADER + SIZE_OF_INT)
                {
                    return Header.SIZEOF_HEADER + int32Get(buffer, offset + Header.SIZEOF_HEADER);
                }
                return Header.SIZEOF_HEADER;
            default:
                return int32Get(buffer, offset);
            }
        default:
            return length;
        }
    }

    private final class AmqpConnectionState
    {
        private final ConnectionFactory<C, S, L> connectionFactory;
        private final SenderFactory senderFactory;
        private final Long2ObjectHashMap<Connection<C, S, L>> statesByConnectionID;

        public AmqpConnectionState(ConnectionFactory<C, S, L> connectionFactory, SenderFactory senderFactory)
        {
            this.connectionFactory = connectionFactory;
            this.senderFactory = senderFactory;
            this.statesByConnectionID = new Long2ObjectHashMap<>();
        }

        private Connection<C, S, L> lifecycle(Object headers, int typeId)
        {
            TcpManagerHeadersDecoder tcpHeaders = (TcpManagerHeadersDecoder) headers;
            switch (typeId)
            {
            case NEW_CONNECTION:
                long newConnectionID = tcpHeaders.connectionId();
                MutableDirectBuffer reassemblyBuffer = new UnsafeBuffer(ByteBuffer.allocate(8192));
                Sender newSender = senderFactory.newSender(headers);
                Connection<C, S, L> newConnection = connectionFactory.newConnection(newSender, reassemblyBuffer);
                statesByConnectionID.put(newConnectionID, newConnection);
                return newConnection;
            case RECEIVED_DATA:
                long connectionID = tcpHeaders.connectionId();
                return statesByConnectionID.get(connectionID);
            case EOF:
                long oldConnectionID = tcpHeaders.connectionId();
                return statesByConnectionID.remove(oldConnectionID);
            default:
                return null;
            }
        }
    }
}