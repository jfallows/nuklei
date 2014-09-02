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

import static java.nio.ByteOrder.LITTLE_ENDIAN;

import org.kaazing.nuklei.concurrent.AtomicBuffer;

/**
 * Little endian flyweight.
 */
public class FlyweightLE extends Flyweight
{
    /**
     * Construct a flyweight with a big endian byte order
     */
    public FlyweightLE()
    {
        super(LITTLE_ENDIAN);
    }

    /**
     * Return the 32-bit field at a given location as an float.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return float representation of the 32-bit field
     */
    public static float floatGet(final AtomicBuffer buffer, final int offset)
    {
        return buffer.getFloat(offset, LITTLE_ENDIAN);
    }

    /**
     * Return the 64-bit field at a given location as an double.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return double representation of the 64-bit field
     */
    public static double doubleGet(final AtomicBuffer buffer, final int offset)
    {
        return buffer.getDouble(offset, LITTLE_ENDIAN);
    }

    /**
     * Return the 16-bit field at a given location as an unsigned integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return int representation of the 16-bit signed value
     */
    public static int uint16Get(final AtomicBuffer buffer, final int offset)
    {
        return (int)(buffer.getShort(offset, LITTLE_ENDIAN) & 0xFFFF);
    }

    /**
     * Encode a given value as an 16-bit unsigned integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as an int
     */
    public static void uint16Put(final AtomicBuffer buffer, final int offset, final int value)
    {
        buffer.putShort(offset, (short)value, LITTLE_ENDIAN);
    }

    /**
     * Return the 16-bit field at a given location as a signed integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return short representation of the 16-bit signed value
     */
    public static short int16Get(final AtomicBuffer buffer, final int offset)
    {
        return buffer.getShort(offset, LITTLE_ENDIAN);
    }

    /**
     * Encode a given value as an 16-bit signed integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as a short
     */
    public static void int16Put(final AtomicBuffer buffer, final int offset, final short value)
    {
        buffer.putShort(offset, value, LITTLE_ENDIAN);
    }

    /**
     * Return the 32-bit field at a given location as an unsigned integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return long representation of the 32-bit signed value
     */
    public static long uint32Get(final AtomicBuffer buffer, final int offset)
    {
        return (long)(buffer.getInt(offset, LITTLE_ENDIAN) & 0xFFFFFFFFL);
    }

    /**
     * Encode a given value as an 32-bit unsigned integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as an long
     */
    public static void uint32Put(final AtomicBuffer buffer, final int offset, final long value)
    {
        buffer.putInt(offset, (int)value, LITTLE_ENDIAN);
    }

    /**
     * Return the 32-bit field at a given location as a signed integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return int representation of the 32-bit signed value
     */
    public static int int32Get(final AtomicBuffer buffer, final int offset)
    {
        return buffer.getInt(offset, LITTLE_ENDIAN);
    }

    /**
     * Encode a given value as an 32-bit signed integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as a int
     */
    public static void int32Put(final AtomicBuffer buffer, final int offset, final int value)
    {
        buffer.putInt(offset, value, LITTLE_ENDIAN);
    }

    /**
     * Return the 64-bit field at a given location as a signed integer.
     *
     * @param buffer to read from
     * @param offset to read from
     * @return long representation of the 64-bit signed value
     */
    public static long int64Get(final AtomicBuffer buffer, final int offset)
    {
        return buffer.getLong(offset, LITTLE_ENDIAN);
    }

    /**
     * Encode a given value as an 64-bit signed integer at a given location.
     *
     * @param buffer to write to
     * @param offset to write at
     * @param value to encode represented as a long
     */
    public static void int64Put(final AtomicBuffer buffer, final int offset, final long value)
    {
        buffer.putLong(offset, value, LITTLE_ENDIAN);
    }

}
