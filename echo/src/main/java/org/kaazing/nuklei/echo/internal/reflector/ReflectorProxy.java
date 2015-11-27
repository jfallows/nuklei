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

import java.util.logging.Logger;

import org.kaazing.nuklei.echo.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class ReflectorProxy
{
    private final Logger logger;
    private final OneToOneConcurrentArrayQueue<ReflectorCommand> commandQueue;

    public ReflectorProxy(Context context)
    {
        this.logger = context.logger();
        this.commandQueue = context.reflectorCommandQueue();
    }

    public void doCapture(
        long correlationId,
        String handler)
    {
        CaptureCommand command = new CaptureCommand(correlationId, handler);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }

    public void doUncapture(
        long correlationId,
        String handler)
    {
        UncaptureCommand command = new UncaptureCommand(correlationId, handler);
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
        String source,
        long sourceRef)
    {
        BindCommand command = new BindCommand(correlationId, source, sourceRef);
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
        long destinationRef)
    {
        PrepareCommand command = new PrepareCommand(correlationId, destination, destinationRef);
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

    public void doConnect(
        long correlationId,
        long referenceId)
    {
        ConnectCommand command = new ConnectCommand(correlationId, referenceId);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }
}
