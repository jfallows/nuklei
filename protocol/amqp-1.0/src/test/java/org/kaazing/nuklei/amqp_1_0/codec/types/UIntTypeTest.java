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
import static org.kaazing.nuklei.FlyweightBE.uint32Get;
import static org.kaazing.nuklei.amqp_1_0.codec.types.UIntType.SIZEOF_UINT_MAX;
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
public class UIntTypeTest
{

    private static final int BUFFER_CAPACITY = 64;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - SIZEOF_UINT_MAX - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode0(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(0L);

        assertEquals(0x43, uint8Get(buffer, offset));
        assertEquals(offset + 1, uintType.limit());
    }

    @Theory
    public void shouldEncode1(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(1L);

        assertEquals(0x52, uint8Get(buffer, offset));
        assertEquals(0x1L, uint8Get(buffer, offset + 1));
        assertEquals(offset + 2, uintType.limit());
    }

    @Theory
    public void shouldEncode4(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(0x12345678L);

        assertEquals(0x70, uint8Get(buffer, offset));
        assertEquals(0x12345678L, uint32Get(buffer, offset + 1));
        assertEquals(offset + 5, uintType.limit());
    }

    @Theory
    public void shouldDecode0(int offset)
    {
        buffer.putByte(offset, (byte) 0x43);

        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);

        assertEquals(0x00L, uintType.get());
        assertEquals(offset + 1, uintType.limit());
    }

    @Theory
    public void shouldDecode1(int offset)
    {
        buffer.putByte(offset, (byte) 0x52);
        buffer.putByte(offset + 1, (byte) 0x01);

        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);

        assertEquals(0x01L, uintType.get());
        assertEquals(offset + 2, uintType.limit());
    }

    @Theory
    public void shouldDecode4(int offset)
    {
        buffer.putByte(offset, (byte) 0x70);
        buffer.putInt(offset + 1, 0x12345678, BIG_ENDIAN);

        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);

        assertEquals(0x12345678, uintType.get());
        assertEquals(offset + 5, uintType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode0(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(0L);

        assertEquals(0L, uintType.get());
    }

    @Theory
    public void shouldEncodeThenDecode1(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(1L);

        assertEquals(1L, uintType.get());
    }

    @Theory
    public void shouldEncodeThenDecode4(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(12345678L);

        assertEquals(12345678L, uintType.get());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);

        assertEquals(0L, uintType.get());
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        UIntType uintType = new UIntType();
        uintType.watch(observer);
        uintType.wrap(buffer, offset, true);
        uintType.set(12345678L);

        verify(observer).accept(uintType);
    }

}
