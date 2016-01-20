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
package org.kaazing.nuklei.ws.internal.readable;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public class ReadableState
{
    private final long sourceRef;
    private final ReadableProxy destination;
    private final long destinationRef;
    private final String protocol;
    private final RingBuffer destinationRoute;
    private final RingBuffer sourceRoute;

    public ReadableState(
        long sourceRef,
        ReadableProxy destination,
        long destinationRef,
        String protocol,
        RingBuffer sourceRoute,
        RingBuffer destinationRoute)
    {
        this.sourceRef = sourceRef;
        this.destination = destination;
        this.destinationRef = destinationRef;
        this.protocol = protocol;
        this.destinationRoute = destinationRoute;
        this.sourceRoute = sourceRoute;
    }

    public long sourceRef()
    {
        return this.sourceRef;
    }

    public ReadableProxy destination()
    {
        return destination;
    }

    public long destinationRef()
    {
        return this.destinationRef;
    }

    public String protocol()
    {
        return protocol;
    }

    public RingBuffer sourceRoute()
    {
        return sourceRoute;
    }

    public RingBuffer destinationRoute()
    {
        return destinationRoute;
    }

    @Override
    public String toString()
    {
        return String.format("[sourceRef=%d, destination=\"%s\", destinationRef=%d, protocol=%s]",
                sourceRef, destination, destinationRef, protocol);
    }
}
