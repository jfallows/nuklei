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
package org.kaazing.nuklei.reaktor.internal;

import static uk.co.real_logic.agrona.BitUtil.align;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class CncFileDescriptor
{
    public static final String CNC_FILE = "cnc";

    public static final int CNC_VERSION = 1;

    public static final int CNC_VERSION_FIELD_OFFSET;
    public static final int META_DATA_OFFSET;

    /* Meta Data Offsets (offsets within the meta data section) */

    public static final int TO_CONDUCTOR_BUFFER_LENGTH_FIELD_OFFSET;
    public static final int FROM_CONDUCTOR_BUFFER_LENGTH_FIELD_OFFSET;
    public static final int COUNTER_LABELS_BUFFER_LENGTH_FIELD_OFFSET;
    public static final int COUNTER_VALUES_BUFFER_LENGTH_FIELD_OFFSET;

    static
    {
        CNC_VERSION_FIELD_OFFSET = 0;
        META_DATA_OFFSET = CNC_VERSION_FIELD_OFFSET + BitUtil.SIZE_OF_INT;

        TO_CONDUCTOR_BUFFER_LENGTH_FIELD_OFFSET = 0;
        FROM_CONDUCTOR_BUFFER_LENGTH_FIELD_OFFSET = TO_CONDUCTOR_BUFFER_LENGTH_FIELD_OFFSET + BitUtil.SIZE_OF_INT;
        COUNTER_LABELS_BUFFER_LENGTH_FIELD_OFFSET = FROM_CONDUCTOR_BUFFER_LENGTH_FIELD_OFFSET + BitUtil.SIZE_OF_INT;
        COUNTER_VALUES_BUFFER_LENGTH_FIELD_OFFSET = COUNTER_LABELS_BUFFER_LENGTH_FIELD_OFFSET + BitUtil.SIZE_OF_INT;
    }

    public static final int META_DATA_LENGTH = COUNTER_VALUES_BUFFER_LENGTH_FIELD_OFFSET + BitUtil.SIZE_OF_INT;

    public static final int END_OF_META_DATA_OFFSET = align(BitUtil.SIZE_OF_INT + META_DATA_LENGTH, BitUtil.CACHE_LINE_LENGTH);

    /**
     * Compute the length of the cnc file and return it.
     *
     * @param totalLengthOfBuffers in bytes
     * @return cnc file length in bytes
     */
    public static int computeCncFileLength(final int totalLengthOfBuffers)
    {
        return END_OF_META_DATA_OFFSET + totalLengthOfBuffers;
    }

    public static int cncVersionOffset(final int baseOffset)
    {
        return baseOffset + CNC_VERSION_FIELD_OFFSET;
    }

    public static int toConductorBufferLengthOffset(final int baseOffset)
    {
        return baseOffset + META_DATA_OFFSET + TO_CONDUCTOR_BUFFER_LENGTH_FIELD_OFFSET;
    }

    public static int fromConductorBufferLengthOffset(final int baseOffset)
    {
        return baseOffset + META_DATA_OFFSET + FROM_CONDUCTOR_BUFFER_LENGTH_FIELD_OFFSET;
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
        final UnsafeBuffer cncMetaDataBuffer,
        final int toConductorBufferLength,
        final int fromConductorBufferLength,
        final int counterLabelsBufferLength,
        final int counterValuesBufferLength)
    {
        cncMetaDataBuffer.putInt(cncVersionOffset(0), CncFileDescriptor.CNC_VERSION);
        cncMetaDataBuffer.putInt(toConductorBufferLengthOffset(0), toConductorBufferLength);
        cncMetaDataBuffer.putInt(fromConductorBufferLengthOffset(0), fromConductorBufferLength);
        cncMetaDataBuffer.putInt(counterLabelsBufferLengthOffset(0), counterLabelsBufferLength);
        cncMetaDataBuffer.putInt(counterValuesBufferLengthOffset(0), counterValuesBufferLength);
    }

    public static UnsafeBuffer createToConductorBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer)
    {
        return new UnsafeBuffer(buffer, END_OF_META_DATA_OFFSET, metaDataBuffer.getInt(toConductorBufferLengthOffset(0)));
    }

    public static UnsafeBuffer createFromConductorBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer)
    {
        final int offset = END_OF_META_DATA_OFFSET + metaDataBuffer.getInt(fromConductorBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, metaDataBuffer.getInt(fromConductorBufferLengthOffset(0)));
    }

    public static UnsafeBuffer createCounterLabelsBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer)
    {
        final int offset = END_OF_META_DATA_OFFSET +
            metaDataBuffer.getInt(toConductorBufferLengthOffset(0)) +
            metaDataBuffer.getInt(fromConductorBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, metaDataBuffer.getInt(counterLabelsBufferLengthOffset(0)));
    }

    public static UnsafeBuffer createCounterValuesBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer)
    {
        final int offset = END_OF_META_DATA_OFFSET +
            metaDataBuffer.getInt(toConductorBufferLengthOffset(0)) +
            metaDataBuffer.getInt(fromConductorBufferLengthOffset(0)) +
            metaDataBuffer.getInt(counterLabelsBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, metaDataBuffer.getInt(counterValuesBufferLengthOffset(0)));
    }

    private CncFileDescriptor()
    {
        // utility class, no instances
    }
}
