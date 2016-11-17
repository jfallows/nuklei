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
package org.kaazing.nuklei.ws.internal.util;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.nativeOrder;
import org.agrona.BitUtil;
import org.agrona.MutableDirectBuffer;

public final class BufferUtil
{
    private static final int REMAINING_SHIFT_1ST_BYTE;
    private static final int REMAINING_SHIFT_1ST_SHORT;
    private static final int REMAINING_SHIFT_3RD_BYTE;

    static
    {
        if (nativeOrder() == BIG_ENDIAN)
        {
            REMAINING_SHIFT_1ST_BYTE = 24;
            REMAINING_SHIFT_1ST_SHORT = 16;
            REMAINING_SHIFT_3RD_BYTE = 8;
        }
        else
        {
            REMAINING_SHIFT_1ST_BYTE = 0;
            REMAINING_SHIFT_1ST_SHORT = 0;
            REMAINING_SHIFT_3RD_BYTE = 16;
        }
    }

    private BufferUtil()
    {
        // utility class, no instances
    }

    public static int xor(
        final MutableDirectBuffer buffer,
        final int offset,
        final int length,
        final int bits)
    {
        if (bits != 0)
        {
            int index = offset;
            int remaining = length;

            while (remaining >= BitUtil.SIZE_OF_INT)
            {
                buffer.putInt(index, buffer.getInt(index) ^ bits);
                index += BitUtil.SIZE_OF_INT;
                remaining -= BitUtil.SIZE_OF_INT;
            }

            switch (remaining)
            {
            case 0:
                break;
            case 1:
                buffer.putByte(index, (byte) (buffer.getByte(index) ^ ((bits >> REMAINING_SHIFT_1ST_BYTE) & 0xff)));
                break;
            case 2:
                buffer.putShort(index, (short) (buffer.getShort(index) ^ ((bits >> REMAINING_SHIFT_1ST_SHORT) & 0xffff)));
                break;
            case 3:
                buffer.putShort(index, (short) (buffer.getShort(index) ^ ((bits >> REMAINING_SHIFT_1ST_SHORT) & 0xffff)));
                index += BitUtil.SIZE_OF_SHORT;
                buffer.putByte(index, (byte) (buffer.getByte(index) ^ ((bits >> REMAINING_SHIFT_3RD_BYTE) & 0xff)));
                break;
            default:
                throw new IllegalStateException("remaining=" + remaining);
            }
        }

        return length;
    }
}
