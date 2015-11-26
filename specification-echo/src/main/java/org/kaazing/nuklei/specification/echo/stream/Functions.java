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
package org.kaazing.nuklei.specification.echo.stream;

import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.util.Random;

import org.kaazing.k3po.lang.el.Function;
import org.kaazing.k3po.lang.el.spi.FunctionMapperSpi;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class Functions
{
    private static final Random RANDOM = new Random();

    @Function
    public static byte[] randomBytes(int length)
    {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++)
        {
            bytes[i] = (byte) RANDOM.nextInt(0x100);
        }
        return bytes;
    }

    @Function
    public static Layout map(String filename, int streamCapacity)
    {
        return new DeferredLayout(new File(filename), streamCapacity);
    }

    private abstract static class Layout implements AutoCloseable
    {
        public abstract AtomicBuffer getInput();

        public abstract AtomicBuffer getOutput();
    }

    public static final class EagerLayout extends Layout
    {
        private final MappedByteBuffer buffer;
        private final AtomicBuffer input;
        private final AtomicBuffer output;

        public EagerLayout(
                File location,
                int streamCapacity)
        {
            File absolute = location.getAbsoluteFile();
            int sourceLength = streamCapacity + RingBufferDescriptor.TRAILER_LENGTH;
            int destinationLength = streamCapacity + RingBufferDescriptor.TRAILER_LENGTH;
            this.buffer = mapExistingFile(absolute, location.getAbsolutePath());
            this.input = new UnsafeBuffer(buffer, 0, sourceLength);
            this.output = new UnsafeBuffer(buffer, sourceLength, destinationLength);
        }

        @Override
        public AtomicBuffer getInput()
        {
            return input;
        }

        @Override
        public AtomicBuffer getOutput()
        {
            return output;
        }

        @Override
        public void close()
        {
            unmap(buffer);
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
        public AtomicBuffer getInput()
        {
            ensureInitialized();
            return delegate.input;
        }

        @Override
        public AtomicBuffer getOutput()
        {
            ensureInitialized();
            return delegate.output;
        }

        @Override
        public void close() throws Exception
        {
            if (delegate != null)
            {
                delegate.close();
            }
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
