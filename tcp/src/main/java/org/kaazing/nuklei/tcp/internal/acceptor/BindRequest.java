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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.acceptor;

import java.net.InetSocketAddress;

public final class BindRequest implements AcceptorCommand
{
    private final long correlationId;
    private final String source;
    private final long sourceBindingRef;
    private final String destination;
    private final InetSocketAddress address;

    public BindRequest(
        long correlationId,
        String source,
        long sourceBindingRef,
        String destination,
        InetSocketAddress address)
    {
        this.correlationId = correlationId;
        this.source = source;
        this.sourceBindingRef = sourceBindingRef;
        this.destination = destination;
        this.address = address;
    }

    public void execute(Acceptor acceptor)
    {
        acceptor.onBindRequest(correlationId, source, sourceBindingRef, destination, address);
    }
}
