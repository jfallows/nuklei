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
package org.kaazing.nuklei.amqp_1_0.link;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Attach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Detach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Disposition;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Transfer;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;

/*
 * See AMQP 1.0 specification, section 2.6 "Links"
 */
public class Link<L>
{

    public final LinkStateMachine<L> stateMachine;
    public final Sender sender;

    public LinkState state;
    public L parameter;

    public Link(LinkStateMachine<L> stateMachine, Sender sender)
    {
        this.stateMachine = stateMachine;
        this.sender = sender;
    }

    public void send(Frame frame, Attach attach)
    {
        assert attach.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, attach);
    }

    public void send(Frame frame, Detach detach)
    {
        assert detach.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, detach);
    }

    public void send(Frame frame, Disposition disposition)
    {
        assert disposition.limit() == frame.limit();
        sender.send(frame.limit());
        // TODO: implement state machine for this frame?
//        stateMachine.sent(this, frame, disposition);
    }

    public void send(Frame frame, Transfer transfer)
    {
        assert transfer.limit() == frame.limit();
        sender.send(frame.limit());
        stateMachine.sent(this, frame, transfer);
    }
}