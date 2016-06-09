/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.ws.internal.layouts;

import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;
import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.file.Path;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class StreamsLayout extends Layout
{
    private final RingBuffer streamsBuffer;
    private final RingBuffer throttleBuffer;

    private StreamsLayout(
        RingBuffer streamsBuffer,
        RingBuffer throttleBuffer)
    {
        this.streamsBuffer = streamsBuffer;
        this.throttleBuffer = throttleBuffer;
    }

    public RingBuffer streamsBuffer()
    {
        return streamsBuffer;
    }

    public RingBuffer throttleBuffer()
    {
        return throttleBuffer;
    }

    @Override
    public void close()
    {
        unmap(streamsBuffer.buffer().byteBuffer());
        unmap(throttleBuffer.buffer().byteBuffer());
    }

    public static final class Builder extends Layout.Builder<StreamsLayout>
    {
        private long streamsCapacity;
        private long throttleCapacity;
        private Path path;
        private boolean readonly;

        public Builder streamsCapacity(
            long streamsCapacity)
        {
            this.streamsCapacity = streamsCapacity;
            return this;
        }

        public Builder throttleCapacity(
            long throttleCapacity)
        {
            this.throttleCapacity = throttleCapacity;
            return this;
        }

        public Builder path(
            Path path)
        {
            this.path = path;
            return this;
        }

        public Builder readonly(
            boolean readonly)
        {
            this.readonly = readonly;
            return this;
        }

        @Override
        public StreamsLayout build()
        {
            final File streams = path.toFile();
            final long streamsSize = streamsCapacity + RingBufferDescriptor.TRAILER_LENGTH;
            final long throttleSize = throttleCapacity + RingBufferDescriptor.TRAILER_LENGTH;

            if (!readonly)
            {
                createEmptyFile(streams, streamsSize + throttleSize);
            }

            final MappedByteBuffer mappedStreams = mapExistingFile(streams, "streams", 0, streamsSize);
            final MappedByteBuffer mappedThrottle = mapExistingFile(streams, "throttle", streamsSize, throttleSize);

            final AtomicBuffer atomicStreams = new UnsafeBuffer(mappedStreams);
            final AtomicBuffer atomicThrottle = new UnsafeBuffer(mappedThrottle);

            // TODO: use OneToOneRingBuffer instead (now single writer / single reader)
            return new StreamsLayout(new ManyToOneRingBuffer(atomicStreams), new ManyToOneRingBuffer(atomicThrottle));
        }
    }
}
