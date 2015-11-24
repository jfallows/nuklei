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
package org.kaazing.nuklei.echo.internal.acceptor;

import java.util.logging.Logger;

import org.kaazing.nuklei.echo.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class AcceptorProxy
{
    private final Logger logger;
    private final OneToOneConcurrentArrayQueue<AcceptorCommand> commandQueue;

    public AcceptorProxy(Context context)
    {
        this.logger = context.acceptorLogger();
        this.commandQueue = context.acceptorCommandQueue();
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
        long reference)
    {
        UnbindCommand command = new UnbindCommand(correlationId, reference);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }

        logger.finest(() -> { return command.toString(); });
    }

}
