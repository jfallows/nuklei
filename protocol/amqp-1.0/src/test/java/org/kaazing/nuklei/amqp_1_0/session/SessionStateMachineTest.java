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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.Test;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Begin;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Disposition;
import org.kaazing.nuklei.amqp_1_0.codec.transport.End;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Flow;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Frame;
import org.kaazing.nuklei.amqp_1_0.function.FrameConsumer;
import org.kaazing.nuklei.amqp_1_0.sender.Sender;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class SessionStateMachineTest
{

    private final SessionHooks<Void, Void> sessionHooks = new SessionHooks<>();
    private final SessionStateMachine<Void, Void> stateMachine = new SessionStateMachine<>(sessionHooks);
    private final Session<Void, Void> session = new Session<>(stateMachine, mock(Sender.class));

    private final Frame frame = Frame.LOCAL_REF.get().wrap(new UnsafeBuffer(new byte[64]), 0, true);
    private final Begin begin = Begin.LOCAL_REF.get();
    private final Flow flow = Flow.LOCAL_REF.get();
    private final Disposition disposition = Disposition.LOCAL_REF.get();
    private final End end = End.LOCAL_REF.get();

    @Test
    @SuppressWarnings("unchecked")
    public void shouldInitializeInUnmapped()
    {
        sessionHooks.whenInitialized = mock(Consumer.class);

        stateMachine.start(session);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenInitialized).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromUnmappedToBeginReceivedWhenReceiveBegin()
    {
        sessionHooks.whenBeginReceived = mock(FrameConsumer.class);
        session.state = SessionState.UNMAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.received(session, frame, begin);

        assertSame(SessionState.BEGIN_RECEIVED, session.state);

        verify(sessionHooks.whenBeginReceived).accept(session, frame, begin);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromUnmappedToBeginSentWhenSendBegin()
    {
        sessionHooks.whenBeginSent = mock(FrameConsumer.class);
        session.state = SessionState.UNMAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.sent(session, frame, begin);

        assertSame(SessionState.BEGIN_SENT, session.state);

        verify(sessionHooks.whenBeginSent).accept(session, frame, begin);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromUnmappedToUnmappedWhenReceiveFlow()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.UNMAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, flow);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromUnmappedToUnmappedWhenSendFlow()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.UNMAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, flow);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromUnmappedToUnmappedWhenReceiveDisposition()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.UNMAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, disposition);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromUnmappedToUnmappedWhenSendDisposition()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.UNMAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, disposition);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromUnmappedToUnmappedWhenReceiveEnd()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.UNMAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromUnmappedToUnmappedWhenSendEnd()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.UNMAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginSentToMappedWhenReceiveBegin()
    {
        sessionHooks.whenBeginReceived = mock(FrameConsumer.class);
        session.state = SessionState.BEGIN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.received(session, frame, begin);

        assertSame(SessionState.MAPPED, session.state);

        verify(sessionHooks.whenBeginReceived).accept(session, frame, begin);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginSentToUnmappedWhenSendBegin()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.sent(session, frame, begin);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginSentToUnmappedWhenReceiveFlow()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, flow);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginSentToUnmappedWhenSendFlow()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, flow);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginSentToUnmappedWhenReceiveDisposition()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, disposition);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginSentToUnmappedWhenSendDisposition()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, disposition);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginSentToUnmappedWhenReceiveEnd()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginSentToUnmappedWhenSendEnd()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginReceivedToUnmappedWhenReceiveBegin()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.received(session, frame, begin);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginReceivedToMappedWhenSendBegin()
    {
        sessionHooks.whenBeginSent = mock(FrameConsumer.class);
        session.state = SessionState.BEGIN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.sent(session, frame, begin);

        assertSame(SessionState.MAPPED, session.state);

        verify(sessionHooks.whenBeginSent).accept(session, frame, begin);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginReceivedToUnmappedWhenReceiveFlow()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, flow);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginReceivedToUnmappedWhenSendFlow()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, flow);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginReceivedToUnmappedWhenReceiveDisposition()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, disposition);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginReceivedToUnmappedWhenSendDisposition()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, disposition);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginReceivedToUnmappedWhenReceiveEnd()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromBeginReceivedToUnmappedWhenSendEnd()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.BEGIN_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToUnmappedWhenReceiveBegin()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.received(session, frame, begin);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToUnmappedWhenSendBegin()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.sent(session, frame, begin);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToEndReceivedWhenReceiveEnd()
    {
        sessionHooks.whenEndReceived = mock(FrameConsumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, end);

        assertSame(SessionState.END_RECEIVED, session.state);

        verify(sessionHooks.whenEndReceived).accept(session, frame, end);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToEndSentWhenSendEnd()
    {
        sessionHooks.whenEndSent = mock(FrameConsumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, end);

        assertSame(SessionState.END_SENT, session.state);

        verify(sessionHooks.whenEndSent).accept(session, frame, end);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToMappedWhenReceiveFlow()
    {
        sessionHooks.whenFlowReceived = mock(FrameConsumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, flow);

        assertSame(SessionState.MAPPED, session.state);

        verify(sessionHooks.whenFlowReceived).accept(session, frame, flow);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToMappedWhenSendFlow()
    {
        sessionHooks.whenFlowSent = mock(FrameConsumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, flow);

        assertSame(SessionState.MAPPED, session.state);

        verify(sessionHooks.whenFlowSent).accept(session, frame, flow);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToMappedWhenReceiveDisposition()
    {
        sessionHooks.whenDispositionReceived = mock(FrameConsumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, disposition);

        assertSame(SessionState.MAPPED, session.state);

        verify(sessionHooks.whenDispositionReceived).accept(session, frame, disposition);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToMappedWhenSendDisposition()
    {
        sessionHooks.whenDispositionSent = mock(FrameConsumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, disposition);

        assertSame(SessionState.MAPPED, session.state);

        verify(sessionHooks.whenDispositionSent).accept(session, frame, disposition);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromMappedToDiscardingWhenError()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.MAPPED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.error(session);

        assertSame(SessionState.DISCARDING, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToDiscardingWhenReceiveBegin()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.received(session, frame, begin);

        assertSame(SessionState.DISCARDING, session.state);

        verify(sessionHooks.whenError, never()).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToUnmappedWhenSendBegin()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.BEGIN);
        begin.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).setRemoteChannel(0x01)
                .setNextOutgoingId(0x0011223344556677L);
        frame.bodyChanged();

        stateMachine.sent(session, frame, begin);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToUnmappedWhenReceiveEnd()
    {
        sessionHooks.whenEndReceived = mock(FrameConsumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenEndReceived).accept(session, frame, end);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToUnmappedWhenSendEnd()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToDiscardingWhenReceiveFlow()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, flow);

        assertSame(SessionState.DISCARDING, session.state);

        verify(sessionHooks.whenError, never()).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToUnmappedWhenSendFlow()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.FLOW);
        flow.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, flow);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToDiscardingWhenReceiveDisposition()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, disposition);

        assertSame(SessionState.DISCARDING, session.state);

        verify(sessionHooks.whenError, never()).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToUnmappedWhenSendDisposition()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.DISPOSITION);
        disposition.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, disposition);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenError).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromDiscardingToDiscardingWhenError()
    {
        sessionHooks.whenError = mock(Consumer.class);
        session.state = SessionState.DISCARDING;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.error(session);

        assertSame(SessionState.DISCARDING, session.state);

        verify(sessionHooks.whenError, never()).accept(session);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromEndSentToUnmappedWhenReceiveEnd()
    {
        sessionHooks.whenEndReceived = mock(FrameConsumer.class);
        session.state = SessionState.END_SENT;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.received(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenEndReceived).accept(session, frame, end);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransitionFromEndReceivedToUnmappedWhenSendEnd()
    {
        sessionHooks.whenEndSent = mock(FrameConsumer.class);
        session.state = SessionState.END_RECEIVED;

        frame.setChannel(0x00).setDataOffset(0x02).setType(0x01).setPerformative(Performative.END);
        end.wrap(frame.mutableBuffer(), frame.bodyOffset(), true).maxLength(255).clear();
        frame.bodyChanged();

        stateMachine.sent(session, frame, end);

        assertSame(SessionState.UNMAPPED, session.state);

        verify(sessionHooks.whenEndSent).accept(session, frame, end);
    }

}