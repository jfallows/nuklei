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

import static java.nio.ByteOrder.BIG_ENDIAN;
import static org.junit.Assert.assertEquals;
import static org.kaazing.nuklei.Flyweight.uint8Get;
import static org.kaazing.nuklei.FlyweightBE.int32Get;
import static org.kaazing.nuklei.amqp_1_0.codec.types.IntType.SIZEOF_INT_MAX;
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
public class IntTypeTest
{

    private static final int BUFFER_CAPACITY = 64;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - SIZEOF_INT_MAX - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode1(int offset)
    {
        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);
        intType.set(1);

        assertEquals(0x54, uint8Get(buffer, offset));
        assertEquals(offset + 2, intType.limit());
    }

    @Theory
    public void shouldEncode4(int offset)
    {
        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);
        intType.set(0x12345678);

        assertEquals(0x71, uint8Get(buffer, offset));
        assertEquals(0x12345678, int32Get(buffer, offset + 1));
        assertEquals(offset + 5, intType.limit());
    }

    @Theory
    public void shouldDecode1(int offset)
    {
        buffer.putByte(offset, (byte) 0x54);
        buffer.putByte(offset + 1, (byte) 0x01);

        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);

        assertEquals(0x01, intType.get());
        assertEquals(offset + 2, intType.limit());
    }

    @Theory
    public void shouldDecode4(int offset)
    {
        buffer.putByte(offset, (byte) 0x71);
        buffer.putInt(offset + 1, 0x12345678, BIG_ENDIAN);

        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);

        assertEquals(0x12345678, intType.get());
        assertEquals(offset + 5, intType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode1(int offset)
    {
        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);
        intType.set(1);

        assertEquals(1, intType.get());
    }

    @Theory
    public void shouldEncodeThenDecode4(int offset)
    {
        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);
        intType.set(12345678);

        assertEquals(12345678, intType.get());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);

        assertEquals(0L, intType.get());
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        IntType intType = new IntType();
        intType.watch(observer);
        intType.wrap(buffer, offset, true);
        intType.set(12345678);

        verify(observer).accept(intType);
    }

}
