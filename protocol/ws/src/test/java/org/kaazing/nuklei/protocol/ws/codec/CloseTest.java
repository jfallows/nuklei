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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static uk.co.real_logic.agrona.BitUtil.fromHex;
import static uk.co.real_logic.agrona.BitUtil.toHex;

import java.net.ProtocolException;

import org.junit.experimental.theories.Theory;
import org.kaazing.nuklei.protocol.ws.codec.Frame.Payload;

public class CloseTest extends FrameTest
{

    @Theory
    public void shouldDecodeWithEmptyPayload(int offset, boolean masked) throws Exception {
        buffer.putBytes(offset, fromHex("88"));
        putLengthMaskAndPayload(buffer, offset+1, null, masked);
        Frame frame = frameFactory.wrap(buffer, offset);
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        assertNull(frame.getPayload());
        Close close = (Close)frame;
        assertEquals(0, close.getLength());
        assertEquals(1005, close.getStatusCode());
        assertNull(close.getReason());
    }

    @Theory
    public void shouldDecodeWithStatusCode1000(int offset, boolean masked) throws Exception {
        buffer.putBytes(offset, fromHex("88"));
        putLengthMaskAndPayload(buffer, offset+1, "03e8", masked);
        Frame frame = frameFactory.wrap(buffer, offset);
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        byte[] payloadBytes = new byte[2];
        Payload payload = frame.getPayload();
        payload.buffer().getBytes(payload.offset(), payloadBytes);
        assertArrayEquals(fromHex("03e8"), payloadBytes);
        Close close = (Close)frame;
        assertEquals(2, close.getLength());
        assertEquals(1000, close.getStatusCode());
        assertNull(close.getReason());
    }

    @Theory
    public void shouldDecodeWithStatusCodeAndReason(int offset, boolean masked) throws Exception {
        buffer.putBytes(offset, fromHex("88"));
        String reason = "Something bad happened";
        putLengthMaskAndPayload(buffer, offset+1, "03ff" + toHex(reason.getBytes(UTF_8)), masked);
        Frame frame = frameFactory.wrap(buffer, offset);
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        byte[] payloadBytes = new byte[2];
        Payload payload = frame.getPayload();
        payload.buffer().getBytes(payload.offset(), payloadBytes);
        Close close = (Close)frame;
        assertEquals(2 + reason.length(), close.getLength());
        assertEquals(1023, close.getStatusCode());
        assertEquals(reason, close.getReason());
    }

    @Theory
    public void shouldRejectCloseFrameWithLength1(int offset, boolean masked) throws Exception {
        buffer.putBytes(offset, fromHex("88"));
        putLengthMaskAndPayload(buffer, offset+1, "01", masked);
        try {
            frameFactory.wrap(buffer, offset);
        }
        catch(ProtocolException e) {
            return;
        }
        fail("Exception exception was not thrown");
    }

    @Theory
    public void shouldRejectCloseFrameWithLengthOver125(int offset, boolean masked) throws Exception {
        buffer.putBytes(offset, fromHex("88"));
        putLengthAndMaskBit(buffer, offset+1, 126, masked);
        try {
            frameFactory.wrap(buffer, offset);
        }
        catch(ProtocolException e) {
            return;
        }
        fail("Exception exception was not thrown");
    }

    @Theory
    public void shouldRejectCloseFrameWithReasonNotValidUTF8(int offset, boolean masked) throws Exception {
        buffer.putBytes(offset, fromHex("88"));
        String validMultibyteCharEuroSign = "e282ac";
        String invalidUTF8 = toHex("valid text".getBytes(UTF_8)) + validMultibyteCharEuroSign + "ff";
        putLengthMaskAndPayload(buffer, offset+1, "03ff" + invalidUTF8, masked);
        try {
            frameFactory.wrap(buffer, offset);
        }
        catch(ProtocolException e) {
            return;
        }
        fail("Exception exception was not thrown");
    }
}
