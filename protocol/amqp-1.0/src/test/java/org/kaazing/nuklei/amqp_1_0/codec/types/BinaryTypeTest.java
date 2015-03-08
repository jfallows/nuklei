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
public class BinaryTypeTest
{

    private static final int BUFFER_CAPACITY = 512;
    private static final DirectBufferAccessor<String> READ_UTF_8 = newAccessor(UTF_8);
    private static final MutableDirectBufferMutator<String> WRITE_UTF_8 = newMutator(UTF_8);

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - 256 - 1) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode1(int offset)
    {
        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);
        binaryType.set(WRITE_UTF_8, "Hello, world");

        assertEquals(0xa0, uint8Get(buffer, offset));
        assertEquals(offset + 14, binaryType.limit());
    }

    @Theory
    public void shouldEncode4(int offset)
    {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');

        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);
        binaryType.set(WRITE_UTF_8, new String(chars));

        assertEquals(0xb0, uint8Get(buffer, offset));
        assertEquals(0x100, int32Get(buffer, offset + 1));
        assertEquals(offset + 261, binaryType.limit());
    }

    @Theory
    public void shouldDecode1(int offset)
    {
        buffer.putByte(offset, (byte) 0xa0);
        buffer.putByte(offset + 1, (byte) 0x0c);
        buffer.putBytes(offset + 2, "Hello, world".getBytes(UTF_8));

        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);

        assertEquals("Hello, world", binaryType.get(READ_UTF_8));
        assertEquals(offset + 14, binaryType.limit());
    }

    @Theory
    public void shouldDecode4(int offset)
    {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');

        buffer.putByte(offset, (byte) 0xb0);
        buffer.putInt(offset + 1, 0x100, BIG_ENDIAN);
        buffer.putBytes(offset + 5, new String(chars).getBytes(UTF_8));

        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);

        assertEquals(new String(chars), binaryType.get(READ_UTF_8));
        assertEquals(offset + 261, binaryType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode1(int offset)
    {
        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);
        binaryType.set(WRITE_UTF_8, "Hello, world");

        assertEquals("Hello, world", binaryType.get(READ_UTF_8));
        assertEquals(offset + 14, binaryType.limit());
    }

    @Theory
    public void shouldEncodeThenDecode4(int offset)
    {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');

        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);
        binaryType.set(WRITE_UTF_8, new String(chars));

        assertEquals(new String(chars), binaryType.get(READ_UTF_8));
        assertEquals(offset + 261, binaryType.limit());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);
        binaryType.get(READ_UTF_8);
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        BinaryType binaryType = new BinaryType();
        binaryType.watch(observer);
        binaryType.wrap(buffer, offset, true);
        binaryType.set(WRITE_UTF_8, "Hello, world");

        verify(observer).accept(binaryType);
    }

}
