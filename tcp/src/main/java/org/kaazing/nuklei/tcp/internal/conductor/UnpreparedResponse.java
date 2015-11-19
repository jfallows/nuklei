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

public final class UnpreparedResponse implements ConductorResponse
{
    private final long correlationId;
    private final String destination;
    private final long destinationRef;
    private final String source;
    private final InetSocketAddress remoteAddress;

    public UnpreparedResponse(
        long correlationId,
        String destination,
        long destinationRef,
        String source,
        InetSocketAddress remoteAddress)
    {
        this.correlationId = correlationId;
        this.destination = destination;
        this.destinationRef = destinationRef;
        this.source = source;
        this.remoteAddress = remoteAddress;
    }

    public void execute(Conductor conductor)
    {
        conductor.onUnpreparedResponse(correlationId, destination, destinationRef, source, remoteAddress);
    }
}
