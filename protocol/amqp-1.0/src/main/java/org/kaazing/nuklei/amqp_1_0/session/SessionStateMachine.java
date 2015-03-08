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

import static java.util.EnumSet.allOf;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Begin;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Disposition;
import org.kaazing.nuklei.amqp_1_0.codec.transport.End;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Flow;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;

/*
 * See AMQP 1.0 specification, section 2.5.5 "Session States"
 */
public final class SessionStateMachine<S, L>
{

    private final SessionHooks<S, L> sessionHooks;

    public SessionStateMachine(SessionHooks<S, L> sessionHooks)
    {
        this.sessionHooks = sessionHooks;
    }

    public void start(Session<S, L> session)
    {
        session.state = SessionState.UNMAPPED;
        sessionHooks.whenInitialized.accept(session);
    }

    public void received(Session<S, L> session, Frame frame, Begin begin)
    {
        switch (session.state)
        {
        case DISCARDING:
            transition(session, SessionTransition.RECEIVED_BEGIN);
            break;
        case UNMAPPED:
        case BEGIN_SENT:
            transition(session, SessionTransition.RECEIVED_BEGIN);
            sessionHooks.whenBeginReceived.accept(session, frame, begin);
            break;
        default:
            transition(session, SessionTransition.RECEIVED_BEGIN);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    public void sent(Session<S, L> session, Frame frame, Begin begin)
    {
        switch (session.state)
        {
        case UNMAPPED:
        case BEGIN_RECEIVED:
            transition(session, SessionTransition.SENT_BEGIN);
            sessionHooks.whenBeginSent.accept(session, frame, begin);
            break;
        default:
            transition(session, SessionTransition.SENT_BEGIN);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    public void received(Session<S, L> session, Frame frame, Flow flow)
    {
        switch (session.state)
        {
        case DISCARDING:
            transition(session, SessionTransition.RECEIVED_FLOW);
            break;
        case MAPPED:
            transition(session, SessionTransition.RECEIVED_FLOW);
            sessionHooks.whenFlowReceived.accept(session, frame, flow);
            break;
        default:
            transition(session, SessionTransition.RECEIVED_FLOW);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    public void sent(Session<S, L> session, Frame frame, Flow flow)
    {
        switch (session.state)
        {
        case MAPPED:
            transition(session, SessionTransition.SENT_FLOW);
            sessionHooks.whenFlowSent.accept(session, frame, flow);
            break;
        default:
            transition(session, SessionTransition.SENT_FLOW);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    public void received(Session<S, L> session, Frame frame, Disposition disposition)
    {
        switch (session.state)
        {
        case DISCARDING:
            transition(session, SessionTransition.RECEIVED_DISPOSITION);
            break;
        case MAPPED:
            transition(session, SessionTransition.RECEIVED_DISPOSITION);
            sessionHooks.whenDispositionReceived.accept(session, frame, disposition);
            break;
        default:
            transition(session, SessionTransition.RECEIVED_DISPOSITION);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    public void sent(Session<S, L> session, Frame frame, Disposition disposition)
    {
        switch (session.state)
        {
        case MAPPED:
            transition(session, SessionTransition.SENT_DISPOSITION);
            sessionHooks.whenDispositionSent.accept(session, frame, disposition);
            break;
        default:
            transition(session, SessionTransition.SENT_DISPOSITION);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    public void received(Session<S, L> session, Frame frame, End end)
    {
        switch (session.state)
        {
        case MAPPED:
        case END_SENT:
        case DISCARDING:
            transition(session, SessionTransition.RECEIVED_END);
            sessionHooks.whenEndReceived.accept(session, frame, end);
            break;
        default:
            transition(session, SessionTransition.RECEIVED_END);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    public void sent(Session<S, L> session, Frame frame, End end)
    {
        switch (session.state)
        {
        case MAPPED:
        case END_RECEIVED:
            transition(session, SessionTransition.SENT_END);
            sessionHooks.whenEndSent.accept(session, frame, end);
            break;
        default:
            transition(session, SessionTransition.SENT_END);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    public void error(Session<S, L> session)
    {
        switch (session.state)
        {
        case DISCARDING:
            transition(session, SessionTransition.ERROR);
            break;
        default:
            transition(session, SessionTransition.ERROR);
            sessionHooks.whenError.accept(session);
            break;
        }
    }

    private static void transition(Session<?, ?> session, SessionTransition transition)
    {
        session.state = STATE_MACHINE[session.state.ordinal()][transition.ordinal()];
    }

    private static final SessionState[][] STATE_MACHINE;

    static
    {
        int stateCount = SessionState.values().length;
        int transitionCount = SessionTransition.values().length;

        SessionState[][] stateMachine = new SessionState[stateCount][transitionCount];
        for (SessionState state : allOf(SessionState.class))
        {
            for (SessionTransition transition : allOf(SessionTransition.class))
            {
                // default transition to "unmapped" state
                stateMachine[state.ordinal()][transition.ordinal()] = SessionState.UNMAPPED;
            }

            // default "error" transition to "discarding" state
            stateMachine[state.ordinal()][SessionTransition.ERROR.ordinal()] = SessionState.DISCARDING;
        }

        stateMachine[SessionState.UNMAPPED.ordinal()][SessionTransition.RECEIVED_BEGIN.ordinal()] = SessionState.BEGIN_RECEIVED;
        stateMachine[SessionState.UNMAPPED.ordinal()][SessionTransition.SENT_BEGIN.ordinal()] = SessionState.BEGIN_SENT;
        stateMachine[SessionState.BEGIN_RECEIVED.ordinal()][SessionTransition.SENT_BEGIN.ordinal()] = SessionState.MAPPED;
        stateMachine[SessionState.BEGIN_SENT.ordinal()][SessionTransition.RECEIVED_BEGIN.ordinal()] = SessionState.MAPPED;
        stateMachine[SessionState.MAPPED.ordinal()][SessionTransition.RECEIVED_END.ordinal()] = SessionState.END_RECEIVED;
        stateMachine[SessionState.MAPPED.ordinal()][SessionTransition.SENT_END.ordinal()] = SessionState.END_SENT;
        stateMachine[SessionState.MAPPED.ordinal()][SessionTransition.RECEIVED_FLOW.ordinal()] = SessionState.MAPPED;
        stateMachine[SessionState.MAPPED.ordinal()][SessionTransition.SENT_FLOW.ordinal()] = SessionState.MAPPED;
        stateMachine[SessionState.MAPPED.ordinal()][SessionTransition.RECEIVED_DISPOSITION.ordinal()] = SessionState.MAPPED;
        stateMachine[SessionState.MAPPED.ordinal()][SessionTransition.SENT_DISPOSITION.ordinal()] = SessionState.MAPPED;
        stateMachine[SessionState.END_RECEIVED.ordinal()][SessionTransition.SENT_END.ordinal()] = SessionState.UNMAPPED;
        stateMachine[SessionState.END_SENT.ordinal()][SessionTransition.RECEIVED_END.ordinal()] = SessionState.UNMAPPED;
        stateMachine[SessionState.DISCARDING.ordinal()][SessionTransition.RECEIVED_BEGIN.ordinal()] = SessionState.DISCARDING;
        stateMachine[SessionState.DISCARDING.ordinal()][SessionTransition.SENT_BEGIN.ordinal()] = SessionState.UNMAPPED;
        stateMachine[SessionState.DISCARDING.ordinal()][SessionTransition.RECEIVED_END.ordinal()] = SessionState.UNMAPPED;
        stateMachine[SessionState.DISCARDING.ordinal()][SessionTransition.SENT_END.ordinal()] = SessionState.UNMAPPED;
        stateMachine[SessionState.DISCARDING.ordinal()][SessionTransition.RECEIVED_FLOW.ordinal()] = SessionState.DISCARDING;
        stateMachine[SessionState.DISCARDING.ordinal()][SessionTransition.SENT_FLOW.ordinal()] = SessionState.UNMAPPED;
        stateMachine[SessionState.DISCARDING.ordinal()][SessionTransition.RECEIVED_DISPOSITION.ordinal()] =
                SessionState.DISCARDING;
        stateMachine[SessionState.DISCARDING.ordinal()][SessionTransition.SENT_DISPOSITION.ordinal()] = SessionState.UNMAPPED;

        STATE_MACHINE = stateMachine;
    }

}