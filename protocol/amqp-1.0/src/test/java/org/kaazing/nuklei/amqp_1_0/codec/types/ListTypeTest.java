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
public class ListTypeTest
{

    private static final int BUFFER_CAPACITY = 512;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - 256 - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode0(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0x00);
        listType.clear();

        assertEquals(0x45, uint8Get(buffer, offset));
        assertEquals(offset + 1, listType.limit());
    }

    @Theory
    public void shouldEncode1(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0xff);
        listType.clear();

        assertEquals(0xc0, uint8Get(buffer, offset));
        assertEquals(offset + 3, listType.limit());
    }

    @Theory
    public void shouldEncode8(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0x100);
        listType.clear();

        assertEquals(0xd0, uint8Get(buffer, offset));
        assertEquals(offset + 9, listType.limit());
    }

    @Theory
    public void shouldDecode0(int offset)
    {
        buffer.putByte(offset, (byte) 0x45);

        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);

        assertEquals(0x00, listType.count());
        assertEquals(0x00, listType.length());
        assertEquals(offset + 1, listType.limit());
    }

    @Theory
    public void shouldDecode1(int offset)
    {
        buffer.putByte(offset, (byte) 0xc0);
        buffer.putByte(offset + 1, (byte) 0x01);
        buffer.putByte(offset + 2, (byte) 0x00);

        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);

        assertEquals(0x01, listType.length());
        assertEquals(0x00, listType.count());
        assertEquals(offset + 3, listType.limit());
    }

    @Theory
    public void shouldDecode8(int offset)
    {
        buffer.putByte(offset, (byte) 0xd0);
        buffer.putInt(offset + 1, (byte) 0x04, BIG_ENDIAN);
        buffer.putInt(offset + 5, (byte) 0x00);

        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);

        assertEquals(0x04, listType.length());
        assertEquals(0x00, listType.count());
        assertEquals(offset + 9, listType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode0(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0x00);
        listType.clear();

        assertEquals(0x00, listType.count());
        assertEquals(0x00, listType.length());
        assertEquals(offset + 1, listType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode1(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0xff);
        listType.clear();

        assertEquals(0x00, listType.count());
        assertEquals(0x01, listType.length());
        assertEquals(offset + 3, listType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode8(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0x100);
        listType.clear();

        assertEquals(0x00, listType.count());
        assertEquals(0x04, listType.length());
        assertEquals(offset + 9, listType.limit());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);

        assertEquals(0, listType.count());
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        ListType listType = new ListType();
        listType.watch(observer);
        listType.wrap(buffer, offset, true);
        listType.maxLength(0x00);
        listType.clear();

        verify(observer).accept(listType);
    }
}
