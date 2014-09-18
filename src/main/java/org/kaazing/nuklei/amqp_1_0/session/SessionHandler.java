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

import org.kaazing.nuklei.amqp_1_0.codec.transport.Attach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Begin;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Detach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Disposition;
import org.kaazing.nuklei.amqp_1_0.codec.transport.End;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Flow;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Transfer;
import org.kaazing.nuklei.amqp_1_0.link.Link;
import org.kaazing.nuklei.amqp_1_0.link.LinkFactory;
import org.kaazing.nuklei.amqp_1_0.link.LinkHandler;

public final class SessionHandler {

    private final LinkFactory linkFactory;
    private final LinkHandler linkHandler;

    public SessionHandler(LinkFactory linkFactory, LinkHandler linkHandler) {
        this.linkFactory = linkFactory;
        this.linkHandler = linkHandler;
    }

    public void init(Session session) {
        session.stateMachine.start(session);
    }
    
    public void handle(Session session, Frame frame) {
        switch (frame.getPerformative()) {
        case BEGIN:
            Begin begin = Begin.LOCAL_REF.get().wrap(frame.buffer(), frame.bodyOffset());
            session.stateMachine.received(session, frame, begin);
            break;
        case FLOW:
            Flow flow = Flow.LOCAL_REF.get().wrap(frame.buffer(), frame.bodyOffset());
            session.stateMachine.received(session, frame, flow);
            break;
        case DISPOSITION:
            Disposition disposition = Disposition.LOCAL_REF.get().wrap(frame.buffer(), frame.bodyOffset());
            session.stateMachine.received(session, frame, disposition);
            break;
        case END:
            End end = End.LOCAL_REF.get().wrap(frame.buffer(), frame.bodyOffset());
            session.stateMachine.received(session, frame, end);
            break;
        case ATTACH:
            handleLinkAttach(session, frame);
            break;
        case TRANSFER:
            handleLinkTransfer(session, frame);
            break;
        case DETACH:
            handleLinkDetach(session, frame);
            break;
        default:
            session.stateMachine.error(session);
            break;
        }
    }

    private void handleLinkAttach(Session session, Frame frame) {
        Attach attach = Attach.LOCAL_REF.get().wrap(frame.buffer(), frame.bodyOffset());
        int newHandle = (int) attach.getHandle();
        Link newLink = session.links.get(newHandle);
        if (newLink == null) {
            newLink = linkFactory.newLink(session.sender);
            session.links.put(newHandle, newLink);
            linkHandler.init(newLink);
        }
        linkHandler.handle(newLink, frame);
    }

    private void handleLinkTransfer(Session session, Frame frame) {
        Transfer transfer = Transfer.LOCAL_REF.get().wrap(frame.buffer(), frame.bodyOffset());
        int handle = (int) transfer.getHandle();
        Link link = session.links.get(handle);
        if (link == null) {
            session.stateMachine.error(session);
        }
        else {
            linkHandler.handle(link, frame);
        }
    }

    private void handleLinkDetach(Session session, Frame frame) {
        Detach detach = Detach.LOCAL_REF.get().wrap(frame.buffer(), frame.bodyOffset());
        int oldHandle = (int) detach.getHandle();
        Link oldLink = session.links.remove(oldHandle);
        if (oldLink == null) {
            session.stateMachine.error(session);
        }
        else {
            linkHandler.handle(oldLink, frame);
        }
    }
}
