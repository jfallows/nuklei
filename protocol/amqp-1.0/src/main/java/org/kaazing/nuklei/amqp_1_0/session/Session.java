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
package org.kaazing.nuklei.amqp_1_0.session;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Begin;
import org.kaazing.nuklei.amqp_1_0.codec.transport.End;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.link.Link;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;

import uk.co.real_logic.agrona.collections.Int2ObjectHashMap;

public class Session<S, L>
{

    public final SessionStateMachine<S, L> stateMachine;
    public final Sender sender;
    public final Int2ObjectHashMap<Link<L>> links;

    public SessionState state;
    public S parameter;

    public Session(SessionStateMachine<S, L> stateMachine, Sender sender)
    {
        this.stateMachine = stateMachine;
        this.sender = sender;
        this.links = new Int2ObjectHashMap<>();
    }

    public void send(Frame frame, Begin begin)
    {
        assert begin.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, begin);
    }

    public void send(Frame frame, End end)
    {
        assert end.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, end);
    }
}