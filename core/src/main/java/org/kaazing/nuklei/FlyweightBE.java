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
package org.kaazing.nuklei;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import static java.nio.ByteOrder.BIG_ENDIAN;

/**
 * Big endian flyweight.
 */
public class FlyweightBE extends Flyweight
{
    /**
     * Construct a flyweight with a big endian byte order
     */
    public FlyweightBE()
    {
        super(BIG_ENDIAN);
    }

    /**
     * Return the 32-bit field at a given location as an float.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return float representation of the 32-bit field
     */
    public static float floatGet(final DirectBuffer buffer, final int offset)
    {
        return buffer.getFloat(offset, BIG_ENDIAN);
    }

    /**
     * Encode the given float value as a 32-bit field at the given location.
     *
     * @param buffer to write from
     * @param offset to write at
     * @param value to encode represented as a 32-bit field
     */
    public static void floatPut(final MutableDirectBuffer buffer, final int offset, final float value)
    {
        buffer.putFloat(offset, value, BIG_ENDIAN);
    }

    /**
     * Return the 64-bit field at a given location as an double.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return double representation of the 64-bit field
     */
    public static double doubleGet(final DirectBuffer buffer, final int offset)
    {
        return buffer.getDouble(offset, BIG_ENDIAN);
    }

    /**
     * Encode the given double value as a 64-bit field at the given location.
     *
     * @param buffer to write from
     * @param offset to write at
     * @param value to encode represented as a 64-bit field
     */
    public static void doublePut(final MutableDirectBuffer buffer, final int offset, final double value)
    {
        buffer.putDouble(offset, value, BIG_ENDIAN);
    }

    /**
     * Return the 16-bit field at a given location as an unsigned integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return int representation of the 16-bit signed value
     */
    public static int uint16Get(final DirectBuffer buffer, final int offset)
    {
        return (int)(buffer.getShort(offset, BIG_ENDIAN) & 0xFFFF);
    }

    /**
     * Encode a given value as an 16-bit unsigned integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as an int
     */
    public static void uint16Put(final MutableDirectBuffer buffer, final int offset, final int value)
    {
        buffer.putShort(offset, (short)value, BIG_ENDIAN);
    }

    /**
     * Return the 16-bit field at a given location as a signed integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return short representation of the 16-bit signed value
     */
    public static short int16Get(final DirectBuffer buffer, final int offset)
    {
        return buffer.getShort(offset, BIG_ENDIAN);
    }

    /**
     * Encode a given value as an 16-bit signed integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as a short
     */
    public static void int16Put(final MutableDirectBuffer buffer, final int offset, final short value)
    {
        buffer.putShort(offset, value, BIG_ENDIAN);
    }

    /**
     * Return the 32-bit field at a given location as an unsigned integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return long representation of the 32-bit signed value
     */
    public static long uint32Get(final DirectBuffer buffer, final int offset)
    {
        return (long)(buffer.getInt(offset, BIG_ENDIAN) & 0xFFFFFFFFL);
    }

    /**
     * Encode a given value as an 32-bit unsigned integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as an long
     */
    public static void uint32Put(final MutableDirectBuffer buffer, final int offset, final long value)
    {
        buffer.putInt(offset, (int)value, BIG_ENDIAN);
    }

    /**
     * Return the 32-bit field at a given location as a signed integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return int representation of the 32-bit signed value
     */
    public static int int32Get(final DirectBuffer buffer, final int offset)
    {
        return buffer.getInt(offset, BIG_ENDIAN);
    }

    /**
     * Encode a given value as an 32-bit signed integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as a int
     */
    public static void int32Put(final MutableDirectBuffer buffer, final int offset, final int value)
    {
        buffer.putInt(offset, value, BIG_ENDIAN);
    }

    /**
     * Return the 64-bit field at a given location as a signed integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return long representation of the 64-bit signed value
     */
    public static long int64Get(final DirectBuffer buffer, final int offset)
    {
        return buffer.getLong(offset, BIG_ENDIAN);
    }

    /**
     * Encode a given value as an 64-bit signed integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as a long
     */
    public static void int64Put(final MutableDirectBuffer buffer, final int offset, final long value)
    {
        buffer.putLong(offset, value, BIG_ENDIAN);
    }

}
