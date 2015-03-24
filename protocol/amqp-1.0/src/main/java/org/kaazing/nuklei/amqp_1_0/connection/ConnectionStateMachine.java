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

import static java.util.EnumSet.allOf;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Close;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Open;

/*
 * See AMQP 1.0 specification, section 2.4.7 "Connection State Diagram"
 */
public class ConnectionStateMachine<C, S, L>
{

    private final ConnectionHooks<C, S, L> connectionHooks;

    public ConnectionStateMachine(ConnectionHooks<C, S, L> connectionHooks)
    {
        this.connectionHooks = connectionHooks;
    }

    public void start(Connection<C, S, L> connection)
    {
        connection.state = ConnectionState.START;
        connectionHooks.whenInitialized.accept(connection);
    }

    public void received(Connection<C, S, L> connection, Header header)
    {
        connection.headerReceived = header.buffer().getLong(header.offset());

        switch (connection.state)
        {
        case START:
            transition(connection, ConnectionTransition.RECEIVED_HEADER);
            connectionHooks.whenHeaderReceived.accept(connection, header);
            break;
        case HEADER_SENT:
        case OPEN_PIPE:
        case OPEN_CLOSE_PIPE:
            if (connection.headerReceived == connection.headerSent)
            {
                transition(connection, ConnectionTransition.RECEIVED_HEADER);
                connectionHooks.whenHeaderReceived.accept(connection, header);
            }
            else
            {
                transition(connection, ConnectionTransition.RECEIVED_HEADER_NOT_EQUAL_SENT);
                connectionHooks.whenHeaderReceivedNotEqualSent.accept(connection, header);
            }
            break;
        default:
            transition(connection, ConnectionTransition.RECEIVED_HEADER);
            connectionHooks.whenError.accept(connection);
            break;
        }

    }

    public void sent(Connection<C, S, L> connection, Header header)
    {
        connection.headerSent = header.buffer().getLong(header.offset());

        switch (connection.state)
        {
        case START:
            transition(connection, ConnectionTransition.SENT_HEADER);
            connectionHooks.whenHeaderSent.accept(connection, header);
            break;
        case HEADER_RECEIVED:
            if (connection.headerReceived == connection.headerSent)
            {
                transition(connection, ConnectionTransition.SENT_HEADER);
                connectionHooks.whenHeaderSent.accept(connection, header);
            }
            else
            {
                transition(connection, ConnectionTransition.SENT_HEADER_NOT_EQUAL_RECEIVED);
                connectionHooks.whenHeaderSentNotEqualReceived.accept(connection, header);
            }
            break;
        default:
            transition(connection, ConnectionTransition.SENT_HEADER);
            connectionHooks.whenError.accept(connection);
            break;
        }
    }

    public void received(Connection<C, S, L> connection, Frame frame, Open open)
    {
        switch (connection.state)
        {
        case DISCARDING:
            transition(connection, ConnectionTransition.RECEIVED_OPEN);
            break;
        case HEADER_EXCHANGED:
        case OPEN_SENT:
        case CLOSE_PIPE:
            transition(connection, ConnectionTransition.RECEIVED_OPEN);
            connectionHooks.whenOpenReceived.accept(connection, frame, open);
            break;
        default:
            transition(connection, ConnectionTransition.RECEIVED_OPEN);
            connectionHooks.whenError.accept(connection);
            break;
        }
    }

    public void sent(Connection<C, S, L> connection, Frame frame, Open open)
    {
        switch (connection.state)
        {
        case HEADER_SENT:
        case HEADER_EXCHANGED:
        case OPEN_RECEIVED:
            transition(connection, ConnectionTransition.SENT_OPEN);
            connectionHooks.whenOpenSent.accept(connection, frame, open);
            break;
        default:
            transition(connection, ConnectionTransition.SENT_OPEN);
            connectionHooks.whenError.accept(connection);
            break;
        }
    }

    public void received(Connection<C, S, L> connection, Frame frame, Close close)
    {
        switch (connection.state)
        {
        case DISCARDING:
        case OPENED:
        case CLOSE_SENT:
            transition(connection, ConnectionTransition.RECEIVED_CLOSE);
            connectionHooks.whenCloseReceived.accept(connection, frame, close);
            break;
        default:
            transition(connection, ConnectionTransition.RECEIVED_CLOSE);
            connectionHooks.whenError.accept(connection);
            break;
        }
    }

    public void sent(Connection<C, S, L> connection, Frame frame, Close close)
    {
        switch (connection.state)
        {
        case OPEN_SENT:
        case OPEN_PIPE:
        case OPENED:
        case CLOSE_RECEIVED:
            transition(connection, ConnectionTransition.SENT_CLOSE);
            connectionHooks.whenCloseSent.accept(connection, frame, close);
            break;
        default:
            transition(connection, ConnectionTransition.SENT_CLOSE);
            connectionHooks.whenError.accept(connection);
            break;
        }
    }

