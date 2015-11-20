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

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class StreamsLayout extends Layout
{
    private final AtomicBuffer inputBuffer = new UnsafeBuffer(new byte[0]);
    private final AtomicBuffer outputBuffer = new UnsafeBuffer(new byte[0]);

    public AtomicBuffer inputBuffer()
    {
        return inputBuffer;
    }

    public AtomicBuffer outputBuffer()
    {
        return outputBuffer;
    }

    @Override
    public void close()
    {
        unmap(inputBuffer.byteBuffer());
        unmap(outputBuffer.byteBuffer());
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
            int ringBufferLength = streamCapacity + RingBufferDescriptor.TRAILER_LENGTH;
            createEmptyFile(streamsFile, ringBufferLength << 1);
            layout.inputBuffer.wrap(mapExistingFile(streamsFile, "input", 0, ringBufferLength));
            layout.outputBuffer.wrap(mapExistingFile(streamsFile, "output", ringBufferLength, ringBufferLength));
            return layout;
        }
    }
}
