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
package org.kaazing.nuklei.http.internal.readable;

import uk.co.real_logic.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class ReadableProxy
{
    private final String sourceName;
    private final ManyToOneConcurrentArrayQueue<ReadableCommand> commandQueue;

    ReadableProxy(
        String sourceName,
        ManyToOneConcurrentArrayQueue<ReadableCommand> commandQueue)
    {
        this.sourceName = sourceName;
        this.commandQueue = commandQueue;
    }

    public String name()
    {
        return sourceName;
    }

    public void doBind(
        long correlationId,
        long sourceRef,
        Object headers,
        ReadableProxy destination,
        RingBuffer sourceRoute,
        RingBuffer destinationRoute)
    {
        BindCommand command = new BindCommand(correlationId, sourceRef, headers, destination, sourceRoute, destinationRoute);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doUnbind(
        long correlationId,
        long referenceId)
    {
        UnbindCommand command = new UnbindCommand(correlationId, referenceId);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doPrepare(
        long correlationId,
        long sourceRef,
        Object headers,
        ReadableProxy destination,
        RingBuffer sourceRoute, RingBuffer destinationRoute)
    {
        PrepareCommand command =
                new PrepareCommand(correlationId, sourceRef, headers, destination, sourceRoute, destinationRoute);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doUnprepare(
        long correlationId,
        long referenceId)
    {
        UnprepareCommand command = new UnprepareCommand(correlationId, referenceId);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doRegisterEncoder(
        long destinationInitialStreamId,
        long sourceReplyStreamId,
        RingBuffer sourceRoute)
    {
        RegisterEncoderCommand command =
                new RegisterEncoderCommand(destinationInitialStreamId, sourceReplyStreamId, sourceRoute);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doRegisterDecoder(
        long destinationInitialStreamId,
        long sourceInitialStreamId,
        RingBuffer sourceRoute)
    {
        RegisterDecoderCommand command =
                new RegisterDecoderCommand(destinationInitialStreamId, sourceInitialStreamId, sourceRoute);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }
}
