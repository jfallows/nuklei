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

import static org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId.EOF;
import static org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId.NEW_CONNECTION;
import static org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId.RECEIVED_DATA;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.connection.Connection;
import org.kaazing.nuklei.amqp_1_0.connection.ConnectionHandler;
import org.kaazing.nuklei.function.AlignedMikro;

import uk.co.real_logic.agrona.DirectBuffer;

public class AmqpMikro<C, S, L> implements AlignedMikro<Connection<C, S, L>>
{

    private final ConnectionHandler<C, S, L> connectionHandler;

    protected AmqpMikro(ConnectionHandler<C, S, L> connectionHandler)
    {
        this.connectionHandler = connectionHandler;
    }

    @Override
    public void onMessage(Connection<C, S, L> connection,
                          Object headers,
                          int typeId,
                          DirectBuffer buffer,
                          int offset,
                          int length)
    {
        if (connection == null)
        {
            return;
        }

        switch (typeId)
        {
        case NEW_CONNECTION:
            connectionHandler.init(connection);
            break;
        case RECEIVED_DATA:
            int limit = offset + length;
            while (offset < limit)
            {
                switch (connection.state)
                {
                case START:
                case HEADER_SENT:
                    Header header = Header.LOCAL_REF.get().wrap(buffer, offset, false);
                    offset = header.limit();
                    connectionHandler.handleHeader(connection, header);
                    break;
                case DISCARDING:
                    offset = limit;
                    break;
                default:
                    Frame frame = Frame.LOCAL_REF.get().wrap(buffer, offset, true);
                    offset = frame.limit();
                    connectionHandler.handleFrame(connection, frame);
                    break;
                }
            }
            break;
        case EOF:
            connectionHandler.destroy(connection);
            break;
        }
    }
}