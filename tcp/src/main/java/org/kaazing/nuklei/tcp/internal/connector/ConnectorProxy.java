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
package org.kaazing.nuklei.tcp.internal.connector;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import org.kaazing.nuklei.tcp.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public abstract class ConnectorProxy
{
    public static class FromConductor extends ConnectorProxy
    {
        private final Logger logger;
        private final OneToOneConcurrentArrayQueue<ConnectorCommand> commandQueue;

        public FromConductor(Context context)
        {
            this.logger = context.acceptorLogger();
            this.commandQueue = context.connectorCommandQueueFromConductor();
        }

        public void doPrepare(
            long correlationId,
            String handler,
            InetSocketAddress remoteAddress)
        {
            PrepareCommand command = new PrepareCommand(correlationId, handler, remoteAddress);
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
    }

    public static class FromReader extends ConnectorProxy
    {
        private final Logger logger;
        private final OneToOneConcurrentArrayQueue<ConnectorCommand> commandQueue;

        public FromReader(Context context)
        {
            this.logger = context.acceptorLogger();
            this.commandQueue = context.connectorCommandQueueFromReader();
        }

        public void doConnect(
            String handler,
            long handlerRef,
            long streamId)
        {
            ConnectCommand command = new ConnectCommand(handler, handlerRef, streamId);
            if (!commandQueue.offer(command))
            {
                throw new IllegalStateException("unable to offer command");
            }

            logger.finest(() -> { return command.toString(); });
        }
    }
}
