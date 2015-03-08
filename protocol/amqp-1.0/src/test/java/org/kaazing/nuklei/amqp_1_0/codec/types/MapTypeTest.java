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
public class MapTypeTest
{

    private static final int BUFFER_CAPACITY = 512;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - 256 - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode1(int offset)
    {
        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);
        mapType.maxLength(0xff);
        mapType.clear();

        assertEquals(0xc1, uint8Get(buffer, offset));
        assertEquals(offset + 3, mapType.limit());
    }

    @Theory
    public void shouldEncode8(int offset)
    {
        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);
        mapType.maxLength(0x100);
        mapType.clear();

        assertEquals(0xd1, uint8Get(buffer, offset));
        assertEquals(offset + 9, mapType.limit());
    }

    @Theory
    public void shouldDecode1(int offset)
    {
        buffer.putByte(offset, (byte) 0xc1);
        buffer.putByte(offset + 1, (byte) 0x01);
        buffer.putByte(offset + 2, (byte) 0x00);

        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);

        assertEquals(0x01, mapType.length());
        assertEquals(0x00, mapType.count());
        assertEquals(offset + 3, mapType.limit());
    }

    @Theory
    public void shouldDecode8(int offset)
    {
        buffer.putByte(offset, (byte) 0xd1);
        buffer.putInt(offset + 1, (byte) 0x04, BIG_ENDIAN);
        buffer.putInt(offset + 5, (byte) 0x00);

        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);

        assertEquals(0x04, mapType.length());
        assertEquals(0x00, mapType.count());
        assertEquals(offset + 9, mapType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode1(int offset)
    {
        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);
        mapType.maxLength(0xff);
        mapType.clear();

        assertEquals(0x00, mapType.count());
        assertEquals(0x01, mapType.length());
        assertEquals(offset + 3, mapType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode8(int offset)
    {
        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);
        mapType.maxLength(0x100);
        mapType.clear();

        assertEquals(0x00, mapType.count());
        assertEquals(0x04, mapType.length());
        assertEquals(offset + 9, mapType.limit());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);

        assertEquals(0, mapType.count());
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        MapType mapType = new MapType();
        mapType.watch(observer);
        mapType.wrap(buffer, offset, true);
        mapType.maxLength(0x00);
        mapType.clear();

        verify(observer).accept(mapType);
    }
}
