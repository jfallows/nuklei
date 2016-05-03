/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.http.internal.readable;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

import java.util.Map;
import java.util.function.LongFunction;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.http.internal.Context;
import org.kaazing.nuklei.http.internal.conductor.Conductor;
import org.kaazing.nuklei.http.internal.readable.stream.HttpInitialStreamPool;
import org.kaazing.nuklei.http.internal.readable.stream.HttpReplyStreamPool;
import org.kaazing.nuklei.http.internal.readable.stream.InitialStreamPool;
import org.kaazing.nuklei.http.internal.readable.stream.ReplyStreamPool;
import org.kaazing.nuklei.http.internal.reader.Reader;
import org.kaazing.nuklei.http.internal.types.stream.BeginFW;
import org.kaazing.nuklei.http.internal.types.stream.FrameFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

@Reaktive
public class Readable implements Nukleus
{
    private final FrameFW frameRO = new FrameFW();
    private final BeginFW beginRO = new BeginFW();

    private final Conductor conductor;
    private final Reader reader;
    private final AtomicCounter streamsBound;
    private final AtomicCounter streamsPrepared;

    private final InitialStreamPool initialStreamPool;
    private final ReplyStreamPool replyStreamPool;
    private final HttpInitialStreamPool httpInitialStreamPool;
    private final HttpReplyStreamPool httpReplyStreamPool;

    private final String captureName;
    private final RingBuffer captureBuffer;

    private final Long2ObjectHashMap<ReadableState> stateByRef;

    private final Long2ObjectHashMap<MessageHandler> handlersByStreamId;
    private final Long2ObjectHashMap<LongFunction<MessageHandler>> registrationsByStreamId;

    public Readable(
        Context context,
        Conductor conductor,
        Reader reader,
        String captureName,
        RingBuffer captureBuffer)
    {
        this.conductor = conductor;
        this.reader = reader;
        this.streamsBound = context.counters().streamsBound();
        this.streamsPrepared = context.counters().streamsPrepared();

        int maximumStreamsCount = context.maximumStreamsCount();
        AtomicCounter streamsConnected = context.counters().streamsConnected();
        AtomicCounter streamsAccepted = context.counters().streamsAccepted();
        AtomicBuffer atomicBuffer = new UnsafeBuffer(allocateDirect(captureBuffer.maxMsgLength()).order(nativeOrder()));

        this.initialStreamPool = new InitialStreamPool(maximumStreamsCount, atomicBuffer, streamsAccepted);
        this.replyStreamPool = new ReplyStreamPool(maximumStreamsCount, atomicBuffer);
        this.httpInitialStreamPool = new HttpInitialStreamPool(maximumStreamsCount, atomicBuffer, streamsConnected);
        this.httpReplyStreamPool = new HttpReplyStreamPool(maximumStreamsCount, atomicBuffer);

        this.captureName = captureName;
        this.captureBuffer = captureBuffer;

        this.stateByRef = new Long2ObjectHashMap<>();
        this.handlersByStreamId = new Long2ObjectHashMap<>();
        this.registrationsByStreamId = new Long2ObjectHashMap<>();
    }

    @Override
    public String name()
    {
        return this.captureName;
    }

    public int process()
    {
        return captureBuffer.read(this::handleRead);
    }

    @Override
    public String toString()
    {
        return String.format("[name=%d]", captureName);
    }

