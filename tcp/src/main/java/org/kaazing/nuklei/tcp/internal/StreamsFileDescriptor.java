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
package org.kaazing.nuklei.tcp.internal;

import org.kaazing.nuklei.tcp.internal.types.Type;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class StreamsFileDescriptor extends Type<DirectBuffer>
{
    private final int bufferSize;
    private final AtomicBuffer inputBuffer;
    private final AtomicBuffer outputBuffer;

    public StreamsFileDescriptor(int ringBufferSize)
    {
        this.bufferSize = ringBufferSize;
        this.inputBuffer = new UnsafeBuffer(new byte[0]);
        this.outputBuffer = new UnsafeBuffer(new byte[0]);
    }

    public int length()
    {
        return bufferSize << 1;
    }

    public StreamsFileDescriptor wrap(DirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset);
        inputBuffer.wrap(buffer, offset, bufferSize);
        outputBuffer.wrap(buffer, offset + bufferSize, bufferSize);
        return this;
    }

    public AtomicBuffer inputBuffer()
    {
        return inputBuffer;
    }

    public AtomicBuffer outputBuffer()
    {
        return outputBuffer;
    }

    @Override
    public int limit()
    {
        return offset() + length();
    }
}
