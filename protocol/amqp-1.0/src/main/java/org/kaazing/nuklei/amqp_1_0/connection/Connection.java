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
import org.kaazing.nuklei.amqp_1_0.sender.Sender;
import org.kaazing.nuklei.amqp_1_0.session.Session;
import org.kaazing.nuklei.function.AlignedMikro.Storage;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Int2ObjectHashMap;

public class Connection<C, S, L>
{

    public final Sender sender;
    public final Storage reassemblyStorage;
    public final ConnectionStateMachine<C, S, L> stateMachine;
    public final Int2ObjectHashMap<Session<S, L>> sessions;

    public long headerSent;
    public long headerReceived;

    public C parameter;
    public ConnectionState state;

    public Connection(ConnectionStateMachine<C, S, L> stateMachine, Sender sender, MutableDirectBuffer reassemblyBuffer)
    {
        this.stateMachine = stateMachine;
        this.sender = sender;
        this.reassemblyStorage = new Storage(reassemblyBuffer);
        this.sessions = new Int2ObjectHashMap<>();
    }

    public void send(Header header)
    {
        sender.send(header.limit());
        stateMachine.sent(this, header);
    }

    public void send(Frame frame, Open open)
    {
        assert open.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, open);
    }

    public void send(Frame frame, Close close)
    {
        assert close.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, close);
    }
}