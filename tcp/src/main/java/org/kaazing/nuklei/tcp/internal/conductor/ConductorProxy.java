/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.conductor;

import java.net.InetSocketAddress;

import org.kaazing.nuklei.tcp.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class ConductorProxy
{
    private final OneToOneConcurrentArrayQueue<ConductorResponse> responseQueue;

    public ConductorProxy(Context context)
    {
        this.responseQueue = context.acceptorResponseQueue();
    }

    public void onErrorResponse(long correlationId)
    {
        ErrorResponse response = new ErrorResponse(correlationId);
        if (!responseQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer response");
        }
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
        String source,
        long sourceRef,
        String destination,
        InetSocketAddress localAddress)
    {
        UnboundResponse response = new UnboundResponse(correlationId, source, sourceRef, destination, localAddress);
        if (!responseQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer response");
        }
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
        String destination,
        long destinationRef,
        String source,
        InetSocketAddress remoteAddress)
    {
        UnpreparedResponse response = new UnpreparedResponse(correlationId, destination, destinationRef, source, remoteAddress);
        if (!responseQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer response");
        }
    }

    public void onConnectedResponse(
        long correlationId,
        long connectionId)
    {
        ConnectedResponse response = new ConnectedResponse(correlationId, connectionId);
        if (!responseQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer response");
        }
    }
}
