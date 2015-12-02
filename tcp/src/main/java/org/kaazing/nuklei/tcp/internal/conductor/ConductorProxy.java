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
package org.kaazing.nuklei.tcp.internal.conductor;

import java.net.InetSocketAddress;

import org.kaazing.nuklei.tcp.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public abstract class ConductorProxy
{
    protected final OneToOneConcurrentArrayQueue<ConductorResponse> responseQueue;

    protected ConductorProxy(OneToOneConcurrentArrayQueue<ConductorResponse> responseQueue)
    {
        this.responseQueue = responseQueue;
    }

    public void onErrorResponse(long correlationId)
    {
        ErrorResponse response = new ErrorResponse(correlationId);
        if (!responseQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer response");
        }
    }

    public static class FromReader extends ConductorProxy
    {
        public FromReader(Context context)
        {
            super(context.readerResponseQueue());
        }

        public void onCapturedResponse(long correlationId)
        {
            CapturedResponse response = new CapturedResponse(correlationId);
            if (!responseQueue.offer(response))
            {
                throw new IllegalStateException("unable to offer response");
            }
        }

        public void onUncapturedResponse(long correlationId)
        {
            UncapturedResponse response = new UncapturedResponse(correlationId);
            if (!responseQueue.offer(response))
            {
                throw new IllegalStateException("unable to offer response");
            }
        }
    }

    public static class FromWriter extends ConductorProxy
    {
        public FromWriter(Context context)
        {
            super(context.writerResponseQueue());
        }

        public void onRoutedResponse(long correlationId)
        {
            RoutedResponse response = new RoutedResponse(correlationId);
            if (!responseQueue.offer(response))
            {
                throw new IllegalStateException("unable to offer response");
            }
        }

        public void onUnroutedResponse(long correlationId)
        {
            UnroutedResponse response = new UnroutedResponse(correlationId);
            if (!responseQueue.offer(response))
            {
                throw new IllegalStateException("unable to offer response");
            }
        }
    }

    public static class FromAcceptor extends ConductorProxy
    {
        public FromAcceptor(Context context)
        {
            super(context.acceptorResponseQueue());
        }

        public void onBoundResponse(
            long correlationId,
            long referenceId)
        {
            BoundResponse response = new BoundResponse(correlationId, referenceId);
            if (!responseQueue.offer(response))
            {
                throw new IllegalStateException("unable to offer response");
            }
        }

        public void onUnboundResponse(
            long correlationId,
            String destination,
            InetSocketAddress localAddress)
        {
            UnboundResponse response = new UnboundResponse(correlationId, destination, localAddress);
            if (!responseQueue.offer(response))
            {
                throw new IllegalStateException("unable to offer response");
            }
        }
    }

    public static class FromConnector extends ConductorProxy
    {
        public FromConnector(Context context)
        {
            super(context.connectorResponseQueue());
        }

        public void onPreparedResponse(
            long correlationId,
            long referenceId)
        {
            PreparedResponse response = new PreparedResponse(correlationId, referenceId);
            if (!responseQueue.offer(response))
            {
                throw new IllegalStateException("unable to offer response");
            }
        }

        public void onUnpreparedResponse(
            long correlationId,
            String handler,
            InetSocketAddress remoteAddress)
        {
            UnpreparedResponse response = new UnpreparedResponse(correlationId, handler, remoteAddress);
            if (!responseQueue.offer(response))
            {
                throw new IllegalStateException("unable to offer response");
            }
        }
    }
}
