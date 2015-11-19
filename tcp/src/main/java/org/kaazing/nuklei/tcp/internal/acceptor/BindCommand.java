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
package org.kaazing.nuklei.tcp.internal.acceptor;

import static java.lang.String.format;

import java.net.InetSocketAddress;

public final class BindCommand implements AcceptorCommand
{
    private final long correlationId;
    private final String source;
    private final long sourceRef;
    private final String destination;
    private final InetSocketAddress localAddress;

    public BindCommand(
        long correlationId,
        String source,
        long sourceBindingRef,
        String destination,
        InetSocketAddress localAddress)
    {
        this.correlationId = correlationId;
        this.source = source;
        this.sourceRef = sourceBindingRef;
        this.destination = destination;
        this.localAddress = localAddress;
    }

    @Override
    public void execute(Acceptor acceptor)
    {
        acceptor.doBind(correlationId, source, sourceRef, destination, localAddress);
    }

    @Override
    public String toString()
    {
        return format("BIND [correlationId=%d, source=\"%s\", sourceRef=%d, destination=\"%s\", localAddress=%s]",
                correlationId, source, sourceRef, destination, localAddress);
    }
}
