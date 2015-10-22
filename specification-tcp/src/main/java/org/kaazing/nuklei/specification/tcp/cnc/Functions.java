/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.nuklei.specification.tcp.cnc;

import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;
import static uk.co.real_logic.agrona.IoUtil.mapNewFile;

import java.io.File;
import java.nio.MappedByteBuffer;

import org.kaazing.k3po.lang.el.Function;
import org.kaazing.k3po.lang.el.spi.FunctionMapperSpi;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastBufferDescriptor;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class Functions
{

    @Function
    public static Layout mapNew(String filename, int ringCapacity, int broadcastCapacity)
    {
        return map(filename, ringCapacity, broadcastCapacity, true);
    }

    @Function
    public static Layout map(String filename, int ringCapacity, int broadcastCapacity)
    {
        return map(filename, ringCapacity, broadcastCapacity, false);
    }

    private static Layout map(String filename, int ringCapacity, int broadcastCapacity, boolean create)
    {
        File location = new File(filename);
        int totalRingLength = ringCapacity + RingBufferDescriptor.TRAILER_LENGTH;
        int totalBroadcastLength = broadcastCapacity + BroadcastBufferDescriptor.TRAILER_LENGTH;
        MappedByteBuffer buffer = create ? mapNewFile(location, totalRingLength + totalBroadcastLength)
                                         : mapExistingFile(location, filename);
        AtomicBuffer ring = new UnsafeBuffer(buffer, 0, totalRingLength);
        AtomicBuffer broadcast = new UnsafeBuffer(buffer, totalRingLength, totalBroadcastLength);
        return new Layout(ring, broadcast);
    }

    public static final class Layout
    {
        private final AtomicBuffer nukleus;
        private final AtomicBuffer controller;

        public Layout(AtomicBuffer ring, AtomicBuffer broadcast)
        {
            this.nukleus = ring;
            this.controller = broadcast;
        }

        public AtomicBuffer getNukleus()
        {
            return nukleus;
        }

        public AtomicBuffer getController()
        {
            return controller;
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
            return "cnc";
        }
    }

    private Functions()
    {
        // utility
    }
}
