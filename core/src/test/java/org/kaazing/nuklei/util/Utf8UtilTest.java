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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.kaazing.nuklei.util.Utf8Util.initialDecodeUTF8;
import static org.kaazing.nuklei.util.Utf8Util.remainingBytesUTF8;
import static org.kaazing.nuklei.util.Utf8Util.remainingDecodeUTF8;
import static uk.co.real_logic.agrona.BitUtil.fromHex;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class Utf8UtilTest
{

    @Test
    public void shouldDecode3ByteChar() throws Exception
    {

        byte[] bytes = new byte[]
        {(byte) 0xe8, (byte) 0xaf, (byte) 0x85};

        int remainingBytes = remainingBytesUTF8(bytes[0]);
        assertEquals(2, remainingBytes);

        int bytesOffset = 0;
        int codePoint = initialDecodeUTF8(remainingBytes, bytes[bytesOffset++]);
        while (remainingBytes > 0)
        {
            switch (remainingBytes)
            {
            case 1:
                codePoint = remainingDecodeUTF8(codePoint, remainingBytes--, bytes[bytesOffset++]);
                assertEquals(0x8bc5, new String(new int[]
                {codePoint}, 0, 1).codePointAt(0));
                break;
            default:
                codePoint = remainingDecodeUTF8(codePoint, remainingBytes--, bytes[bytesOffset++]);
                break;
            }
        }
    }

    public void shouldDecode4ByteChar() throws Exception
    {
        byte[] bytes = new byte[]
        {(byte) 0xf1, (byte) 0x88, (byte) 0xb5, (byte) 0x92};

        int remainingBytes = remainingBytesUTF8(bytes[0]);
        assertEquals(3, remainingBytes);

        int bytesOffset = 0;
        int codePoint = initialDecodeUTF8(remainingBytes, bytes[bytesOffset++]);
        while (remainingBytes > 0)
        {
            switch (remainingBytes)
            {
            case 1:
                codePoint = remainingDecodeUTF8(codePoint, remainingBytes--, bytes[bytesOffset++]);
                assertEquals(0x48d52, new String(new int[]
                {codePoint}, 0, 1).codePointAt(0));
                break;
            default:
                codePoint = remainingDecodeUTF8(codePoint, remainingBytes--, bytes[bytesOffset++]);
                break;
            }
        }
    }

    @Test(expected = IOException.class)
    public void shouldNotDecode5ByteChar() throws Exception
    {
        byte[] bytes = new byte[]
        {(byte) 0b11111101, (byte) 0x88, (byte) 0xb5, (byte) 0x92, (byte) 0x88};

        int remainingBytes = remainingBytesUTF8(bytes[0]);
        assertEquals(5, remainingBytes);

        int bytesOffset = 0;
        initialDecodeUTF8(remainingBytes, bytes[bytesOffset++]);
    }

    @Test
    public void validateUTF8ShouldAcceptValidUTF8() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 7;
        bytes.position(offset);
        bytes.put("e acute (0xE9 or 0x11101001): ".getBytes(UTF_8));
        bytes.put((byte) 0b11000011).put((byte) 0b10101001);
        bytes.put(", Euro sign: ".getBytes(UTF_8));
        bytes.put(fromHex("e282ac"));
        bytes.put(", Hwair: ".getBytes(UTF_8));
        bytes.put(fromHex("f0908d88"));
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(0, result);
    }

    @Test
    public void validateUTF8ShouldRejectByteC0() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 0;
        bytes.position(offset);
        bytes.put((byte) 0xC0);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    public void validateUTF8ShouldRejectByteC1() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 0;
        bytes.position(offset);
        bytes.put((byte) 0xC1);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    public void validateUTF8ShouldAcceptLeadingBytef4() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 0;
        bytes.position(offset);
        bytes.put((byte) 0xf4);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(1, result); // incomplete character, only 1 byte present
    }

    @Test
    public void validateUTF8ShouldRejectLeadingByteGTf4() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 0;
        bytes.position(offset);
        bytes.put((byte) 0xf5);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    // This is actually same as reject C1 but it's a more realistic case
    public void validateUTF8ShouldRejectOverwide2ByteCharacter() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 17;
        bytes.position(offset);
        bytes.put("Invalid over wide character P (0x50 = 0b101000): ".getBytes(UTF_8));
        bytes.put((byte) 0b11000001).put((byte) 0b1001000);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    public void validateUTF8ShouldRejectOverwide3ByteCharacter() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 17;
        bytes.position(offset);
        bytes.put("Invalid over wide U+11Cx (0b100011100) Hangul (Korean) character): ".getBytes(UTF_8));
        bytes.put((byte) 0b11100000);
        bytes.put((byte) 0b10000100);
        bytes.put((byte) 0b10011100);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    // This is actually same as reject C1 but it's a more realistic case
    public void validateUTF8ShouldRejectOverwide4ByteCharacter() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 17;
        bytes.position(offset);
        bytes.put("Invalid over wide Euro character 0xf08282ac): ".getBytes(UTF_8));
        bytes.put(fromHex("f08282ac"));
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    public void validateUTF8ShouldRejectInvalidContinuationByte() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 0;
        bytes.position(offset);
        bytes.put((byte) 0b11000011);
        bytes.put((byte) 0b11001001);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    public void validateUTF8ShouldRejectCodePointOver0x10FFFF() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 7;
        bytes.position(offset);
        bytes.put("Invalid 5 byte character: ".getBytes(UTF_8));
        bytes.put((byte) 0b11110100);
        bytes.put((byte) 0b10011111);
        bytes.put((byte) 0b10111111);
        bytes.put((byte) 0b10111111);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    public void validateUTF8ShouldReject5byteCharacter() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 7;
        bytes.position(offset);
        bytes.put("Invalid 5 byte character: ".getBytes(UTF_8));
        bytes.put((byte) 0b11111011);
        bytes.put((byte) 0b11000001);
        bytes.put((byte) 0b10000001);
        bytes.put((byte) 0b10000001);
        bytes.put((byte) 0b10000001);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    public void validateUTF8ShouldReject6byteCharacter() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 7;
        bytes.position(offset);
        bytes.put("Invalid 6 byte character: ".getBytes(UTF_8));
        bytes.put((byte) 0b11111101);
        bytes.put((byte) 0b11000001);
        bytes.put((byte) 0b10000001);
        bytes.put((byte) 0b10000001);
        bytes.put((byte) 0b10000001);
        bytes.put((byte) 0b10000001);
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(-1, result);
    }

    @Test
    public void validateUTF8ShouldReturnIncompleteCharacterByteCount1() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 7;
        bytes.position(offset);
        bytes.put("e acute (0xE9 or 0x11101001): ".getBytes(UTF_8));
        bytes.put((byte) 0b11000011).put((byte) 0b10101001);
        bytes.put(", Euro sign: ".getBytes(UTF_8));
        bytes.put(fromHex("e2")); // missing final 82ac
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(1, result);
    }

    @Test
    public void validateUTF8ShouldReturnIncompleteCharacterByteCount2() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 7;
        bytes.position(offset);
        bytes.put("e acute (0xE9 or 0x11101001): ".getBytes(UTF_8));
        bytes.put((byte) 0b11000011).put((byte) 0b10101001);
        bytes.put(", Euro sign: ".getBytes(UTF_8));
        bytes.put(fromHex("e282")); // missing final ac
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(2, result);
    }

    @Test
    public void validateUTF8ShouldReturnIncompleteCharacterByteCount3() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        int offset = 7;
        bytes.position(offset);
        bytes.put("e acute (0xE9 or 0x11101001): ".getBytes(UTF_8));
        bytes.put((byte) 0b11000011).put((byte) 0b10101001);
        bytes.put(", Euro sign: ".getBytes(UTF_8));
        bytes.put(fromHex("e282ac"));
        bytes.put(", Hwair: ".getBytes(UTF_8));
        bytes.put(fromHex("f0908d")); // final 88 missing
        bytes.limit(bytes.position());
        bytes.position(offset);
        MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
        int result = Utf8Util.validateUTF8(buffer, offset, bytes.limit() - offset, (message) ->
        {
            System.out.println(message);
        });
        assertEquals(3, result);
    }

}