    public void doBind(
        long correlationId,
        long destinationRef,
        Map<String, String> headers,
        Readable destination,
        RingBuffer sourceRoute,
        RingBuffer destinationRoute)
    {
        try
        {
            // positive, even, non-zero sourceRef
            streamsBound.increment();
            final long sourceRef = streamsBound.get() << 1L;

            ReadableState newState =
                    new ReadableState(sourceRef, destination, destinationRef, headers, sourceRoute, destinationRoute);

            stateByRef.put(newState.sourceRef(), newState);

            reader.onBoundResponse(captureName, correlationId, sourceRef);
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doUnbind(
        long correlationId,
        long sourceRef)
    {
        try
        {
            ReadableState oldState = stateByRef.remove(sourceRef);

            if (oldState == null)
            {
                throw new IllegalStateException("unrecognized reference id: " + sourceRef);
            }
            else
            {
                Map<String, String> headers = oldState.headers();
                Readable destination = oldState.destination();
                long destinationRef = oldState.destinationRef();
                String destinationName = destination.name();

                conductor.onUnboundResponse(correlationId, destinationName, destinationRef, captureName, headers);
            }
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doPrepare(
        long correlationId,
        long destinationRef,
        Map<String, String> headers,
        Readable destination,
        RingBuffer sourceRoute,
        RingBuffer destinationRoute)
    {
        try
        {
            // positive, odd sourceRef
            final long sourceRef = (streamsPrepared.increment() << 1L) | 0x0000000000000001L;

            ReadableState newState =
                    new ReadableState(sourceRef, destination, destinationRef, headers, sourceRoute, destinationRoute);

            stateByRef.put(newState.sourceRef(), newState);

            reader.onPreparedResponse(captureName, correlationId, sourceRef);
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doUnprepare(
        long correlationId,
        long sourceRef)
    {
        try
        {
            ReadableState oldState = stateByRef.remove(sourceRef);

            if (oldState == null)
            {
                throw new IllegalStateException("unrecognized reference id: " + sourceRef);
            }
            else
            {
                Map<String, String> headers = oldState.headers();
                Readable destination = oldState.destination();
                long destinationRef = oldState.destinationRef();
                String destinationName = destination.name();

                conductor.onUnpreparedResponse(correlationId, destinationName, destinationRef, captureName, headers);
            }
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doRegisterEncoder(
        long destinationInitialStreamId,
        long sourceReplyStreamId,
        RingBuffer sourceRoute)
    {
        LongFunction<MessageHandler> handlerSupplier = (destinationReplyStreamId) ->
        {
            return replyStreamPool.acquire(sourceReplyStreamId, sourceRoute,
                acceptEncoder -> handlersByStreamId.remove(destinationReplyStreamId));
        };

        registrationsByStreamId.put(destinationInitialStreamId, handlerSupplier);
    }

    public void doRegisterDecoder(
        long destinationInitialStreamId,
        long sourceInitialStreamId,
        RingBuffer sourceRoute)
    {
        LongFunction<MessageHandler> handlerSupplier = (destinationReplyStreamId) ->
        {
            return httpReplyStreamPool.acquire(sourceInitialStreamId, sourceRoute,
                    connectDecoder -> handlersByStreamId.remove(destinationReplyStreamId));
        };

        registrationsByStreamId.put(destinationInitialStreamId, handlerSupplier);
    }

    private void handleRead(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        frameRO.wrap(buffer, index, index + length);

        final long streamId = frameRO.streamId();

        final MessageHandler handler = handlersByStreamId.get(streamId);

        if (handler != null)
        {
            handler.onMessage(msgTypeId, buffer, index, length);
        }
        else
        {
            switch (msgTypeId)
            {
            case BeginFW.TYPE_ID:
                if (initiating(streamId))
                {
                    handleBeginInitial(buffer, index, length);
                }
                else
                {
                    handleBeginReply(buffer, index, length);
                }
                break;

            default:
                throw new IllegalStateException("stream not found: " + streamId);
            }
        }
    }

    private void handleBeginInitial(
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        beginRO.wrap(buffer, index, index + length);

        final long initialStreamId = beginRO.streamId();
        final long referenceId = beginRO.referenceId();

        ReadableState readableState = stateByRef.get(referenceId);
        if (readableState == null)
        {
            throw new IllegalStateException("reference not found: " + referenceId);
        }
        else
        {
            final RingBuffer sourceRoute = readableState.sourceRoute();
            final long destinationRef = readableState.destinationRef();
            final RingBuffer destinationRoute = readableState.destinationRoute();
            final Readable destination = readableState.destination();

            if (initiating(referenceId))
            {
                MessageHandler httpInitialStream =
                        httpInitialStreamPool.acquire(destinationRef, sourceRoute, destinationRoute, destination,
                                connectEncoder -> handlersByStreamId.remove(initialStreamId));

                handlersByStreamId.put(initialStreamId, httpInitialStream);

                httpInitialStream.onMessage(BeginFW.TYPE_ID, buffer, index, length);
            }
            else
            {
                // positive, even, non-zero
                final long sourceReplyStreamId = (initialStreamId & ~1L) << 1L;

                MessageHandler initialStream =
                        initialStreamPool.acquire(destinationRef, sourceReplyStreamId, destination, sourceRoute,
                                destinationRoute, acceptDecoder -> handlersByStreamId.remove(initialStreamId));

                handlersByStreamId.put(initialStreamId, initialStream);

                initialStream.onMessage(BeginFW.TYPE_ID, buffer, index, length);
            }
        }
    }

    private void handleBeginReply(
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        beginRO.wrap(buffer, index, index + length);

        final long replyStreamId = beginRO.streamId();
        final long initialStreamId = beginRO.referenceId();

        LongFunction<MessageHandler> handlerSupplier = registrationsByStreamId.remove(initialStreamId);
        if (handlerSupplier == null)
        {
            throw new ReadableException("stream not found: " + replyStreamId);
        }

        MessageHandler handler = handlerSupplier.apply(replyStreamId);
        handlersByStreamId.put(replyStreamId, handler);

        handler.onMessage(BeginFW.TYPE_ID, buffer, index, length);
    }

    private static boolean initiating(
        long v)
    {
        return (v & 0x0000000000000001L) != 0L;
    }
}
