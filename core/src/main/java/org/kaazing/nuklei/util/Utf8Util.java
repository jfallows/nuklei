/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.nuklei.util;

import static java.lang.Character.charCount;
import static java.lang.Character.codePointAt;
import static java.lang.String.format;

import java.io.IOException;

import org.kaazing.nuklei.ErrorHandler;

import uk.co.real_logic.agrona.DirectBuffer;

public final class Utf8Util
{

    public static final int INVALID_UTF8 = -1;

    private Utf8Util()
    {
    }

    public static int byteCountUTF8(char[] cbuf, int offset, int length) throws IOException
    {
        int count = 0;
        while (offset < length)
        {
            int codePoint = codePointAt(cbuf, offset);
            count += byteCountUTF8(codePoint);
            offset += charCount(codePoint);
        }
        return count;
    }

    public static int byteCountUTF8(int codePoint)
    {
        if ((codePoint | 0x7f) == 0x7f)
        {
            return 1;
        }
        else if ((codePoint | 0x07ff) == 0x07ff)
        {
            return 2;
        }
        else if ((codePoint | 0xffff) == 0xffff)
        {
            return 3;
        }
        else if ((codePoint | 0x1fffff) == 0x1fffff)
        {
            return 4;
        }
        else
        {
            return -1;
        }
    }

    public static int initialDecodeUTF8(int remainingWidth, int encodedByte) throws IOException
    {
        switch (remainingWidth)
        {
        case 0:
            return encodedByte & 0x7f;
        case 1:
            return encodedByte & 0x1f;
        case 2:
            return encodedByte & 0x0f;
        case 3:
            return encodedByte & 0x07;
        default:
            throw new IOException("Invalid UTF-8 byte sequence. UTF-8 char cannot span for more than 4 bytes.");
        }
    }

    public static int remainingDecodeUTF8(int decodedBytes, int remainingWidth, int encodedByte) throws IOException
    {
        switch (remainingWidth)
        {
        case 3:
        case 2:
        case 1:
            return (decodedBytes << 6) | (encodedByte & 0x3f);
        case 0:
            return decodedBytes;
        default:
            throw new IOException("Invalid UTF-8 byte sequence. UTF-8 char cannot span for more than 4 bytes.");
        }
    }

    public static int remainingBytesUTF8(int leadingByte)
    {
        if ((leadingByte & 0x80) == 0)
        {
            return 0;
        }

        for (byte i = 0; i < 7; i++)
        {
            int bitMask = 1 << (7 - i);

            if ((leadingByte & bitMask) != 0)
            {
                continue;
            }
            else
            {
                switch (i)
                {
                case 0:
                case 7:
                    throw new IllegalStateException(format("Invalid UTF-8 sequence leader byte: 0x%02x", leadingByte));
                default:
                    return i - 1;
                }
            }
        }

        throw new IllegalStateException(String.format("Invalid UTF-8 sequence leader byte: 0x%02x", leadingByte));
    }

    public static boolean validBytesUTF8(byte[] input)
    {
        for (int index = 0; index < input.length;)
        {
            byte leadingByte = input[index++];
            if ((leadingByte & 0xc0) == 0x80)
            {
                return false;
            }
            int remaining = remainingBytesUTF8(leadingByte);
            switch (remaining)
            {
            case 0:
                break;
            default:
                while (remaining-- > 0)
                {
                    if ((input[index++] & 0xc0) != 0x80)
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * @param buffer
     * @param offset Position where the byte sequence to be validated starts
     * @param length Number of bytes to validate
     * @return <li>0: byte sequence is a complete sequence of valid UTF-8 characters
     *         <li>N=1, 2 or 3: byte sequence is valid UTF-8 but the last N bytes are an incomplete UTF-8 character
     *         <li>INVALID_UTF8 (-1): byte sequence is not valid UTF-8
     */
    public static int validateUTF8(DirectBuffer buffer, int offset, int length, ErrorHandler errorHandler)
    {
        for (int index = 0; index < length; index++)
        {
            byte leadingByte = buffer.getByte(offset + index);
            final int expectedLen;
            int codePoint;
            if ((leadingByte & 0b10000000) == 0b00000000)
            {
                continue;
            }
            if ((leadingByte & 0xff) > 0xf4)
            {
                errorHandler.handleError(format("Invalid leading byte: %x", leadingByte));
                return INVALID_UTF8;
            }
            if ((leadingByte & 0b11100000) == 0b11000000)
            {
                expectedLen = 2;
                codePoint = (leadingByte & 0b00011111);
                if (codePoint < 2)
                {
                    errorHandler.handleError(format("Overlong encoding: %x%x", leadingByte,
                            buffer.getByte(offset + index + 1)));
                    return INVALID_UTF8;
                }
            }
            else if ((leadingByte & 0b11110000) == 0b11100000)
            {
                expectedLen = 3;
                codePoint = (leadingByte & 0b00001111);
            }
            else if ((leadingByte & 0b11111000) == 0b11110000)
            {
                expectedLen = 4;
                codePoint = (leadingByte & 0b00000111);
            }
            else
            {
                errorHandler.handleError(format("Value exceeds Unicode limit: %x", leadingByte));
                return INVALID_UTF8;
            }
            int characterStartIndex = index;
            int remainingLen = expectedLen;
            while (--remainingLen > 0)
            {
                if (++index >= length)
                {
                    // incomplete character at end
                    return length - characterStartIndex;
                }
                byte nextByte = buffer.getByte(offset + index);
                if ((nextByte & 0b11000000) != 0b10000000)
                {
                    errorHandler.handleError(format("Invalid continuation byte: %x", nextByte));
                    return INVALID_UTF8;
                }
                codePoint = ((codePoint << 6) | (nextByte & 0b00111111));
                if (codePoint > 0x10FFFF) // maximum Unicode code point
                {
                    return INVALID_UTF8;
                }
            }
            if (expectedLen > byteCountUTF8(codePoint))
            {
                errorHandler.handleError(format("Overlong encoding starting at byte %x postion %d", leadingByte,
                        characterStartIndex));
                return INVALID_UTF8;
            }
        }
        return 0;
    }
}
