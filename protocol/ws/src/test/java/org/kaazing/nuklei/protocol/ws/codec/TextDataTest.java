/*
 * Copyright 2015 Kaazing Corporation, All rights reserved.
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
package org.kaazing.nuklei.protocol.ws.codec;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static uk.co.real_logic.agrona.BitUtil.fromHex;

import java.nio.ByteBuffer;

import org.junit.experimental.theories.Theory;
import org.kaazing.nuklei.protocol.ws.codec.Frame.Payload;

public class TextDataTest extends FrameTest
{

    @Theory
    public void shouldDecodeWithEmptyPayload(int offset, boolean masked) throws Exception
    {
        buffer.putBytes(offset, fromHex("81"));
        putLengthMaskAndHexPayload(buffer, offset + 1, null, masked);
        Frame frame = frameFactory.wrap(buffer, offset);
        assertEquals(OpCode.TEXT, frame.getOpCode());
        Data data = (Data) frame;
        Payload payload = frame.getPayload();
        assertEquals(payload.offset(), payload.limit());
        assertEquals(0, data.getLength());
    }

    @Theory
    public void shouldDecodeWithValidPayload(int offset, boolean masked) throws Exception
    {
        buffer.putBytes(offset, fromHex("81"));
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        bytes.position(offset);
        bytes.put("e acute (0xE9 or 0x11101001): ".getBytes(UTF_8));
        bytes.put((byte) 0b11000011).put((byte) 0b10101001);
        bytes.put(", Euro sign: ".getBytes(UTF_8));
        bytes.put(fromHex("e282ac"));
        bytes.put(", Hwair: ".getBytes(UTF_8));
        bytes.put(fromHex("f0908d88"));
        bytes.limit(bytes.position());
        bytes.position(offset);
        byte[] inputPayload = new byte[bytes.remaining()];
        bytes.get(inputPayload);
        putLengthMaskAndPayload(buffer, offset + 1, inputPayload, masked);
        Frame frame = frameFactory.wrap(buffer, offset);
        assertEquals(OpCode.TEXT, frame.getOpCode());
        Payload payload = frame.getPayload();
        byte[] payloadBytes = new byte[payload.limit() - payload.offset()];
        payload.buffer().getBytes(payload.offset(), payloadBytes);
        assertArrayEquals(inputPayload, payloadBytes);
        Data data = (Data) frame;
        assertEquals(inputPayload.length, data.getLength());
    }

    @Theory
    public void shouldDecodeDataFrameWithIncompleteUTF8(int offset, boolean masked) throws Exception
    {
        buffer.putBytes(offset, fromHex("81"));
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        bytes.position(offset);
        bytes.put("e acute (0xE9 or 0x11101001): ".getBytes(UTF_8));
        bytes.put((byte) 0b11000011).put((byte) 0b10101001);
        bytes.put(", Euro sign: ".getBytes(UTF_8));
        bytes.put(fromHex("e282ac"));
        bytes.put(", Hwair (first 2 bytes only): ".getBytes(UTF_8));
        bytes.put(fromHex("f090")); // missing 8d88
        bytes.limit(bytes.position());
        bytes.position(offset);
        byte[] inputPayload = new byte[bytes.remaining()];
        bytes.get(inputPayload);
        putLengthMaskAndPayload(buffer, offset + 1, inputPayload, masked);
        Frame frame = frameFactory.wrap(buffer, offset);
        assertEquals(OpCode.TEXT, frame.getOpCode());
        Payload payload = frame.getPayload();
        byte[] payloadBytes = new byte[payload.limit() - payload.offset()];
        payload.buffer().getBytes(payload.offset(), payloadBytes);
        assertArrayEquals(inputPayload, payloadBytes);
        Data data = (Data) frame;
        assertEquals(inputPayload.length, data.getLength());
    }

    @Theory
    public void shouldrejectDataFrameWithInvalidUTF8(int offset, boolean masked) throws Exception
    {
        buffer.putBytes(offset, fromHex("81"));
        ByteBuffer bytes = ByteBuffer.allocate(1000);
        bytes.position(offset);
        bytes.put("e acute (0xE9 or 0x11101001): ".getBytes(UTF_8));
        bytes.put((byte) 0b11000011).put((byte) 0b10101001);
        bytes.put(", invalid: ".getBytes(UTF_8));
        bytes.put(fromHex("ff"));
        bytes.put(", Euro sign: ".getBytes(UTF_8));
        bytes.put(fromHex("e282ac"));
        bytes.limit(bytes.position());
        bytes.position(offset);
        byte[] inputPayload = new byte[bytes.remaining()];
        bytes.get(inputPayload);
        putLengthMaskAndPayload(buffer, offset + 1, inputPayload, masked);
        Frame frame = frameFactory.wrap(buffer, offset);
        assertEquals(OpCode.TEXT, frame.getOpCode());
        Data data = (Data) frame;
        assertEquals(inputPayload.length, data.getLength());
        try
        {
            data.getPayload();
        }
        catch(ProtocolException e)
        {
            return;
        }
        fail("Exception was not thrown");
    }
}
