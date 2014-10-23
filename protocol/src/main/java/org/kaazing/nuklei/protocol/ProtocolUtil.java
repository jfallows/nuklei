/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
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

package org.kaazing.nuklei.protocol;

import org.kaazing.nuklei.BitUtil;
import org.kaazing.nuklei.concurrent.AtomicBuffer;

/**
 * Utility functions for handling protocol parsing
 */
public class ProtocolUtil
{
    public static boolean compareMemory(
        final AtomicBuffer buffer,
        final int index,
        final AtomicBuffer valueBuffer,
        final int valueIndex,
        final int length)
    {
        if (AtomicBuffer.BOUNDS_CHECK)
        {
            if (index + length > buffer.capacity() || valueIndex + length > valueBuffer.capacity())
            {
                throw new IndexOutOfBoundsException(
                    String.format("index=%d capacity=%d valueIndex=%d length=%d valueCapacity=%d",
                        index, buffer.capacity(), valueIndex, length, valueBuffer.capacity()));
            }
        }

        final int maxForLongs = length & ~(BitUtil.SIZE_OF_LONG - 1);
        int maxForBytesAtEnd = length - maxForLongs;
        boolean result = true;

        for (int i = 0; i < maxForLongs; i += BitUtil.SIZE_OF_LONG)
        {
            final long diff = buffer.getLong(index + i) - valueBuffer.getLong(valueIndex + i);

            if (0 != diff)
            {
                result = false;
                maxForBytesAtEnd = 0;   // short circuit remainder loop
                break;
            }
        }

        for (int i = 0; i < maxForBytesAtEnd; i++)
        {
            final long diff =
                buffer.getByte(index + maxForLongs + i) - valueBuffer.getByte(valueIndex + maxForLongs + i);

            if (0 != diff)
            {
                result = false;
                break;
            }
        }

        return result;
    }

    public static boolean compareCaseInsensitiveMemory(
        final AtomicBuffer buffer,
        final int index,
        final AtomicBuffer lowerCaseValueBuffer,
        final AtomicBuffer upperCaseValueBuffer,
        final int valueIndex,
        final int length)
    {

        if (AtomicBuffer.BOUNDS_CHECK)
        {
            if (index + length > buffer.capacity() || valueIndex + length > lowerCaseValueBuffer.capacity())
            {
                throw new IndexOutOfBoundsException(
                        String.format("index=%d capacity=%d valueIndex=%d length=%d valueCapacity=%d",
                                index, buffer.capacity(), valueIndex, length, lowerCaseValueBuffer.capacity()));
            }
        }

        for (int i = 0; i < length; i++)
        {
            byte ch = buffer.getByte(index+i);
            byte lowerValCh = lowerCaseValueBuffer.getByte(valueIndex + i);

            if (ch != lowerValCh)
            {
                byte upperValCh = upperCaseValueBuffer.getByte(valueIndex + i);
                if (ch != upperValCh)
                {
                    return false;
                }
            }
        }

        return true;
    }

    public static int findNextOccurrence(
        final AtomicBuffer buffer,
        final int index,
        final byte[] substr,
        final int length)
    {
        int result = -1;
        int substrIndex = 0;

        final int max = index + length;
        for (int i = index; i <= max; i++)
        {
//            System.out.println(
//                "[" + i + "] " + buffer.getByte(i) + " == [" + substrIndex + "] " + substr[substrIndex]);
            if (buffer.getByte(i) != substr[substrIndex])
            {
                substrIndex = 0;
                continue;
            }

            if (substr.length == ++substrIndex)
            {
                result = i;
                break;
            }
        }

        return result;
    }

    public static int leadingCount(final AtomicBuffer buffer, final int index, final byte[] delim, final int length)
    {
        for (int i = 0; i < length; i++)
        {
            if (!isOneOf(buffer.getByte(index + i), delim))
            {
                return i;
            }
        }

        return length;
    }

    private static boolean isOneOf(final byte value, final byte[] delim)
    {
        for (final byte delimByte : delim)
        {
            if (value == delimByte)
            {
                return true;
            }
        }

        return false;
    }
}
