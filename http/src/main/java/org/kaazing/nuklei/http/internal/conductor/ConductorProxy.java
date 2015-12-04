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
package org.kaazing.nuklei.http.internal.conductor;

import org.kaazing.nuklei.http.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class ConductorProxy
{
    private final OneToOneConcurrentArrayQueue<ConductorResponse> responseQueue;

    public ConductorProxy(Context context)
    {
        this.responseQueue = context.translatorResponseQueue();
    }

    public void onErrorResponse(long correlationId)
    {
        ErrorResponse response = new ErrorResponse(correlationId);
        if (!responseQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer response");
        }
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
        String handler,
        Object headers)
    {
        UnboundResponse response = new UnboundResponse(correlationId, source, sourceRef, handler, headers);
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
        String handler,
        Object headers)
    {
        UnpreparedResponse response = new UnpreparedResponse(correlationId, destination, destinationRef, handler, headers);
        if (!responseQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer response");
        }
    }
}
