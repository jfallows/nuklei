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

import static uk.co.real_logic.agrona.BitUtil.align;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;
import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;

import java.io.File;
import java.nio.MappedByteBuffer;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastBufferDescriptor;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class ControlLayout extends Layout
{
    private static final int CONTROL_VERSION = 1;

    private static final int FIELD_OFFSET_VERSION = 0;
    private static final int FIELD_SIZE_VERSION = BitUtil.SIZE_OF_INT;

    private static final int FIELD_OFFSET_COMMAND_BUFFER_LENGTH = FIELD_OFFSET_VERSION + FIELD_SIZE_VERSION;
    private static final int FIELD_SIZE_COMMAND_BUFFER_LENGTH = BitUtil.SIZE_OF_INT;

    private static final int FIELD_OFFSET_RESPONSE_BUFFER_LENGTH =
            FIELD_OFFSET_COMMAND_BUFFER_LENGTH + FIELD_SIZE_COMMAND_BUFFER_LENGTH;
    private static final int FIELD_SIZE_RESPONSE_BUFFER_LENGTH = BitUtil.SIZE_OF_INT;

    private static final int FIELD_OFFSET_COUNTER_LABELS_BUFFER_LENGTH =
            FIELD_OFFSET_RESPONSE_BUFFER_LENGTH + FIELD_SIZE_RESPONSE_BUFFER_LENGTH;
    private static final int FIELD_SIZE_COUNTER_LABELS_BUFFER_LENGTH = BitUtil.SIZE_OF_INT;

    private static final int FIELD_OFFSET_COUNTER_VALUES_BUFFER_LENGTH =
            FIELD_OFFSET_COUNTER_LABELS_BUFFER_LENGTH + FIELD_SIZE_COUNTER_LABELS_BUFFER_LENGTH;
    private static final int FIELD_SIZE_COUNTER_VALUES_BUFFER_LENGTH = BitUtil.SIZE_OF_INT;

    private static final int END_OF_META_DATA_OFFSET = align(
            FIELD_OFFSET_COUNTER_VALUES_BUFFER_LENGTH + FIELD_SIZE_COUNTER_VALUES_BUFFER_LENGTH, BitUtil.CACHE_LINE_LENGTH);

    private final AtomicBuffer commandBuffer = new UnsafeBuffer(new byte[0]);
    private final AtomicBuffer responseBuffer = new UnsafeBuffer(new byte[0]);
    private final AtomicBuffer counterLabelsBuffer = new UnsafeBuffer(new byte[0]);
    private final AtomicBuffer counterValuesBuffer = new UnsafeBuffer(new byte[0]);


    public AtomicBuffer commandBuffer()
    {
        return commandBuffer;
    }

    public AtomicBuffer responseBuffer()
    {
        return responseBuffer;
    }

    public AtomicBuffer counterLabelsBuffer()
    {
        return counterLabelsBuffer;
    }

    public AtomicBuffer counterValuesBuffer()
    {
        return counterValuesBuffer;
    }

    @Override
    public void close()
    {
        unmap(commandBuffer.byteBuffer());
        unmap(responseBuffer.byteBuffer());
        unmap(counterLabelsBuffer.byteBuffer());
        unmap(counterValuesBuffer.byteBuffer());
    }

    public static final class Builder extends Layout.Builder<ControlLayout>
    {
        private final ControlLayout layout;

        private boolean readonly;
        private File controlFile;
        private int commandBufferCapacity;
        private int responseBufferCapacity;
        private int counterLabelsBufferCapacity;
        private int counterValuesBufferCapacity;

        private AtomicBuffer counterLabelsBuffer;
        private AtomicBuffer counterValuesBuffer;

        public Builder()
        {
            this.layout = new ControlLayout();
        }

        public Builder controlFile(File controlFile)
        {
            this.controlFile = controlFile;
            return this;
        }

        public File controlFile()
        {
            return controlFile;
        }

        public Builder commandBufferCapacity(int commandBufferCapacity)
        {
            this.commandBufferCapacity = commandBufferCapacity;
            return this;
        }

        public Builder responseBufferCapacity(int responseBufferCapacity)
        {
            this.responseBufferCapacity = responseBufferCapacity;
            return this;
        }

        public Builder counterLabelsBufferCapacity(int counterLabelsBufferCapacity)
        {
            this.counterLabelsBufferCapacity = counterLabelsBufferCapacity;
            return this;
        }

        public Builder counterValuesBufferCapacity(int counterValuesBufferCapacity)
        {
            this.counterValuesBufferCapacity = counterValuesBufferCapacity;
            return this;
        }

        public void counterLabelsBuffer(AtomicBuffer counterLabelsBuffer)
        {
            this.counterLabelsBuffer = counterLabelsBuffer;
        }

        public void counterValuesBuffer(AtomicBuffer counterValuesBuffer)
        {
            this.counterValuesBuffer = counterValuesBuffer;
        }

        public Builder readonly(boolean readonly)
        {
            this.readonly = readonly;
            return this;
        }

        @Override
        public ControlLayout build()
        {
            int commandBufferLength = commandBufferCapacity + RingBufferDescriptor.TRAILER_LENGTH;
            int responseBufferLength = responseBufferCapacity + BroadcastBufferDescriptor.TRAILER_LENGTH;
            int counterLabelsBufferLength = counterLabelsBufferCapacity;
            int counterValuesBufferLength = counterValuesBufferCapacity;

            if (!readonly)
            {
                createEmptyFile(controlFile, END_OF_META_DATA_OFFSET +
                        commandBufferLength + responseBufferLength + counterLabelsBufferLength + counterValuesBufferLength);

                MappedByteBuffer metadata = mapExistingFile(controlFile, "metadata", 0, END_OF_META_DATA_OFFSET);
                metadata.putInt(FIELD_OFFSET_VERSION, CONTROL_VERSION);
                metadata.putInt(FIELD_OFFSET_COMMAND_BUFFER_LENGTH, commandBufferCapacity);
                metadata.putInt(FIELD_OFFSET_RESPONSE_BUFFER_LENGTH, responseBufferCapacity);
                metadata.putInt(FIELD_OFFSET_COUNTER_LABELS_BUFFER_LENGTH, counterLabelsBufferCapacity);
                metadata.putInt(FIELD_OFFSET_COUNTER_VALUES_BUFFER_LENGTH, counterValuesBufferCapacity);
                unmap(metadata);
            }

            int commandBufferOffset = END_OF_META_DATA_OFFSET;
            layout.commandBuffer.wrap(mapExistingFile(controlFile, "commands", commandBufferOffset, commandBufferLength));

            int responseBufferOffset = commandBufferOffset + commandBufferLength;
            layout.responseBuffer.wrap(
                    mapExistingFile(controlFile, "responses", responseBufferOffset, responseBufferLength));

            int counterLabelsBufferOffset = responseBufferOffset + responseBufferLength;
            if (counterLabelsBuffer != null)
            {
                layout.counterLabelsBuffer.wrap(counterLabelsBuffer);
                counterLabelsBuffer = null;
            }
            else
            {
                layout.counterLabelsBuffer.wrap(
                        mapExistingFile(controlFile, "counterLabels", counterLabelsBufferOffset, counterLabelsBufferLength));
            }

            int counterValuesBufferOffset = counterLabelsBufferOffset + counterLabelsBufferLength;
            if (counterValuesBuffer != null)
            {
                layout.counterValuesBuffer.wrap(counterValuesBuffer);
                counterValuesBuffer = null;
            }
            else
            {
                layout.counterValuesBuffer.wrap(
                        mapExistingFile(controlFile, "counterValues", counterValuesBufferOffset, counterValuesBufferLength));
            }
            return layout;
        }
    }
}
