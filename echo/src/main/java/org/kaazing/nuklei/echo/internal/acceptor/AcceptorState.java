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
package org.kaazing.nuklei.echo.internal.acceptor;

import java.nio.channels.ServerSocketChannel;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public class AcceptorState
{
    private final long reference;
    private final String source;
    private final long sourceRef;
    private final RingBuffer inputBuffer;
    private final RingBuffer outputBuffer;

    private ServerSocketChannel serverChannel;

    public AcceptorState(
        long reference,
        String source,
        long sourceRef,
        RingBuffer inputBuffer,
        RingBuffer outputBuffer)
    {
        this.reference = reference;
        this.source = source;
        this.sourceRef = sourceRef;
        this.inputBuffer = inputBuffer;
        this.outputBuffer = outputBuffer;
    }

    public long reference()
    {
        return this.reference;
    }

    public String source()
    {
        return source;
    }

    public long sourceRef()
    {
        return sourceRef;
    }

    public RingBuffer inputBuffer()
    {
        return inputBuffer;
    }

    public RingBuffer outputBuffer()
    {
        return outputBuffer;
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
        return String.format("[reference=%d, source=\"%s\", sourceRef=%d]", reference, source, sourceRef);
    }
}