    public void error(Connection<C, S, L> connection)
    {
        switch (connection.state)
        {
        case DISCARDING:
            transition(connection, ConnectionTransition.ERROR);
            break;
        default:
            transition(connection, ConnectionTransition.ERROR);
            connectionHooks.whenError.accept(connection);
            break;
        }
    }

    private static void transition(Connection<?, ?, ?> connection, ConnectionTransition transition)
    {
        connection.state = STATE_MACHINE[connection.state.ordinal()][transition.ordinal()];
    }

    private static final ConnectionState[][] STATE_MACHINE;

    static
    {
        int stateCount = ConnectionState.values().length;
        int transitionCount = ConnectionTransition.values().length;

        ConnectionState[][] stateMachine = new ConnectionState[stateCount][transitionCount];
        for (ConnectionState state : allOf(ConnectionState.class))
        {
            // default transition to "end" state
            for (ConnectionTransition transition : allOf(ConnectionTransition.class))
            {
                stateMachine[state.ordinal()][transition.ordinal()] = ConnectionState.END;
            }

            // default "error" transition to "discarding" state
            stateMachine[state.ordinal()][ConnectionTransition.ERROR.ordinal()] = ConnectionState.DISCARDING;
        }

        stateMachine[ConnectionState.START.ordinal()][ConnectionTransition.RECEIVED_HEADER.ordinal()] =
                ConnectionState.HEADER_RECEIVED;
        stateMachine[ConnectionState.START.ordinal()][ConnectionTransition.SENT_HEADER.ordinal()] = ConnectionState.HEADER_SENT;
        stateMachine[ConnectionState.HEADER_RECEIVED.ordinal()][ConnectionTransition.SENT_HEADER.ordinal()] =
                ConnectionState.HEADER_EXCHANGED;
        stateMachine[ConnectionState.HEADER_SENT.ordinal()][ConnectionTransition.RECEIVED_HEADER.ordinal()] =
                ConnectionState.HEADER_EXCHANGED;
        stateMachine[ConnectionState.HEADER_SENT.ordinal()][ConnectionTransition.SENT_OPEN.ordinal()] = ConnectionState.OPEN_PIPE;
        stateMachine[ConnectionState.HEADER_EXCHANGED.ordinal()][ConnectionTransition.RECEIVED_OPEN.ordinal()] =
                ConnectionState.OPEN_RECEIVED;
        stateMachine[ConnectionState.HEADER_EXCHANGED.ordinal()][ConnectionTransition.SENT_OPEN.ordinal()] =
                ConnectionState.OPEN_SENT;
        stateMachine[ConnectionState.OPEN_PIPE.ordinal()][ConnectionTransition.RECEIVED_HEADER.ordinal()] =
                ConnectionState.OPEN_SENT;
        stateMachine[ConnectionState.OPEN_PIPE.ordinal()][ConnectionTransition.SENT_CLOSE.ordinal()] =
                ConnectionState.OPEN_CLOSE_PIPE;
        stateMachine[ConnectionState.OPEN_CLOSE_PIPE.ordinal()][ConnectionTransition.RECEIVED_HEADER.ordinal()] =
                ConnectionState.CLOSE_PIPE;
        stateMachine[ConnectionState.OPEN_RECEIVED.ordinal()][ConnectionTransition.SENT_OPEN.ordinal()] = ConnectionState.OPENED;
        stateMachine[ConnectionState.OPEN_SENT.ordinal()][ConnectionTransition.RECEIVED_OPEN.ordinal()] = ConnectionState.OPENED;
        stateMachine[ConnectionState.OPEN_SENT.ordinal()][ConnectionTransition.SENT_CLOSE.ordinal()] =
                ConnectionState.CLOSE_PIPE;
        stateMachine[ConnectionState.CLOSE_PIPE.ordinal()][ConnectionTransition.RECEIVED_OPEN.ordinal()] =
                ConnectionState.CLOSE_SENT;
        stateMachine[ConnectionState.OPENED.ordinal()][ConnectionTransition.RECEIVED_CLOSE.ordinal()] =
                ConnectionState.CLOSE_RECEIVED;
        stateMachine[ConnectionState.OPENED.ordinal()][ConnectionTransition.SENT_CLOSE.ordinal()] =
                ConnectionState.CLOSE_SENT;
        stateMachine[ConnectionState.CLOSE_RECEIVED.ordinal()][ConnectionTransition.SENT_CLOSE.ordinal()] =
                ConnectionState.END;
        stateMachine[ConnectionState.CLOSE_SENT.ordinal()][ConnectionTransition.RECEIVED_CLOSE.ordinal()] =
                ConnectionState.END;
        stateMachine[ConnectionState.DISCARDING.ordinal()][ConnectionTransition.RECEIVED_OPEN.ordinal()] =
                ConnectionState.DISCARDING;
        stateMachine[ConnectionState.DISCARDING.ordinal()][ConnectionTransition.RECEIVED_CLOSE.ordinal()] = ConnectionState.END;

        STATE_MACHINE = stateMachine;
    }
}