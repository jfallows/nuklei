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

import java.util.HashMap;
import java.util.Map;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Close;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Header;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Open;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;
import org.kaazing.nuklei.amqp_1_0.session.Session;
import org.kaazing.nuklei.concurrent.AtomicBuffer;

public final class Connection {
    
    public final Sender sender;
    public final AtomicBuffer reassemblyBuffer;
    public final ConnectionStateMachine stateMachine;
    public final Map<Integer, Session> sessions;
    
    public long headerSent;
    public long headerReceived;

    public ConnectionState state;

    Connection(ConnectionStateMachine stateMachine, Sender sender, AtomicBuffer reassemblyBuffer) {
        this.stateMachine = stateMachine;
        this.sender = sender;
        this.reassemblyBuffer = reassemblyBuffer;
        this.sessions = new HashMap<>();
    }

    public void send(Header header) {
        assert header.buffer() == sender.sendBuffer;
        assert header.offset() == sender.sendBufferOffset;
        sender.send(header.limit());
        stateMachine.sent(this, header);
    }

    public void send(Frame frame, Open open) {
        assert frame.buffer() == sender.sendBuffer;
        assert frame.offset() == sender.sendBufferOffset;
        assert open.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, open);
    }

    public void send(Frame frame, Close close) {
        assert frame.buffer() == sender.sendBuffer;
        assert frame.offset() == sender.sendBufferOffset;
        assert close.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, close);
    }
}