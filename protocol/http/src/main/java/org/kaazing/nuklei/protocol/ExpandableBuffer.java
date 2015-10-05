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
package org.kaazing.nuklei.protocol;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.util.Arrays;

public class ExpandableBuffer
{
    private final MutableDirectBuffer reassemblyBuffer;

    private byte[] byteBuffer;

    public ExpandableBuffer(final int initialCapacity)
    {
        byteBuffer = new byte[BitUtil.findNextPositivePowerOfTwo(initialCapacity)];
        reassemblyBuffer = new UnsafeBuffer(byteBuffer);
    }

    public MutableDirectBuffer atomicBuffer()
    {
        return reassemblyBuffer;
    }

    public int capacity()
    {
        return reassemblyBuffer.capacity();
    }

    public void putBytes(final int index, final DirectBuffer srcBuffer, final int srcIndex, final int length)
    {
        if (index + length > byteBuffer.length)
        {
            final int newSize = BitUtil.findNextPositivePowerOfTwo(index + length);
            byteBuffer = Arrays.copyOf(byteBuffer, newSize);
            reassemblyBuffer.wrap(byteBuffer);
        }

        srcBuffer.getBytes(srcIndex, reassemblyBuffer, index, length);
    }
}
