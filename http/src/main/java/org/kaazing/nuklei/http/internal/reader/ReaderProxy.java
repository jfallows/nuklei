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
package org.kaazing.nuklei.http.internal.reader;

import java.util.Map;
import java.util.logging.Logger;

import org.kaazing.nuklei.http.internal.Context;

import uk.co.real_logic.agrona.concurrent.ManyToOneConcurrentArrayQueue;

public final class ReaderProxy
{
    private final Logger logger;
    private final ManyToOneConcurrentArrayQueue<ReaderCommand> commandQueue;

    public ReaderProxy(Context context)
    {
        this.logger = context.logger();
        this.commandQueue = context.readerCommandQueue();
    }

    public void doCapture(
        long correlationId,
        String source)
    {
        CaptureCommand command = new CaptureCommand(correlationId, source);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }

    public void doUncapture(
        long correlationId,
        String source)
    {
        UncaptureCommand command = new UncaptureCommand(correlationId, source);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }

    public void doRoute(
        long correlationId,
        String destination)
    {
        RouteCommand command = new RouteCommand(correlationId, destination);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }

    public void doUnroute(
        long correlationId,
        String destination)
    {
        UnrouteCommand command = new UnrouteCommand(correlationId, destination);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }

    public void doBind(
        long correlationId,
        String destination,
        long destinationRef,
        String source,
        Map<String, String> headers)
    {
        BindCommand command = new BindCommand(correlationId, destination, destinationRef, source, headers);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
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

        logger.finest(() -> { return command.toString(); });
    }

    public void doPrepare(
        long correlationId,
        String destination,
        long destinationRef,
        String source,
        Map<String, String> headers)
    {
        PrepareCommand command = new PrepareCommand(correlationId, destination, destinationRef, source, headers);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
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

        logger.finest(() -> { return command.toString(); });
    }

    public void onBoundResponse(
        String source,
        long correlationId,
        long referenceId)
    {
        BoundResponse command = new BoundResponse(source, correlationId, referenceId);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }

    public void onPreparedResponse(
        String source,
        long correlationId,
        long referenceId)
    {
        PreparedResponse command = new PreparedResponse(source, correlationId, referenceId);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }
}
