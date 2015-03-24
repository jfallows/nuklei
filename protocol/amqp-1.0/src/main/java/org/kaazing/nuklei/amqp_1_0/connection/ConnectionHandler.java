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

import org.kaazing.nuklei.amqp_1_0.codec.transport.Close;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Open;
import org.kaazing.nuklei.amqp_1_0.session.Session;
import org.kaazing.nuklei.amqp_1_0.session.SessionFactory;
import org.kaazing.nuklei.amqp_1_0.session.SessionHandler;

public final class ConnectionHandler<C, S, L>
{

    private final SessionHandler<S, L> sessionHandler;
    private final SessionFactory<C, S, L> sessionFactory;

    public ConnectionHandler(SessionFactory<C, S, L> sessionFactory, SessionHandler<S, L> sessionHandler)
    {
        this.sessionHandler = sessionHandler;
        this.sessionFactory = sessionFactory;
    }

    public void init(Connection<C, S, L> connection)
    {
        connection.stateMachine.start(connection);
    }

    public void handleHeader(final Connection<C, S, L> connection, final Header header)
    {
        connection.stateMachine.received(connection, header);
    }

    public void handleFrame(final Connection<C, S, L> connection, final Frame frame)
    {

        switch (frame.getPerformative())
        {
        case OPEN:
            Open open = Open.LOCAL_REF.get().wrap(frame.mutableBuffer(), frame.bodyOffset(), true);
            connection.stateMachine.received(connection, frame, open);
            break;
        case CLOSE:
            Close close = Close.LOCAL_REF.get().wrap(frame.mutableBuffer(), frame.bodyOffset(), true);
            connection.stateMachine.received(connection, frame, close);
            break;
        case BEGIN:
            handleSessionBegin(connection, frame);
            break;
        case ATTACH:
        case FLOW:
        case TRANSFER:
        case DISPOSITION:
        case DETACH:
            handleSessionFrame(connection, frame);
            break;
        case END:
            handleSessionEnd(connection, frame);
            break;
        }
    }

    public void destroy(Connection<C, S, L> connection)
    {
    }

    private void handleSessionBegin(final Connection<C, S, L> connection, final Frame frame)
    {
        int newChannel = frame.getChannel();
        Session<S, L> newSession = connection.sessions.get(newChannel);
        if (newSession == null)
        {
            newSession = sessionFactory.newSession(connection);
            connection.sessions.put(newChannel, newSession);
            sessionHandler.init(newSession);
        }
        sessionHandler.handle(newSession, frame);
    }

    private void handleSessionFrame(final Connection<C, S, L> connection, final Frame frame)
    {
        int channel = frame.getChannel();
        Session<S, L> session = connection.sessions.get(channel);
        if (session == null)
        {
            connection.stateMachine.error(connection);
        }
        else
        {
            sessionHandler.handle(session, frame);
        }
    }

    private void handleSessionEnd(final Connection<C, S, L> connection, final Frame frame)
    {
        int oldChannel = frame.getChannel();
        Session<S, L> oldSession = connection.sessions.remove(oldChannel);
        if (oldSession == null)
        {
            connection.stateMachine.error(connection);
        }
        else
        {
            sessionHandler.handle(oldSession, frame);
        }
    }
}
