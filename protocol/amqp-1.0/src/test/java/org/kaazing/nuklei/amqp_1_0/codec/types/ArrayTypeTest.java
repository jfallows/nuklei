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
import static org.kaazing.nuklei.Flyweight.uint8Get;
import static org.kaazing.nuklei.FlyweightBE.int32Get;

import java.util.Random;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

@RunWith(Theories.class)
public class ArrayTypeTest
{

    private static final int BUFFER_CAPACITY = 512;

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - 256 - 1) + 1;

    private final AtomicBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldEncode1(int offset)
    {
        ArrayType arrayType = new ArrayType();
        arrayType.wrap(buffer, offset, true);
        arrayType.maxLength(0xff);
        arrayType.limit(0x00, offset + 0x03);

        assertEquals(0xe0, uint8Get(buffer, offset));
        assertEquals(0x01, uint8Get(buffer, offset + 1));
        assertEquals(0x00, uint8Get(buffer, offset + 2));
        assertEquals(offset + 3, arrayType.limit());
    }

    @Theory
    public void shouldEncode4(int offset)
    {
        ArrayType arrayType = new ArrayType();
        arrayType.wrap(buffer, offset, true);
        arrayType.maxLength(0x100);
        arrayType.limit(0x00, offset + 0x09);

        assertEquals(0xf0, uint8Get(buffer, offset));
        assertEquals(0x04, int32Get(buffer, offset + 1));
        assertEquals(0x00, int32Get(buffer, offset + 5));
        assertEquals(offset + 9, arrayType.limit());
    }
}
