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
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Transfer;

/*
 * See AMQP 1.0 specification, section 2.6 "Links"
 */
public final class LinkHandler<L>
{

    public void init(Link<L> session)
    {
        session.stateMachine.start(session);
    }

    public void handle(Link<L> link, Frame frame)
    {
        switch (frame.getPerformative())
        {
        case ATTACH:
            Attach attach = Attach.LOCAL_REF.get().wrap(frame.mutableBuffer(), frame.bodyOffset(), true);
            link.stateMachine.received(link, frame, attach);
            break;
        case TRANSFER:
            Transfer transfer = Transfer.LOCAL_REF.get().wrap(frame.mutableBuffer(), frame.bodyOffset(), true);
            link.stateMachine.received(link, frame, transfer);
            break;
        case DETACH:
            Detach detach = Detach.LOCAL_REF.get().wrap(frame.mutableBuffer(), frame.bodyOffset(), true);
            link.stateMachine.received(link, frame, detach);
            break;
        default:
            link.stateMachine.error(link);
            break;
        }
    }
}
