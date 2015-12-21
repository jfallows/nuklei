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

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class AcceptorState
{
    private final String destination;
    private final long destinationRef;
    private final InetSocketAddress localAddress;

    private ServerSocketChannel serverChannel;

    public AcceptorState(
        String destination,
        long destinationRef,
        InetSocketAddress localAddress)
    {
        this.destinationRef = destinationRef;
        this.destination = destination;
        this.localAddress = localAddress;
    }

    public String destination()
    {
        return destination;
    }

    public long destinationRef()
    {
        return this.destinationRef;
    }

    public InetSocketAddress localAddress()
    {
        return localAddress;
    }

    public void attach(ServerSocketChannel attachment)
    {
        this.serverChannel = attachment;
    }

    public ServerSocketChannel channel()
    {
        return serverChannel;
    }

    @Override
    public String toString()
    {
        return String.format("[destination=\"%s\", destinationRef=%d, localAddress=%s]",
                destination, destinationRef, localAddress);
    }
}
