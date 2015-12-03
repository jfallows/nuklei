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
package org.kaazing.nuklei.specification.common.control;

import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;
import static uk.co.real_logic.agrona.IoUtil.mapNewFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;

import java.io.File;
import java.nio.MappedByteBuffer;

import org.kaazing.k3po.lang.el.Function;
import org.kaazing.k3po.lang.el.spi.FunctionMapperSpi;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastBufferDescriptor;
import uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class Functions
{

    // TODO: compute with alignment
    private static final int META_DATA_LENGTH = 64;

    @Function
    public static Layout mapNew(String filename, int ringCapacity, int broadcastCapacity)
    {
        return new EagerLayout(true, new File(filename), ringCapacity, broadcastCapacity);
    }

    @Function
    public static Layout map(String filename, int ringCapacity, int broadcastCapacity)
    {
        return new DeferredLayout(false, new File(filename), ringCapacity, broadcastCapacity);
    }

    private abstract static class Layout implements AutoCloseable
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
    }

    public static final class EagerLayout extends Layout
    {
        private final MappedByteBuffer buffer;
        private final AtomicBuffer nukleus;
        private final AtomicBuffer controller;

        public EagerLayout(
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
    }

    public static final class DeferredLayout extends Layout
    {
        private final boolean overwrite;
        private final File location;
        private final int ringCapacity;
        private final int broadcastCapacity;

        private EagerLayout delegate;

        public DeferredLayout(
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

        void ensureInitialized()
        {
            if (delegate == null)
            {
                delegate = new EagerLayout(overwrite, location, ringCapacity, broadcastCapacity);
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
            return "control";
        }
    }

    private Functions()
    {
        // utility
    }
}
