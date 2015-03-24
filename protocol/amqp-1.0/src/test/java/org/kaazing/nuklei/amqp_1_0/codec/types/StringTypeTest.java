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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.kaazing.nuklei.Flyweight.uint8Get;
import static org.kaazing.nuklei.FlyweightBE.int32Get;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldAccessors.newAccessor;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldMutators.newMutator;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

@RunWith(Theories.class)
public class StringTypeTest
{

    private static final int BUFFER_CAPACITY = 1024;
    private static final DirectBufferAccessor<String> READ_UTF_8 = newAccessor(UTF_8);
    private static final MutableDirectBufferMutator<String> WRITE_UTF_8 = newMutator(UTF_8);

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - 256) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode1(int offset)
    {
        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);
        stringType.set(WRITE_UTF_8, "a");

        assertEquals(0xa1, uint8Get(buffer, offset));
        assertEquals(0x01, uint8Get(buffer, offset + 1));
        assertEquals(0x61, uint8Get(buffer, offset + 2));
        assertEquals(offset + 3, stringType.limit());
    }

    @Theory
    public void shouldEncode4(int offset)
    {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');

        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);
        stringType.set(WRITE_UTF_8, new String(chars));

        assertEquals(0xb1, uint8Get(buffer, offset));
        assertEquals(0x100, int32Get(buffer, offset + 1));
        for (int i = 0; i < 256; i++)
        {
            assertEquals(0x61, uint8Get(buffer, offset + 5 + i));
        }
        assertEquals(offset + 261, stringType.limit());
    }

    @Theory
    public void shouldDecode1(int offset)
    {
        buffer.putByte(offset, (byte) 0xa1);
        buffer.putByte(offset + 1, (byte) 0x01);
        buffer.putByte(offset + 2, (byte) 0x61);

        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);

        assertEquals("a", stringType.get(READ_UTF_8).toString());
        assertEquals(offset + 3, stringType.limit());
    }

    @Theory
    public void shouldDecode4(int offset)
    {
        buffer.putByte(offset, (byte) 0xb1);
        buffer.putInt(offset + 1, 0x100, BIG_ENDIAN);
        for (int i = 0; i < 256; i++)
        {
            buffer.putByte(offset + 5 + i, (byte) 0x61);
        }

        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);

        char[] chars = new char[256];
        Arrays.fill(chars, 'a');
        assertEquals(new String(chars), stringType.get(READ_UTF_8).toString());
        assertEquals(offset + 261, stringType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode1(int offset)
    {
        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);
        stringType.set(WRITE_UTF_8, "a");

        assertEquals("a", stringType.get(READ_UTF_8).toString());
    }

    @Theory
    public void shouldEncodeThenDecode4(int offset)
    {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');
        String string = new String(chars);

        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);
        stringType.set(WRITE_UTF_8, string);

        assertEquals(string, stringType.get(READ_UTF_8).toString());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);
        stringType.get(READ_UTF_8);
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        StringType stringType = new StringType();
        stringType.watch(observer);
        stringType.wrap(buffer, offset, true);
        stringType.set(WRITE_UTF_8, "a");

        verify(observer).accept(stringType);
    }

}
