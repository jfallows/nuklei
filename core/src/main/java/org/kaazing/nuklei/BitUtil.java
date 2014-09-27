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

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import sun.misc.Unsafe;

/**
 * Various bit utilities
 *
 * Heavily adopted from SBE.
 */
public class BitUtil
{
    /** Size of a byte in bytes */
    public static final int SIZE_OF_BYTE = 1;
    /** Size of a boolean in bytes */
    public static final int SIZE_OF_BOOLEAN = 1;
    /** Size of a signed int (8-bit) */
    public static final int SIZE_OF_INT8 = 1;
    /** Size of an unsigned int (8-bit) */
    public static final int SIZE_OF_UINT8 = 1;

    /** Size of a char in bytes */
    public static final int SIZE_OF_CHAR = 2;
    /** Size of a short in bytes */
    public static final int SIZE_OF_SHORT = 2;
    /** Size of a signed int (16-bit) */
    public static final int SIZE_OF_INT16 = 2;
    /** Size of an unsigned int (16-bit) */
    public static final int SIZE_OF_UINT16 = 2;

    /** Size of an int in bytes */
    public static final int SIZE_OF_INT = 4;
    /** Size of a a float in bytes */
    public static final int SIZE_OF_FLOAT = 4;
    /** Size of a signed int (32-bit) */
    public static final int SIZE_OF_INT32 = 4;
    /** Size of an unsigned int (32-bit) */
    public static final int SIZE_OF_UINT32 = 4;

    /** Size of a long in bytes */
    public static final int SIZE_OF_LONG = 8;
    /** Size of a double in bytes */
    public static final int SIZE_OF_DOUBLE = 8;
    /** Size of a signed int (64-bit) */
    public static final int SIZE_OF_INT64 = 8;

    /** Size of the data blocks used by the CPU cache sub-system in bytes. */
    public static final int CACHE_LINE_SIZE = 64;

    /** theUnsafe */
    public static final Unsafe UNSAFE;

