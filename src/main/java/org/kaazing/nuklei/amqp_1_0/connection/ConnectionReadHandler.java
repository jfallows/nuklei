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

import static org.kaazing.nuklei.FlyweightBE.int32Get;
import static org.kaazing.nuklei.net.TcpManagerTypeId.EOF;
import static org.kaazing.nuklei.net.TcpManagerTypeId.NEW_CONNECTION;
import static org.kaazing.nuklei.net.TcpManagerTypeId.RECEIVED_DATA;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_INT;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_LONG;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.concurrent.ringbuffer.RingBufferReader.ReadHandler;
import org.kaazing.nuklei.function.AlignedReadHandler;
import org.kaazing.nuklei.function.AlignedReadHandler.DataOffsetSupplier;
import org.kaazing.nuklei.function.StatefulReadHandler;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class ConnectionReadHandler<C, S, L> implements ReadHandler {

    private final ConnectionFactory<C, S, L> connectionFactory;
    private final Map<Long, Connection<C, S, L>> statesByConnectionID;
    private final ConnectionHandler<C, S, L> connectionHandler;
    private final ReadHandler readHandler;

    
    public ConnectionReadHandler(ConnectionFactory<C, S, L> connectionFactory, DataOffsetSupplier dataOffset, ConnectionHandler<C, S, L> connectionHandler) {
        
        this.connectionFactory = connectionFactory;
        this.connectionHandler = connectionHandler;

        AlignedReadHandler<Connection<C, S, L>> alignedHandler = this::readAligned;

        StatefulReadHandler<Connection<C, S, L>> statefulHandler = 
                alignedHandler.alignedBy(dataOffset, (connection) -> (connection != null) ? connection.reassemblyBuffer : null, ConnectionReadHandler::alignLength);

        this.statesByConnectionID = new HashMap<>();
        this.readHandler = statefulHandler.statefulBy(this::connectionLifecycle);
    }

    @Override
    public void onMessage(int typeId, MutableDirectBuffer buffer, int offset, int length) {
        readHandler.onMessage(typeId, buffer, offset, length);
    }

    private static <C, S, L> int alignLength(Connection<C, S, L> connection, int typeId, MutableDirectBuffer buffer, int offset, int length)  {
        switch (typeId) {
        case RECEIVED_DATA:
            switch (connection.state) {
            case START:
            case HEADER_SENT:
                if (length >= SIZE_OF_LONG + Header.SIZEOF_HEADER + SIZE_OF_INT) {
                    return SIZE_OF_LONG + Header.SIZEOF_HEADER + int32Get(buffer, offset + SIZE_OF_LONG + 8);
                }
                return SIZE_OF_LONG + Header.SIZEOF_HEADER;
            default:
                return SIZE_OF_LONG + int32Get(buffer, offset + SIZE_OF_LONG);
            }
        default:
            return length;
        }
    }
    
    private void readAligned(Connection<C, S, L> connection, int typeId, MutableDirectBuffer buffer, int offset, int length)  {
        if (connection == null) {
            return;
        }
        
        switch (typeId) {
        case NEW_CONNECTION:
            connectionHandler.init(connection);
            break;
        case RECEIVED_DATA:
            int limit = offset + length;
            offset += BitUtil.SIZE_OF_LONG;  // TODO: abstract out
            while (offset < limit) {
                switch (connection.state) {
                case START:
                case HEADER_SENT:
                    Header header = Header.LOCAL_REF.get().wrap(buffer, offset);
                    offset = header.limit();
                    connectionHandler.handleHeader(connection, header);
                    break;
                case DISCARDING:
                    offset = limit;
                    break;
                default:
                    Frame frame = Frame.LOCAL_REF.get().wrap(buffer, offset);
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
    
    private Connection<C, S, L> connectionLifecycle(int typeId, MutableDirectBuffer buffer, int offset, int length)  {
        switch (typeId) {
        case NEW_CONNECTION:
            long newConnectionID = buffer.getLong(offset);
            MutableDirectBuffer reassemblyBuffer = new UnsafeBuffer(ByteBuffer.allocate(8192));
            Connection<C, S, L> newConnection = connectionFactory.newConnection(newConnectionID, reassemblyBuffer);
            statesByConnectionID.put(newConnectionID, newConnection);
            return newConnection;
        case RECEIVED_DATA:
            long connectionID = buffer.getLong(offset);
            return statesByConnectionID.get(connectionID);
        case EOF:
            long oldConnectionID = buffer.getLong(offset);
            return statesByConnectionID.remove(oldConnectionID);
        default:
            return null;
        }
    }
}