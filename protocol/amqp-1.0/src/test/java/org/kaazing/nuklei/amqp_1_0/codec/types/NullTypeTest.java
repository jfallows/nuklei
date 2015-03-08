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
package org.kaazing.nuklei.amqp_1_0.codec.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.kaazing.nuklei.Flyweight.uint8Get;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Random;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

@RunWith(Theories.class)
public class NullTypeTest
{

    private static final int BUFFER_CAPACITY = 64;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode(int offset)
    {
        NullType nullType = new NullType();
        nullType.wrap(buffer, offset, true);
        nullType.set(null);

        assertEquals(0x40, uint8Get(buffer, offset));
        assertEquals(offset + 1, nullType.limit());
    }

    @Theory
    public void shouldDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x40);

        NullType nullType = new NullType();
        nullType.wrap(buffer, offset, true);

        assertNull(nullType.get());
        assertEquals(offset + 1, nullType.limit());
    }

    @Theory
    public void shouldEncodeAndDecode(int offset)
    {
        NullType nullType = new NullType();
        nullType.wrap(buffer, offset, true);
        nullType.set(null);

        assertNull(nullType.get());
        assertEquals(offset + 1, nullType.limit());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        NullType nullType = new NullType();
        nullType.wrap(buffer, offset, true);

        assertNull(nullType.get());
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        NullType nullType = new NullType();
        nullType.watch(observer);
        nullType.wrap(buffer, offset, true);
        nullType.set(null);

        verify(observer).accept(nullType);
    }

}
