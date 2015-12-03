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
package org.kaazing.nuklei.specification.common.streams;

import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.util.Random;

import org.kaazing.k3po.lang.el.Function;
import org.kaazing.k3po.lang.el.spi.FunctionMapperSpi;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class Functions
{
    private static final Random RANDOM = new Random();

    private static final ThreadLocal<MutableDirectBuffer> BUFFER_REF =
        new ThreadLocal<MutableDirectBuffer>()
        {
            @Override
            protected MutableDirectBuffer initialValue()
            {
                return new UnsafeBuffer(new byte[0]);
            }
        };

    @Function
    public static byte[] newClientStreamId()
    {
        // odd, positive, non-zero
        long value = (RANDOM.nextLong() & 0x3fffffffffffffffL) | 0x0000000000000001L;

        final MutableDirectBuffer buffer = BUFFER_REF.get();
        byte[] bytes = new byte[8];
        buffer.wrap(bytes);
        buffer.putLong(0, value);
        return bytes;
    }

    @Function
    public static byte[] newServerStreamId()
    {
        // even, positive, non-zero
        long value;
        do
        {
            value = (RANDOM.nextLong() & 0x3ffffffffffffffeL);
        }
        while (value == 0L);

        final MutableDirectBuffer buffer = BUFFER_REF.get();
        byte[] bytes = new byte[8];
        buffer.wrap(bytes);
        buffer.putLong(0, value);
        return bytes;
    }

    @Function
    public static byte[] newReferenceId()
    {
        // positive
        long value = (RANDOM.nextLong() & 0x3fffffffffffffffL);

        final MutableDirectBuffer buffer = BUFFER_REF.get();
        byte[] bytes = new byte[8];
        buffer.wrap(bytes);
        buffer.putLong(0, value);
        return bytes;
    }

    @Function
    public static Layout map(String filename, int streamCapacity)
    {
        return new DeferredLayout(new File(filename), streamCapacity);
    }

    private abstract static class Layout implements AutoCloseable
    {
        public abstract AtomicBuffer getBuffer();
    }

    public static final class EagerLayout extends Layout
    {
        private final MappedByteBuffer byteBuffer;
        private final AtomicBuffer atomicBuffer;

        public EagerLayout(
            File location,
            int streamCapacity)
        {
            File absolute = location.getAbsoluteFile();
            int length = streamCapacity + RingBufferDescriptor.TRAILER_LENGTH;
            this.byteBuffer = mapExistingFile(absolute, location.getAbsolutePath());
            this.atomicBuffer = new UnsafeBuffer(byteBuffer, 0, length);
        }

        @Override
        public AtomicBuffer getBuffer()
        {
            return atomicBuffer;
        }

        @Override
        public void close()
        {
            unmap(byteBuffer);
        }
    }

    public static final class DeferredLayout extends Layout
    {
        private final File location;
        private final int streamCapacity;

        private EagerLayout delegate;

        public DeferredLayout(
            File location,
            int streamCapacity)
        {
            this.location = location;
            this.streamCapacity = streamCapacity;
        }

        @Override
        public AtomicBuffer getBuffer()
        {
            ensureInitialized();
            return delegate.atomicBuffer;
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
            return String.format("Layout [%s]", location);
        }

        void ensureInitialized()
        {
            if (delegate == null)
            {
                delegate = new EagerLayout(location, streamCapacity);
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
            return "streams";
        }
    }

    private Functions()
    {
        // utility
    }
}