    private static final byte[] HEX_DIGIT_TABLE = { '0', '1', '2', '3', '4', '5', '6', '7',
                                                    '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final byte[] FROM_HEX_DIGIT_TABLE;
    
    static {
        FROM_HEX_DIGIT_TABLE = new byte[128];
        FROM_HEX_DIGIT_TABLE['0'] = 0x00;
        FROM_HEX_DIGIT_TABLE['1'] = 0x01;
        FROM_HEX_DIGIT_TABLE['2'] = 0x02;
        FROM_HEX_DIGIT_TABLE['3'] = 0x03;
        FROM_HEX_DIGIT_TABLE['4'] = 0x04;
        FROM_HEX_DIGIT_TABLE['5'] = 0x05;
        FROM_HEX_DIGIT_TABLE['6'] = 0x06;
        FROM_HEX_DIGIT_TABLE['7'] = 0x07;
        FROM_HEX_DIGIT_TABLE['8'] = 0x08;
        FROM_HEX_DIGIT_TABLE['9'] = 0x09;
        FROM_HEX_DIGIT_TABLE['a'] = 0x0a;
        FROM_HEX_DIGIT_TABLE['A'] = 0x0a;
        FROM_HEX_DIGIT_TABLE['b'] = 0x0b;
        FROM_HEX_DIGIT_TABLE['B'] = 0x0b;
        FROM_HEX_DIGIT_TABLE['c'] = 0x0c;
        FROM_HEX_DIGIT_TABLE['C'] = 0x0c;
        FROM_HEX_DIGIT_TABLE['d'] = 0x0d;
        FROM_HEX_DIGIT_TABLE['D'] = 0x0d;
        FROM_HEX_DIGIT_TABLE['e'] = 0x0e;
        FROM_HEX_DIGIT_TABLE['E'] = 0x0e;
        FROM_HEX_DIGIT_TABLE['f'] = 0x0f;
        FROM_HEX_DIGIT_TABLE['F'] = 0x0f;
    }

    static
    {
        try
        {
            final PrivilegedExceptionAction<Unsafe> action = () ->
            {
                final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return (Unsafe)field.get(null);
            };

            UNSAFE = AccessController.doPrivileged(action);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fast method of finding the next power of 2 greater than or equal to the supplied value.
     *
     * If the value is less than or equal to 0 then 1 will be returned.
     *
     * This method is not suitable for {@link Integer#MIN_VALUE} or numbers greater than 2^30.
     *
     * @param value from which to search for next power of 2
     * @return The next power of 2 or the value itself if it is a power of 2
     */
    public static int findNextPositivePowerOfTwo(final int value)
    {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    /**
     * Align a value to the next multiple up of alignment.
     * If the value equals an alignment multiple then it is returned unchanged.
     *
     * This method executes without branching.
     *
     * @param value to be aligned up.
     * @param alignment to use.
     * @return the value aligned to the next boundary.
     */
    public static int align(final int value, final int alignment)
    {
        return (value + (alignment - 1)) & ~(alignment - 1);
    }

    /**
     * Align a value to the next multiple up of alignment.
     * If the value equals an alignment multiple then it is returned unchanged.
     *
     * This method executes without branching.
     *
     * @param value to be aligned up.
     * @param alignment to use.
     * @return the value aligned to the next boundary.
     */
    public static long align(final long value, final long alignment)
    {
        return (value + (alignment - 1)) & ~(alignment - 1);
    }

    /**
     * Generate a byte array from the hex representation of the given byte array.
     *
     * @param buffer to convert from a hex representation (in Big Endian)
     * @return new byte array that is decimal representation of the passed array
     */
    public static byte[] fromHexByteArray(final byte[] buffer) {
        
        final byte[] outputBuffer = new byte[buffer.length >> 1];

        for (int i = 0; i < buffer.length; i += 2)
        {
            outputBuffer[i >> 1] = (byte) ((FROM_HEX_DIGIT_TABLE[buffer[i]] << 4) | FROM_HEX_DIGIT_TABLE[buffer[i + 1]]);
        }

        return outputBuffer;
    }

    /**
     * Generate a byte array that is a hex representation of a given byte array.
     *
     * @param buffer to convert to a hex representation
     * @return new byte array that is hex representation (in Big Endian) of the passed array
     */
    public static byte[] toHexByteArray(final byte[] buffer)
    {
        return toHexByteArray(buffer, 0, buffer.length);
    }

    /**
     * Generate a byte array that is a hex representation of a given byte array.
     *
     * @param buffer to convert to a hex representation
     * @param index  into the buffer
     * @param length number of bytes to convert
     * @return new byte array that is hex representation (in Big Endian) of the passed array
     */
    public static byte[] toHexByteArray(final byte[] buffer, final int index, final int length)
    {
        final byte[] outputBuffer = new byte[length << 1];

        for (int i = 0; i < (length << 1); i += 2)
        {
            final byte b = buffer[index + (i >> 1)]; // readability

            outputBuffer[i] = HEX_DIGIT_TABLE[(b >> 4) & 0x0F];
            outputBuffer[i + 1] = HEX_DIGIT_TABLE[b & 0x0F];
        }
        return outputBuffer;
    }

    /**
     * Generate a byte array from a string that is the hex representation of the given byte array.
     *
     * @param string to convert from a hex representation (in Big Endian)
     * @return new byte array holding the decimal representation of the passed array
     */
    public static byte[] fromHex(final String string)
    {
        return fromHexByteArray(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a string that is the hex representation of a given byte array.
     *
     * @param buffer to convert to a hex representation
     * @return new String holding the hex representation (in Big Endian) of the passed array
     */
    public static String toHex(final byte[] buffer) {
        return toHex(buffer, 0, buffer.length);
    }

    /**
     * Generate a string that is the hex representation of a given byte array.
     *
     * @param buffer to convert to a hex representation
     * @param index to start from
     * @param length of buffer
     * @return new String holding the hex representation (in Big Endian) of the passed array
     */
    public static String toHex(final byte[] buffer, final int index, final int length)
    {
        return new String(toHexByteArray(buffer, index, length), StandardCharsets.UTF_8);
    }

    /**
     * Set the private address of direct {@link java.nio.ByteBuffer}.
     *
     * <b>Note:</b> It is assumed a cleaner is not responsible for reclaiming the memory under this buffer and that
     * the caller is responsible for memory allocation and reclamation.
     *
     * @param byteBuffer to set the address on.
     * @param address to set for the underlying buffer.
     * @param capacity to set for the underlying buffer.
     * @return the modified {@link java.nio.ByteBuffer}
     */
    public static ByteBuffer resetAddressAndCapacity(final ByteBuffer byteBuffer, final long address, final int capacity)
    {
        if (!byteBuffer.isDirect())
        {
            throw new IllegalArgumentException("Can only change address of direct buffers");
        }

        try
        {
            final Field addressField = Buffer.class.getDeclaredField("address");
            addressField.setAccessible(true);
            addressField.set(byteBuffer, address);

            final Field capacityField = Buffer.class.getDeclaredField("capacity");
            capacityField.setAccessible(true);
            capacityField.set(byteBuffer, capacity);

            final Field cleanerField = byteBuffer.getClass().getDeclaredField("cleaner");
            cleanerField.setAccessible(true);
            cleanerField.set(byteBuffer, null);
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }

        return byteBuffer;
    }
}
