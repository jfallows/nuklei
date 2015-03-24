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
import static org.kaazing.nuklei.FlyweightBE.int64Get;
import static org.kaazing.nuklei.amqp_1_0.codec.types.TimestampType.SIZEOF_TIMESTAMP;
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
public class TimestampTypeTest
{

    private static final int BUFFER_CAPACITY = 64;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - SIZEOF_TIMESTAMP - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode(int offset)
    {
        TimestampType timestampType = new TimestampType();
        timestampType.wrap(buffer, offset, true);
        timestampType.set(0x12345678L);

        assertEquals(0x83, uint8Get(buffer, offset));
        assertEquals(0x12345678L, int64Get(buffer, offset + 1));
    }

    @Theory
    public void shouldDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x83);
        buffer.putLong(offset + 1, 0x12345678L, BIG_ENDIAN);

        TimestampType timestampType = new TimestampType();
        timestampType.wrap(buffer, offset, true);

        assertEquals(0x12345678L, timestampType.get());
        assertEquals(offset + 9, timestampType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode(int offset)
    {
        TimestampType timestampType = new TimestampType();
        timestampType.wrap(buffer, offset, true);
        timestampType.set(0x12345678L);

        assertEquals(0x12345678L, timestampType.get());
        assertEquals(offset + 9, timestampType.limit());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        TimestampType timestampType = new TimestampType();
        timestampType.wrap(buffer, offset, true);

        assertEquals(0L, timestampType.get());
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        TimestampType timestampType = new TimestampType();
        timestampType.watch(observer);
        timestampType.wrap(buffer, offset, true);
        timestampType.set(0x12345678L);

        verify(observer).accept(timestampType);
    }

}
