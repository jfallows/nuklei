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
import java.nio.channels.ServerSocketChannel;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public class BindingInfo
{
    private final long reference;
    private final String source;
    private final long sourceBindingRef;
    private final String destination;
    private final InetSocketAddress address;
    private final RingBuffer readBuffer;
    private final RingBuffer writeBuffer;

    private ServerSocketChannel serverChannel;

    public BindingInfo(
        long reference,
        String source,
        long sourceBindingRef,
        String destination,
        InetSocketAddress address,
        RingBuffer readBuffer,
        RingBuffer writeBuffer)
    {
        this.reference = reference;
        this.source = source;
        this.sourceBindingRef = sourceBindingRef;
        this.destination = destination;
        this.address = address;
        this.readBuffer = readBuffer;
        this.writeBuffer = writeBuffer;
    }

    public long reference()
    {
        return this.reference;
    }

    public String source()
    {
        return source;
    }

    public long sourceBindingRef()
    {
        return sourceBindingRef;
    }

    public String destination()
    {
        return destination;
    }

    public InetSocketAddress address()
    {
        return address;
    }

    public RingBuffer readBuffer()
    {
        return readBuffer;
    }

    public RingBuffer writeBuffer()
    {
        return writeBuffer;
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
        return String.format("[reference=%d, source=\"%s\", sourceBindingRef=%d, destination=\"%s\", address=%s]",
                reference(), source(), sourceBindingRef(), destination(), address());
    }
}
