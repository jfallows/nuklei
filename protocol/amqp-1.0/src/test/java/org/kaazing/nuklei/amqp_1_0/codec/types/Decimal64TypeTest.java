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

import static java.math.MathContext.DECIMAL64;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static org.junit.Assert.assertEquals;
import static org.kaazing.nuklei.Flyweight.uint8Get;
import static org.kaazing.nuklei.FlyweightBE.int64Get;
import static org.kaazing.nuklei.amqp_1_0.codec.types.Decimal64Type.SIZEOF_INT_MAX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

@RunWith(Theories.class)
public class Decimal64TypeTest
{

    private static final int BUFFER_CAPACITY = 64;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - SIZEOF_INT_MAX - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    @Ignore("until decimal64 format implemented")
    public void shouldEncode(int offset)
    {
        Decimal64Type decimal64Type = new Decimal64Type();
        decimal64Type.wrap(buffer, offset, true);
        decimal64Type.set(new BigDecimal(12345678L, DECIMAL64));

        assertEquals(0x84, uint8Get(buffer, offset));
        assertEquals(0x12345678L, int64Get(buffer, offset + 1));
        assertEquals(offset + 9, decimal64Type.limit());
    }

    @Theory
    @Ignore("until decimal64 format implemented")
    public void shouldDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x84);
        buffer.putLong(offset + 1, 0x12345678L, BIG_ENDIAN);

        Decimal64Type decimal64Type = new Decimal64Type();
        decimal64Type.wrap(buffer, offset, true);

        assertEquals(0x12345678L, decimal64Type.get());
        assertEquals(offset + 9, decimal64Type.limit());
    }

    @Theory
    @Ignore("until decimal64 format implemented")
    public void shouldEncodeThenDecode(int offset)
    {
        Decimal64Type decimal64Type = new Decimal64Type();
        decimal64Type.wrap(buffer, offset, true);
        decimal64Type.set(new BigDecimal(12345678L, DECIMAL64));

        assertEquals(12345678L, decimal64Type.get());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        Decimal64Type decimal64Type = new Decimal64Type();
        decimal64Type.wrap(buffer, offset, true);

        assertEquals(0L, decimal64Type.get());
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        Decimal64Type decimal64Type = new Decimal64Type();
        decimal64Type.watch(observer);
        decimal64Type.wrap(buffer, offset, true);
        decimal64Type.set(new BigDecimal(12345678L, DECIMAL64));

        verify(observer).accept(decimal64Type);
    }

}
