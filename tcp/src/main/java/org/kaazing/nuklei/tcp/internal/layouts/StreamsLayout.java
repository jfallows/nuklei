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
package org.kaazing.nuklei.tcp.internal.layouts;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class StreamsLayout extends Layout
{
    private final AtomicBuffer inputBuffer = new UnsafeBuffer(new byte[0]);
    private final AtomicBuffer outputBuffer = new UnsafeBuffer(new byte[0]);

    private int limit;

    public StreamsLayout wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.limit = actingLimit;

        checkLimit(limit(), actingLimit);

        int ringBufferLength = remaining() >> 1;

        // TODO: maintain alignment for Reader / Writer but without slice() to reset position to zero
        ByteBuffer byteBuffer = buffer().byteBuffer().duplicate();

        byteBuffer.limit(offset() + ringBufferLength);
        byteBuffer.position(offset());
        inputBuffer.wrap(byteBuffer.slice());

        byteBuffer.limit(offset() + ringBufferLength + ringBufferLength);
        byteBuffer.position(offset() + ringBufferLength);
        outputBuffer.wrap(byteBuffer.slice());

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
        return limit;
    }

    @Override
    public String toString()
    {
        return String.format("[streamCapacity=%d]", remaining() >> 1 - RingBufferDescriptor.TRAILER_LENGTH);
    }

    public static final class Builder extends Layout.Builder<StreamsLayout>
    {
        private final StreamsLayout layout;
        private int streamCapacity;
        private File streamsFile;

        public Builder()
        {
            this.layout = new StreamsLayout();
        }

        public Builder streamCapacity(int streamCapacity)
        {
            this.streamCapacity = streamCapacity;
            return this;
        }

        public Builder streamsFile(File streamsFile)
        {
            this.streamsFile = streamsFile;
            return this;
        }

        @Override
        public StreamsLayout mapNewFile()
        {
            int streamsSize = (streamCapacity + RingBufferDescriptor.TRAILER_LENGTH) << 1;
            MappedByteBuffer byteBuffer = IoUtil.mapNewFile(streamsFile, streamsSize);
            MutableDirectBuffer buffer = new UnsafeBuffer(byteBuffer);
            return layout.wrap(buffer, 0, buffer.capacity());
        }
    }
}
