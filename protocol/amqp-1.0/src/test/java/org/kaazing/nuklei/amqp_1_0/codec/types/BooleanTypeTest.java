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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.kaazing.nuklei.Flyweight.uint8Get;
import static org.kaazing.nuklei.amqp_1_0.codec.types.BooleanType.SIZEOF_BOOLEAN_MAX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
public class BooleanTypeTest
{

    private static final int BUFFER_CAPACITY = 64;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - SIZEOF_BOOLEAN_MAX - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncodeTrue0(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(true);

        assertEquals(0x41, uint8Get(buffer, offset));
    }

    @Theory
    public void shouldEncodeFalse0(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(false);

        assertEquals(0x42, uint8Get(buffer, offset));
    }

    @Theory
    public void shouldDecodeTrue0(int offset)
    {
        buffer.putByte(offset, (byte) 0x41);

        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);

        assertTrue(booleanType.get());
        assertEquals(offset + 1, booleanType.limit());
    }

    @Theory
    public void shouldDecodeFalse0(int offset)
    {
        buffer.putByte(offset, (byte) 0x42);

        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);

        assertFalse(booleanType.get());
        assertEquals(offset + 1, booleanType.limit());
    }

    @Theory
    @Ignore("need way to indicate width")
    public void shouldEncodeTrue1(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(true);

        assertEquals(0x56, uint8Get(buffer, offset));
        assertEquals(0x01, uint8Get(buffer, offset + 1));
    }

    @Theory
    @Ignore("need way to indicate width")
    public void shouldEncodeFalse1(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(false);

        assertEquals(0x56, uint8Get(buffer, offset));
        assertEquals(0x00, uint8Get(buffer, offset + 1));
    }

    @Theory
    public void shouldDecodeTrue1(int offset)
    {
        buffer.putByte(offset, (byte) 0x56);
        buffer.putByte(offset + 1, (byte) 0x01);

        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);

        assertTrue(booleanType.get());
        assertEquals(offset + 2, booleanType.limit());
    }

    @Theory
    public void shouldDecodeFalse1(int offset)
    {
        buffer.putByte(offset, (byte) 0x56);
        buffer.putByte(offset + 1, (byte) 0x00);

        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);

        assertFalse(booleanType.get());
        assertEquals(offset + 2, booleanType.limit());
    }

    @Theory
    public void shouldEncodeThenDecodeTrue0(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(true);

        assertTrue(booleanType.get());
        assertEquals(offset + 1, booleanType.limit());
    }

    @Theory
    public void shouldEncodeThenDecodeFalse0(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(false);

        assertFalse(booleanType.get());
        assertEquals(offset + 1, booleanType.limit());
    }

    @Theory
    @Ignore("need way to indicate width")
    public void shouldEncodeThenDecodeTrue1(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(true);

        assertTrue(booleanType.get());
        assertEquals(offset + 2, booleanType.limit());
    }

    @Theory
    @Ignore("need way to indicate width")
    public void shouldEncodeThenDecodeFalse1(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(false);

        assertFalse(booleanType.get());
        assertEquals(offset + 2, booleanType.limit());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);

        assertTrue(booleanType.get());
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        BooleanType booleanType = new BooleanType();
        booleanType.watch(observer);
        booleanType.wrap(buffer, offset, true);
        booleanType.set(false);

        verify(observer).accept(booleanType);
    }

}
