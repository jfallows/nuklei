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
package org.kaazing.specification.nuklei.common;

import static org.agrona.IoUtil.mapExistingFile;
import static org.agrona.IoUtil.mapNewFile;
import static org.agrona.IoUtil.unmap;

import java.io.File;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.Random;

import org.kaazing.k3po.lang.el.Function;
import org.kaazing.k3po.lang.el.spi.FunctionMapperSpi;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.broadcast.BroadcastBufferDescriptor;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class Functions
{
    private static final Random RANDOM = new Random();

    // TODO: compute with alignment
    private static final int META_DATA_LENGTH = 64;

    @Function
    public static Helper directory(
        String configPath)
    {
        return new Helper(new File(configPath));
    }

    @Function
    public static byte[] newReferenceId()
    {
        return longToBytesNative(RANDOM.nextLong());
    }

    @Function
    public static byte[] newStreamId()
    {
        return longToBytesNative(RANDOM.nextLong());
    }

    @Function
    public static byte[] newCorrelationId()
    {
        return longToBytesNative(RANDOM.nextLong());
    }

    public static final class Helper
    {
        private final File configDirectory;

        private int streamCapacity;
        private int throttleCapacity;

        private int ringCapacity;
        private int broadcastCapacity;

        private Helper(
            File configDirectory)
        {
            this.configDirectory = configDirectory;
        }

        public Helper streamsCapacity(
            int streamCapacity,
            int throttleCapacity)
        {
            this.streamCapacity = streamCapacity;
            this.throttleCapacity = throttleCapacity;
            return this;
        }

        public Helper controlCapacity(
            int ringCapacity,
            int broadcastCapacity)
        {
            this.ringCapacity = ringCapacity;
            this.broadcastCapacity = broadcastCapacity;
            return this;
        }

        public StreamsHelper streams(
            String nukleus,
            String source)
        {
            String relativePath = String.format("%s/streams/%s", nukleus, source);
            return new StreamsHelper.Deferred(new File(configDirectory, relativePath), streamCapacity, throttleCapacity);
        }

        public ControlHelper controlNew(
            String nukleus)
        {
            String relativePath = String.format("%s/control", nukleus);
            return new ControlHelper.Eager(true, new File(configDirectory, relativePath), ringCapacity, broadcastCapacity);
        }

        public ControlHelper control(
            String nukleus)
        {
            String relativePath = String.format("%s/control", nukleus);
            return new ControlHelper.Deferred(false, new File(configDirectory, relativePath), ringCapacity, broadcastCapacity);
        }

        public abstract static class StreamsHelper implements AutoCloseable
        {
            public abstract AtomicBuffer getBuffer();

            public abstract AtomicBuffer getThrottle();

            private static final class Eager extends StreamsHelper
            {
                private final MappedByteBuffer buffer;
                private final AtomicBuffer streamsBuffer;
                private final AtomicBuffer throttleBuffer;

                private Eager(
                    File location,
                    int streamCapacity,
                    int throttleCapacity)
                {
                    File absolute = location.getAbsoluteFile();

                    int streamBufferSize = streamCapacity + RingBufferDescriptor.TRAILER_LENGTH;
                    int throttleBufferSize = throttleCapacity + RingBufferDescriptor.TRAILER_LENGTH;

                    this.buffer = mapExistingFile(absolute, "streams");
                    this.streamsBuffer = new UnsafeBuffer(buffer, 0, streamBufferSize);
                    this.throttleBuffer = new UnsafeBuffer(buffer, streamBufferSize, throttleBufferSize);
                }

                @Override
                public AtomicBuffer getBuffer()
                {
                    return streamsBuffer;
                }

                @Override
                public AtomicBuffer getThrottle()
                {
                    return throttleBuffer;
                }

                @Override
                public void close()
                {
                    unmap(buffer);
                }

                @Override
                public String toString()
                {
                    return String.format("streamsCapacity(%d, %d)", streamsBuffer.capacity(), throttleBuffer.capacity());
                }
            }

            private static final class Deferred extends StreamsHelper
            {
                private final File location;
                private final int streamsCapacity;
                private final int throttleCapacity;

                private Eager delegate;

                private Deferred(
                    File location,
                    int streamsCapacity,
                    int throttleCapacity)
                {
                    this.location = location;
                    this.streamsCapacity = streamsCapacity;
                    this.throttleCapacity = throttleCapacity;
                }

                @Override
                public AtomicBuffer getBuffer()
                {
                    ensureInitialized();
                    return delegate.streamsBuffer;
                }

                @Override
                public AtomicBuffer getThrottle()
                {
                    ensureInitialized();
                    return delegate.throttleBuffer;
                }

                @Override
                public void close() throws Exception
                {
                    if (delegate != null)
                    {
                        delegate.close();
                    }
                }

                @Override
                public String toString()
                {
                    return String.format("streamsCapacity(%d, %d)", streamsCapacity, throttleCapacity);
                }

                void ensureInitialized()
                {
                    if (delegate == null)
                    {
                        delegate = new Eager(location, streamsCapacity, throttleCapacity);
                    }
                }
            }
        }

        public abstract static class ControlHelper implements AutoCloseable
        {
            private long correlationId;

            public abstract AtomicBuffer getNukleus();

            public abstract AtomicBuffer getController();

            public final long nextCorrelationId()
            {
                RingBuffer ring = new ManyToOneRingBuffer(getNukleus());
                correlationId = ring.nextCorrelationId();
                return correlationId;
            }

            public final long correlationId()
            {
                return correlationId;
            }

            private static final class Eager extends ControlHelper
            {
                private final MappedByteBuffer buffer;
                private final AtomicBuffer nukleus;
                private final AtomicBuffer controller;

                private Eager(
                    boolean overwrite,
                    File location,
                    int ringCapacity,
                    int broadcastCapacity)
                {
                    File absolute = location.getAbsoluteFile();
                    int metaLength = META_DATA_LENGTH;
                    int ringLength = ringCapacity + RingBufferDescriptor.TRAILER_LENGTH;
                    int broadcastLength = broadcastCapacity + BroadcastBufferDescriptor.TRAILER_LENGTH;
                    this.buffer = overwrite
                            ? mapNewFile(absolute, metaLength + ringLength + broadcastLength)
                                    : mapExistingFile(absolute, location.getAbsolutePath());
                    this.nukleus = new UnsafeBuffer(buffer, metaLength, ringLength);
                    this.controller = new UnsafeBuffer(buffer, metaLength + ringLength, broadcastLength);
                }

                @Override
                public AtomicBuffer getNukleus()
                {
                    return nukleus;
                }

                @Override
                public AtomicBuffer getController()
                {
                    return controller;
                }

                @Override
                public void close()
                {
                    unmap(buffer);
                }

                @Override
                public String toString()
                {
                    return String.format("controlCapacity(%d, %d)", nukleus.capacity(), controller.capacity());
                }
            }

            private static final class Deferred extends ControlHelper
            {
                private final boolean overwrite;
                private final File location;
                private final int ringCapacity;
                private final int broadcastCapacity;

                private Eager delegate;

                private Deferred(
                    boolean overwrite,
                    File location,
                    int ringCapacity,
                    int broadcastCapacity)
                {
                    this.overwrite = overwrite;
                    this.location = location;
                    this.ringCapacity = ringCapacity;
                    this.broadcastCapacity = broadcastCapacity;
                }

                @Override
                public AtomicBuffer getNukleus()
                {
                    ensureInitialized();
                    return delegate.nukleus;
                }

                @Override
                public AtomicBuffer getController()
                {
                    ensureInitialized();
                    return delegate.controller;
                }

                @Override
                public void close() throws Exception
                {
                    if (delegate != null)
                    {
                        delegate.close();
                    }
                }

                @Override
                public String toString()
                {
                    return String.format("controlCapacity(%d, %d)", ringCapacity, broadcastCapacity);
                }

                void ensureInitialized()
                {
                    if (delegate == null)
                    {
                        delegate = new Eager(overwrite, location, ringCapacity, broadcastCapacity);
                    }
                }
            }
        }
    }

    public static class Mapper extends FunctionMapperSpi.Reflective
    {
        public Mapper()
        {
            super(Functions.class);
        }

        @Override
        public String getPrefixName()
        {
            return "nuklei";
        }
    }

    private static byte[] longToBytesNative(long value)
    {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
        {
            return new byte[] {
                    (byte) (value >> 56),
                    (byte) (value >> 48),
                    (byte) (value >> 40),
                    (byte) (value >> 32),
                    (byte) (value >> 24),
                    (byte) (value >> 16),
                    (byte) (value >> 8),
                    (byte) value
            };
        }
        else
        {
            return new byte[] {
                    (byte) value,
                    (byte) (value >> 8),
                    (byte) (value >> 16),
                    (byte) (value >> 24),
                    (byte) (value >> 32),
                    (byte) (value >> 40),
                    (byte) (value >> 48),
                    (byte) (value >> 56)
            };
        }
    }

    private Functions()
    {
        // utility
    }
}
