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
package org.kaazing.nuklei.echo.internal.reflector;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static org.kaazing.nuklei.echo.internal.types.stream.BeginFW.BEGIN_TYPE_ID;
import static org.kaazing.nuklei.echo.internal.types.stream.DataFW.DATA_TYPE_ID;
import static org.kaazing.nuklei.echo.internal.types.stream.EndFW.END_TYPE_ID;

import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.echo.internal.Context;
import org.kaazing.nuklei.echo.internal.types.stream.BeginFW;
import org.kaazing.nuklei.echo.internal.types.stream.DataFW;
import org.kaazing.nuklei.echo.internal.types.stream.EndFW;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.ArrayUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Reflector implements Nukleus, Consumer<ReflectorCommand>
{
    private static final int MAX_RECEIVE_LENGTH = 1024; // TODO: Configuration and Context

    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final EndFW.Builder endRW = new EndFW.Builder();

    private final BeginFW beginRO = new BeginFW();
    private final EndFW endRO = new EndFW();
    private final DataFW dataRO = new DataFW();

    private final OneToOneConcurrentArrayQueue<ReflectorCommand> commandQueue;
    private final Long2ObjectHashMap<RegistrationState> stateByReferenceId;
    private final Long2ObjectHashMap<StreamState> stateByStreamId;
    private final AtomicBuffer atomicBuffer;
    private final MessageHandler readAcceptHandler;
    private final MessageHandler readConnectHandler;

    private RingBuffer[] readAcceptBuffers;
    private RingBuffer[] readConnectBuffers;

    public Reflector(Context context)
    {
        this.commandQueue = context.reflectorCommandQueue();
        this.stateByReferenceId = new Long2ObjectHashMap<>();
        this.stateByStreamId = new Long2ObjectHashMap<>();
        this.atomicBuffer = new UnsafeBuffer(allocateDirect(MAX_RECEIVE_LENGTH).order(nativeOrder()));
        this.readAcceptBuffers = new RingBuffer[0];
        this.readConnectBuffers = new RingBuffer[0];
        this.readAcceptHandler = this::handleReadAccept;
        this.readConnectHandler = this::handleReadConnect;
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += commandQueue.drain(this);

        for (int i=0; i < readAcceptBuffers.length; i++)
        {
            weight += readAcceptBuffers[i].read(readAcceptHandler);
        }

        for (int i=0; i < readConnectBuffers.length; i++)
        {
            weight += readConnectBuffers[i].read(readConnectHandler);
        }

        return weight;
    }

    @Override
    public String name()
    {
        return "reflector";
    }

    @Override
    public void accept(ReflectorCommand command)
    {
        command.execute(this);
    }

    public void doRegister(
        long referenceId,
        ReflectorMode mode,
        RingBuffer readBuffer,
        RingBuffer writeBuffer)
    {
        RegistrationState state = new RegistrationState(referenceId, mode, readBuffer, writeBuffer);

        stateByReferenceId.put(state.referenceId(), state);

        switch (mode)
        {
        case ACCEPT:
            // TODO: prevent duplicate adds
            readAcceptBuffers = ArrayUtil.add(readAcceptBuffers, readBuffer);
            break;

        case CONNECT:
            // TODO: prevent duplicate adds
            readConnectBuffers = ArrayUtil.add(readConnectBuffers, readBuffer);
            break;
        }
    }

    public void doConnect(
        long referenceId,
        long streamId)
    {
        RegistrationState registrationState = stateByReferenceId.get(referenceId);
        if (registrationState == null)
        {
            throw new IllegalStateException("reference not found: " + referenceId);
        }

        StreamState newState = new StreamState(referenceId, streamId, registrationState.writeBuffer());
        stateByStreamId.put(newState.streamId(), newState);

        BeginFW begin = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                               .streamId(newState.streamId())
                               .referenceId(newState.referenceId())
                               .build();

        if (!newState.writeBuffer().write(begin.typeId(), begin.buffer(), begin.offset(), begin.remaining()))
        {
            throw new IllegalStateException("could not write to ring buffer");
        }
    }

    private void handleReadAccept(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case BEGIN_TYPE_ID:
            beginRO.wrap(buffer, index, index + length);

            RegistrationState registrationState = stateByReferenceId.get(beginRO.referenceId());
            if (registrationState == null)
            {
                throw new IllegalStateException("reference not found: " + beginRO.streamId());
            }

            StreamState newState = new StreamState(beginRO.referenceId(), beginRO.streamId(), registrationState.writeBuffer());
            stateByStreamId.put(newState.streamId(), newState);

            BeginFW begin = beginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                   .streamId(newState.streamId())
                                   .referenceId(newState.referenceId())
                                   .build();

            if (!newState.writeBuffer().write(begin.typeId(), begin.buffer(), begin.offset(), begin.remaining()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }
            break;

        case DATA_TYPE_ID:
            dataRO.wrap(buffer, index, index + length);

            StreamState state = stateByStreamId.get(dataRO.streamId());
            if (state == null)
            {
                throw new IllegalStateException("stream not found: " + dataRO.streamId());
            }

            if (!state.writeBuffer().write(dataRO.typeId(), dataRO.buffer(), dataRO.offset(), dataRO.remaining()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }
            break;

        case END_TYPE_ID:
            endRO.wrap(buffer, index, index + length);

            StreamState oldState = stateByStreamId.remove(endRO.streamId());
            if (oldState == null)
            {
                throw new IllegalStateException("stream not found: " + endRO.streamId());
            }

            EndFW end = endRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                             .streamId(oldState.streamId())
                             .build();

            if (!oldState.writeBuffer().write(end.typeId(), end.buffer(), end.offset(), end.remaining()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }
            break;
        }
    }


    private void handleReadConnect(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case BEGIN_TYPE_ID:
            beginRO.wrap(buffer, index, index + length);

            StreamState newState = stateByStreamId.get(beginRO.streamId());
            if (newState == null)
            {
                throw new IllegalStateException("stream not found: " + beginRO.streamId());
            }
            break;

        case DATA_TYPE_ID:
            dataRO.wrap(buffer, index, index + length);

            StreamState state = stateByStreamId.get(dataRO.streamId());
            if (state == null)
            {
                throw new IllegalStateException("stream not found: " + dataRO.streamId());
            }

            if (!state.writeBuffer().write(dataRO.typeId(), dataRO.buffer(), dataRO.offset(), dataRO.remaining()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }
            break;

        case END_TYPE_ID:
            endRO.wrap(buffer, index, index + length);

            StreamState oldState = stateByStreamId.remove(endRO.streamId());
            if (oldState == null)
            {
                throw new IllegalStateException("stream not found: " + endRO.streamId());
            }

            EndFW end = endRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                             .streamId(oldState.streamId())
                             .build();

            if (!oldState.writeBuffer().write(end.typeId(), end.buffer(), end.offset(), end.remaining()))
            {
                throw new IllegalStateException("could not write to ring buffer");
            }
            break;
        }
    }
}
