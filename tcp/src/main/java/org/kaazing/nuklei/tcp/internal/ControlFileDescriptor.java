/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal;

import static uk.co.real_logic.agrona.BitUtil.align;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class ControlFileDescriptor
{
    public static final String CONTROL_FILE = "control";

    public static final int CONTROL_VERSION = 1;

    public static final int CONTROL_VERSION_FIELD_OFFSET;
    public static final int META_DATA_OFFSET;

    /* Meta Data Offsets (offsets within the meta data section) */

    public static final int TO_NUKLEUS_BUFFER_LENGTH_FIELD_OFFSET;
    public static final int TO_CONTROLLER_BUFFER_LENGTH_FIELD_OFFSET;
    public static final int COUNTER_LABELS_BUFFER_LENGTH_FIELD_OFFSET;
    public static final int COUNTER_VALUES_BUFFER_LENGTH_FIELD_OFFSET;

    static
    {
        CONTROL_VERSION_FIELD_OFFSET = 0;
        META_DATA_OFFSET = CONTROL_VERSION_FIELD_OFFSET + BitUtil.SIZE_OF_INT;

        TO_NUKLEUS_BUFFER_LENGTH_FIELD_OFFSET = 0;
        TO_CONTROLLER_BUFFER_LENGTH_FIELD_OFFSET = TO_NUKLEUS_BUFFER_LENGTH_FIELD_OFFSET + BitUtil.SIZE_OF_INT;
        COUNTER_LABELS_BUFFER_LENGTH_FIELD_OFFSET = TO_CONTROLLER_BUFFER_LENGTH_FIELD_OFFSET + BitUtil.SIZE_OF_INT;
        COUNTER_VALUES_BUFFER_LENGTH_FIELD_OFFSET = COUNTER_LABELS_BUFFER_LENGTH_FIELD_OFFSET + BitUtil.SIZE_OF_INT;
    }

    public static final int META_DATA_LENGTH = COUNTER_VALUES_BUFFER_LENGTH_FIELD_OFFSET + BitUtil.SIZE_OF_INT;

    public static final int END_OF_META_DATA_OFFSET = align(BitUtil.SIZE_OF_INT + META_DATA_LENGTH, BitUtil.CACHE_LINE_LENGTH);

    /**
     * Compute the length of the control file and return it.
     *
     * @param totalLengthOfBuffers in bytes
     * @return control file length in bytes
     */
    public static int computeControlFileLength(final int totalLengthOfBuffers)
    {
        return END_OF_META_DATA_OFFSET + totalLengthOfBuffers;
    }

    public static int controlVersionOffset(final int baseOffset)
    {
        return baseOffset + CONTROL_VERSION_FIELD_OFFSET;
    }

    public static int commandBufferLengthOffset(final int baseOffset)
    {
        return baseOffset + META_DATA_OFFSET + TO_NUKLEUS_BUFFER_LENGTH_FIELD_OFFSET;
    }

    public static int responseBufferLengthOffset(final int baseOffset)
    {
        return baseOffset + META_DATA_OFFSET + TO_CONTROLLER_BUFFER_LENGTH_FIELD_OFFSET;
    }

    public static int counterLabelsBufferLengthOffset(final int baseOffset)
    {
        return baseOffset + META_DATA_OFFSET + COUNTER_LABELS_BUFFER_LENGTH_FIELD_OFFSET;
    }

    public static int counterValuesBufferLengthOffset(final int baseOffset)
    {
        return baseOffset + META_DATA_OFFSET + COUNTER_VALUES_BUFFER_LENGTH_FIELD_OFFSET;
    }

    public static UnsafeBuffer createMetaDataBuffer(MappedByteBuffer buffer)
    {
        return new UnsafeBuffer(buffer, 0, BitUtil.SIZE_OF_INT + META_DATA_LENGTH);
    }

    public static void fillMetaData(
        final UnsafeBuffer controlMetaDataBuffer,
        final int commandBufferLength,
        final int responseBufferLength,
        final int counterLabelsBufferLength,
        final int counterValuesBufferLength)
    {
        controlMetaDataBuffer.putInt(controlVersionOffset(0), ControlFileDescriptor.CONTROL_VERSION);
        controlMetaDataBuffer.putInt(commandBufferLengthOffset(0), commandBufferLength);
        controlMetaDataBuffer.putInt(responseBufferLengthOffset(0), responseBufferLength);
        controlMetaDataBuffer.putInt(counterLabelsBufferLengthOffset(0), counterLabelsBufferLength);
        controlMetaDataBuffer.putInt(counterValuesBufferLengthOffset(0), counterValuesBufferLength);
    }

    public static UnsafeBuffer createCommandBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer)
    {
        return new UnsafeBuffer(buffer, END_OF_META_DATA_OFFSET, metaDataBuffer.getInt(commandBufferLengthOffset(0)));
    }

    public static UnsafeBuffer createResponseBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer)
    {
        final int offset = END_OF_META_DATA_OFFSET + metaDataBuffer.getInt(commandBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, metaDataBuffer.getInt(responseBufferLengthOffset(0)));
    }

    public static UnsafeBuffer createCounterLabelsBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer)
    {
        final int offset = END_OF_META_DATA_OFFSET +
            metaDataBuffer.getInt(commandBufferLengthOffset(0)) +
            metaDataBuffer.getInt(responseBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, metaDataBuffer.getInt(counterLabelsBufferLengthOffset(0)));
    }

    public static UnsafeBuffer createCounterValuesBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer)
    {
        final int offset = END_OF_META_DATA_OFFSET +
            metaDataBuffer.getInt(commandBufferLengthOffset(0)) +
            metaDataBuffer.getInt(responseBufferLengthOffset(0)) +
            metaDataBuffer.getInt(counterLabelsBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, metaDataBuffer.getInt(counterValuesBufferLengthOffset(0)));
    }

    private ControlFileDescriptor()
    {
        // utility class, no instances
    }
}
