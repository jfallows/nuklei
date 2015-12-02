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

import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;
import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;

import java.io.File;
import java.nio.MappedByteBuffer;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class StreamsLayout extends Layout
{
    private final RingBuffer buffer;

    private StreamsLayout(
        RingBuffer buffer)
    {
        this.buffer = buffer;
    }

    public RingBuffer buffer()
    {
        return buffer;
    }

    @Override
    public void close()
    {
        unmap(buffer.buffer().byteBuffer());
    }

    public static final class Builder extends Layout.Builder<StreamsLayout>
    {
        private int streamsCapacity;
        private File streamsFile;
        private boolean createFile;

        public Builder streamsCapacity(int streamsCapacity)
        {
            this.streamsCapacity = streamsCapacity;
            return this;
        }

        public Builder streamsFile(File streamsFile)
        {
            this.streamsFile = streamsFile;
            return this;
        }

        public Builder createFile(boolean createFile)
        {
            this.createFile = createFile;
            return this;
        }

        @Override
        public StreamsLayout build()
        {
            int streamsFileLength = streamsCapacity + RingBufferDescriptor.TRAILER_LENGTH;

            if (createFile)
            {
                createEmptyFile(streamsFile, streamsFileLength);
            }

            MappedByteBuffer byteBuffer = mapExistingFile(streamsFile, "streams");
            AtomicBuffer atomicBuffer= new UnsafeBuffer(byteBuffer);

            return new StreamsLayout(new ManyToOneRingBuffer(atomicBuffer));
        }
    }
}
