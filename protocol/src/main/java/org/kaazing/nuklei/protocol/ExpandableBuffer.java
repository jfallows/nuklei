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

import java.util.Arrays;

public class ExpandableBuffer
{
    private final AtomicBuffer atomicBuffer;

    private byte[] byteBuffer;

    public ExpandableBuffer(final int initialCapacity)
    {
        byteBuffer = new byte[BitUtil.findNextPositivePowerOfTwo(initialCapacity)];
        atomicBuffer = new AtomicBuffer(byteBuffer);
    }

    public AtomicBuffer atomicBuffer()
    {
        return atomicBuffer;
    }

    public int capacity()
    {
        return atomicBuffer.capacity();
    }

    public void putBytes(final int index, final AtomicBuffer srcBuffer, final int srcIndex, final int length)
    {
        if (index + length > byteBuffer.length)
        {
            final int newSize = BitUtil.findNextPositivePowerOfTwo(index + length);
            byteBuffer = Arrays.copyOf(byteBuffer, newSize);
            atomicBuffer.wrap(byteBuffer);
        }

        srcBuffer.getBytes(srcIndex, atomicBuffer, index, length);
    }
}
