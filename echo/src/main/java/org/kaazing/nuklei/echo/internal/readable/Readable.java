/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.echo.internal.readable;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static org.kaazing.nuklei.echo.internal.types.stream.Types.TYPE_ID_BEGIN;

import java.util.function.Consumer;
import java.util.function.LongFunction;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.echo.internal.Context;
import org.kaazing.nuklei.echo.internal.conductor.Conductor;
import org.kaazing.nuklei.echo.internal.readable.stream.InitialStreamPool;
import org.kaazing.nuklei.echo.internal.readable.stream.ReplyStreamPool;
import org.kaazing.nuklei.echo.internal.reader.Reader;
import org.kaazing.nuklei.echo.internal.types.stream.BeginFW;
import org.kaazing.nuklei.echo.internal.types.stream.FrameFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

@Reaktive
public final class Readable implements Nukleus, Consumer<ReadableCommand>
{
    private final FrameFW frameRO = new FrameFW();

    private final BeginFW.Builder beginRW = new BeginFW.Builder();

    private final BeginFW beginRO = new BeginFW();

    private final Conductor conductor;
    private final Reader reader;

    private final AtomicCounter streamsBound;
    private final AtomicCounter streamsPrepared;
    private final AtomicCounter streamsAccepted;
    private final AtomicCounter streamsConnected;

    private final String captureName;
    private final RingBuffer captureBuffer;

    private final Long2ObjectHashMap<ReadableState> stateByRef;

    private final Long2ObjectHashMap<MessageHandler> handlersByStreamId;
    private final Long2ObjectHashMap<LongFunction<MessageHandler>> registrationsByStreamId;

    private final AtomicBuffer atomicBuffer;

    private final InitialStreamPool initialStreamPool;
    private final ReplyStreamPool replyStreamPool;

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
        this.streamsAccepted = context.counters().streamsAccepted();
        this.streamsConnected = context.counters().streamsConnected();

        this.stateByRef = new Long2ObjectHashMap<>();
        this.handlersByStreamId = new Long2ObjectHashMap<>();
        this.registrationsByStreamId = new Long2ObjectHashMap<>();

        int maximumStreamsCount = context.maximumStreamsCount();
        this.atomicBuffer = new UnsafeBuffer(allocateDirect(captureBuffer.maxMsgLength()).order(nativeOrder()));

        this.initialStreamPool = new InitialStreamPool(maximumStreamsCount, atomicBuffer);
        this.replyStreamPool = new ReplyStreamPool(maximumStreamsCount, atomicBuffer);

        this.captureName = captureName;
        this.captureBuffer = captureBuffer;
    }

    @Override
    public String name()
    {
        return captureName;
    }

    @Override
    public int process()
    {
        int weight = 0;

        weight += captureBuffer.read(this::handleRead);

        return weight;
    }

    @Override
    public String toString()
    {
        return String.format("[name=%d]", captureName);
    }

    @Override
    public void accept(ReadableCommand command)
    {
        command.execute(this);
    }

    public void doBind(
        long correlationId,
        RingBuffer sourceRoute)
    {
        try
        {
            // positive, even, non-zero sourceRef
            streamsBound.increment();
            final long sourceRef = streamsBound.get() << 1L;

            ReadableState newState = new ReadableState(sourceRef, 0L, sourceRoute);

            stateByRef.put(newState.sourceRef(), newState);

            reader.onBoundResponse(captureName, correlationId, sourceRef);
        }
        catch (Exception e)
        {
            conductor.onErrorResponse(correlationId);
            throw new RuntimeException(e);
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
                conductor.onUnboundResponse(correlationId, captureName);
            }
        }
        catch (Exception e)
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doPrepare(
        long correlationId,
        long destinationRef,
        RingBuffer destinationRoute)
    {
        try
        {
            // positive, odd sourceRef
            final long sourceRef = (streamsPrepared.increment() << 1L) | 0x0000000000000001L;

            ReadableState newState =
                    new ReadableState(sourceRef, destinationRef, destinationRoute);

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
                long destinationRef = oldState.destinationRef();

                conductor.onUnpreparedResponse(correlationId, captureName, destinationRef);
            }
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doConnect(
        long correlationId,
        long sourceRef)
    {
        try
        {
            ReadableState state = stateByRef.get(sourceRef);

            if (state == null)
            {
                throw new IllegalStateException("unrecognized reference id: " + sourceRef);
            }
            else
            {
                // positive, odd, non-zero
                streamsConnected.increment();
                final long newInitialStreamId = (streamsConnected.get() << 1L) | 0x0000000000000001L;
                final long destinationRef = state.destinationRef();
                final RingBuffer routeBuffer = state.routeBuffer();

                registrationsByStreamId.put(newInitialStreamId, (newReplyStreamId) ->
                {
                    return replyStreamPool.acquire(newInitialStreamId, routeBuffer,
                                                   (replyStream) -> handlersByStreamId.remove(newReplyStreamId));
                });

                BeginFW beginRO = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                         .streamId(newInitialStreamId)
                                         .referenceId(destinationRef)
                                         .build();

                if (!routeBuffer.write(beginRO.typeId(), beginRO.buffer(), beginRO.offset(), beginRO.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }

                conductor.onConnectedResponse(correlationId, newInitialStreamId);
            }
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    private void handleRead(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
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
            case TYPE_ID_BEGIN:
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
        if (readableState == null || initiating(referenceId))
        {
            throw new IllegalStateException("reference not found: " + referenceId);
        }
        else
        {
            final RingBuffer routeBuffer = readableState.routeBuffer();

            // positive, even, non-zero
            streamsAccepted.increment();
            final long sourceReplyStreamId = streamsAccepted.get() << 1L;

            MessageHandler initialStream = initialStreamPool.acquire(initialStreamId, sourceReplyStreamId, routeBuffer,
                                                    (initialDecoder) -> { handlersByStreamId.remove(initialStreamId); });

            handlersByStreamId.put(initialStreamId, initialStream);

            initialStream.onMessage(TYPE_ID_BEGIN, buffer, index, length);
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
            throw new IllegalStateException("stream not found: " + replyStreamId);
        }

        MessageHandler handler = handlerSupplier.apply(replyStreamId);
        handlersByStreamId.put(replyStreamId, handler);

        handler.onMessage(TYPE_ID_BEGIN, buffer, index, length);
    }

    private static boolean initiating(
        long v)
    {
        return (v & 0x0000000000000001L) != 0L;
    }
}
