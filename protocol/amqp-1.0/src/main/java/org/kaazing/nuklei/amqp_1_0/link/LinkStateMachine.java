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

import static java.util.EnumSet.allOf;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Attach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Detach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Transfer;

/*
 * See AMQP 1.0 specification, section 2.6 "Links"
 */
public final class LinkStateMachine<L>
{

    private final LinkHooks<L> linkHooks;

    public LinkStateMachine(LinkHooks<L> linkHooks)
    {
        this.linkHooks = linkHooks;
    }

    public void start(Link<L> link)
    {
        link.state = LinkState.DETACHED;
        linkHooks.whenInitialized.accept(link);
    }

    public void received(Link<L> link, Frame frame, Attach attach)
    {
        transition(link, LinkTransition.RECEIVED_ATTACH);
        linkHooks.whenAttachReceived.accept(link, frame, attach);
    }

    public void sent(Link<L> link, Frame frame, Attach attach)
    {
        transition(link, LinkTransition.SENT_ATTACH);
        linkHooks.whenAttachSent.accept(link, frame, attach);
    }

    public void received(Link<L> link, Frame frame, Transfer transfer)
    {
        transition(link, LinkTransition.RECEIVED_TRANSFER);
        linkHooks.whenTransferReceived.accept(link, frame, transfer);
    }

    public void sent(Link<L> link, Frame frame, Transfer transfer)
    {
        transition(link, LinkTransition.SENT_TRANSFER);
        linkHooks.whenTransferSent.accept(link, frame, transfer);
    }

    public void received(Link<L> link, Frame frame, Detach detach)
    {
        transition(link, LinkTransition.RECEIVED_DETACH);
        linkHooks.whenDetachReceived.accept(link, frame, detach);
    }

    public void sent(Link<L> link, Frame frame, Detach detach)
    {
        transition(link, LinkTransition.SENT_DETACH);
        linkHooks.whenDetachSent.accept(link, frame, detach);
    }

    // TODO: implement?  Also need Flow frames?
//    public void received(Link<L> link, Frame frame, Disposition disposition)
//    {
//        transition(link, LinkTransition.RECEIVED_DISPOSITION);
//        linkHooks.whenDispositionReceived.accept(link, frame, disposition);
//    }
//
//    public void sent(Link<L> link, Frame frame, Disposition disposition)
//    {
//        transition(link, LinkTransition.SENT_DISPOSITION);
//        linkHooks.whenDispositionSent.accept(link, frame, disposition);
//    }

    public void error(Link<L> link)
    {
        transition(link, LinkTransition.ERROR);
        linkHooks.whenError.accept(link);
    }

    private static void transition(Link<?> link, LinkTransition transition)
    {
        link.state = STATE_MACHINE[link.state.ordinal()][transition.ordinal()];
    }

    private static final LinkState[][] STATE_MACHINE;

    static
    {
        int stateCount = LinkState.values().length;
        int transitionCount = LinkTransition.values().length;

        LinkState[][] stateMachine = new LinkState[stateCount][transitionCount];
        for (LinkState state : allOf(LinkState.class))
        {
            for (LinkTransition transition : allOf(LinkTransition.class))
            {
                // default transition to "end" state
                stateMachine[state.ordinal()][transition.ordinal()] = LinkState.DETACHED;
            }

            // default "error" transition to "discarding" state
            stateMachine[state.ordinal()][LinkTransition.ERROR.ordinal()] = LinkState.DISCARDING;
        }

        stateMachine[LinkState.DETACHED.ordinal()][LinkTransition.RECEIVED_ATTACH.ordinal()] = LinkState.ATTACH_RECEIVED;
        stateMachine[LinkState.DETACHED.ordinal()][LinkTransition.SENT_ATTACH.ordinal()] = LinkState.ATTACH_SENT;
        stateMachine[LinkState.ATTACH_RECEIVED.ordinal()][LinkTransition.SENT_ATTACH.ordinal()] = LinkState.ATTACHED;
        stateMachine[LinkState.ATTACH_SENT.ordinal()][LinkTransition.RECEIVED_ATTACH.ordinal()] = LinkState.ATTACHED;
        stateMachine[LinkState.ATTACHED.ordinal()][LinkTransition.RECEIVED_DETACH.ordinal()] = LinkState.DETACH_RECEIVED;
        stateMachine[LinkState.ATTACHED.ordinal()][LinkTransition.SENT_DETACH.ordinal()] = LinkState.DETACH_SENT;
        stateMachine[LinkState.ATTACHED.ordinal()][LinkTransition.RECEIVED_TRANSFER.ordinal()] = LinkState.ATTACHED;
        stateMachine[LinkState.ATTACHED.ordinal()][LinkTransition.SENT_TRANSFER.ordinal()] = LinkState.ATTACHED;
        stateMachine[LinkState.DETACH_RECEIVED.ordinal()][LinkTransition.SENT_DETACH.ordinal()] = LinkState.DETACHED;
        stateMachine[LinkState.DETACH_SENT.ordinal()][LinkTransition.RECEIVED_DETACH.ordinal()] = LinkState.DETACHED;

        STATE_MACHINE = stateMachine;
    }

}