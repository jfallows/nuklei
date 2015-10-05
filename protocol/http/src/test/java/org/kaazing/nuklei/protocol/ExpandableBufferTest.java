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

import org.junit.Test;
import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ExpandableBufferTest
{
    @Test
    public void shouldGrowFromInitialCapacity()
    {
        final ExpandableBuffer buffer = new ExpandableBuffer(64);
        final MutableDirectBuffer addedBuffer = new UnsafeBuffer(new byte[256]);

        assertThat(buffer.capacity(), is(64));
        buffer.putBytes(64, addedBuffer, 0, addedBuffer.capacity());
        assertThat(buffer.capacity(), is(BitUtil.findNextPositivePowerOfTwo(64 + 256)));
    }

    @Test
    public void shouldGrowFromInitialCapacityShortIndexFromEnd()
    {
        final ExpandableBuffer buffer = new ExpandableBuffer(64);
        final MutableDirectBuffer addedBuffer = new UnsafeBuffer(new byte[32]);

        assertThat(buffer.capacity(), is(64));
        buffer.putBytes(32, addedBuffer, 0, addedBuffer.capacity());
        assertThat(buffer.capacity(), is(64));
    }

    @Test
    public void shouldNotGrowWhenIfNotNeededTo()
    {
        final ExpandableBuffer buffer = new ExpandableBuffer(64);
        final MutableDirectBuffer addedBuffer = new UnsafeBuffer(new byte[32]);

        assertThat(buffer.capacity(), is(64));
        buffer.putBytes(32, addedBuffer, 0, addedBuffer.capacity());
        assertThat(buffer.capacity(), is(64));
    }
}
