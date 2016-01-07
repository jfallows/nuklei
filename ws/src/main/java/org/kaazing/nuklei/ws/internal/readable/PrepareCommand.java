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

import static java.lang.String.format;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class PrepareCommand implements ReadableCommand
{
    private final long correlationId;
    private final long destinationRef;
    private final ReadableProxy destinationProxy;
    private final String protocol;
    private final RingBuffer sourceRoute;
    private final RingBuffer destinationRoute;

    public PrepareCommand(
        long correlationId,
        long destinationRef,
        String protocol,
        ReadableProxy destinationProxy,
        RingBuffer sourceRoute,
        RingBuffer destinationRoute)
    {
        this.correlationId = correlationId;
        this.destinationRef = destinationRef;
        this.protocol = protocol;
        this.destinationProxy = destinationProxy;
        this.sourceRoute = sourceRoute;
        this.destinationRoute = destinationRoute;
    }

    @Override
    public void execute(Readable source)
    {
        source.doPrepare(correlationId, destinationRef, protocol, destinationProxy, sourceRoute, destinationRoute);
    }

    @Override
    public String toString()
    {
        return format("PREPARE [correlationId=%d, destinationRef=%d, protocol=%s, destinationProxy=\"%s\"]",
                correlationId, destinationRef, protocol, destinationProxy);
    }
}
