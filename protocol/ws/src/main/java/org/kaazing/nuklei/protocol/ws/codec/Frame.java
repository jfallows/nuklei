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

import static java.lang.String.format;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.FlyweightBE;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public abstract class Frame extends FlyweightBE
{
    private static final byte FIN_MASK = (byte) 0b10000000;
    private static final byte RSV_BITS_MASK = 0b01110000;
    private static final byte OP_CODE_MASK = 0x0F;
    private static final byte MASKED_MASK = (byte) 0b10000000;
    private static final byte LENGTH_BYTE_1_MASK = 0b01111111;
    private static final int LENGTH_OFFSET = 1;
    private static final int MASK_OFFSET = 1;

    private MutableDirectBuffer unmaskedPayload;
    private final Payload payload = new Payload();

    Frame()
    {
    }

    public OpCode getOpCode()
    {
        return getOpCode(buffer(), offset());
    }

    public int getLength()
    {
        int offset = offset() + LENGTH_OFFSET;
        int length = uint8Get(buffer(), offset) & LENGTH_BYTE_1_MASK;

        switch (length)
        {
        case 126:
            return uint16Get(buffer(), offset + 1);
        case 127:
            return (int) int64Get(buffer(), offset + 1);
        default:
            return length;
        }
    }

    public Payload getPayload()
    {
        return getPayload(false);
    }

    public boolean isFin()
    {
        return (byte0() & FIN_MASK) != 0;
    }

    public boolean isMasked()
    {
        return (uint8Get(buffer(), offset() + MASK_OFFSET) & MASKED_MASK) != 0;
    }

    public int limit()
    {
        return getDataOffset() + getLength();
    }

    // TODO: move this to state machine
    public void validate()
    {
        if ((byte0() & RSV_BITS_MASK) != 0)
        {
            protocolError("Reserved bits are set in first byte");
        }
        validateLength();
    }

    public static class Payload extends FlyweightBE
    {
        private int limit;

        protected Payload wrap(DirectBuffer buffer, int offset, int limit, boolean mutable)
        {
            super.wrap(buffer, offset, false);
            this.limit = limit;
            return this;
        }

        public int limit()
        {
            return limit;
        }
    }

    /**
     * TODO: state machine should validate the following: <li>If this is a Continuation frame: previous frame's fin must
     * not have been set <li>If this is not a Continuation frame: previous frame's fin must have been set <li>If from
     * client: must be masked (and vice versa) <li>If from server: must not be masked (and vice versa)
     */
    @Override
    protected Flyweight wrap(final DirectBuffer buffer, final int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        validateLength(); // this must be done before unmasking payload, doing here for consistent behavior
        payload.wrap(null, offset, 0, mutable);
        return this;
    }

    int getDataOffset()
    {
        int offset = offset() + LENGTH_OFFSET;
        int lengthByte1 = uint8Get(buffer(), offset) & LENGTH_BYTE_1_MASK;
        offset += 1;
        switch (lengthByte1)
        {
        case 126:
            offset += 2;
            break;
        case 127:
            offset += 8;
            break;
        default:
            break;
        }
        if (isMasked())
        {
            offset += 4;
        }
        return offset;
    }

    abstract int getMaxPayloadLength();

    static OpCode getOpCode(DirectBuffer buffer, int offset)
    {
        return OpCode.fromInt(uint8Get(buffer, offset) & OP_CODE_MASK);
    }

    Payload getPayload(boolean mutable)
    {
        if (payload.buffer() != null)
        {
            return payload;
        }
        if (!isMasked())
        {
            payload.wrap(buffer(), getDataOffset(), limit(), false);
        }
        else
        {
            if (unmaskedPayload == null)
            {
                unmaskedPayload = new UnsafeBuffer(new byte[getMaxPayloadLength()]);
            }
            unmask(unmaskedPayload);
            payload.wrap(unmaskedPayload, 0, getLength(), false);
        }
        return payload;
    }

    static void protocolError(String message) throws ProtocolException
    {
        throw new ProtocolException(message);
    }

    private int byte0()
    {
        return uint8Get(buffer(), offset());
    }

    private void unmask(MutableDirectBuffer unmaskedPayload)
    {
        if (!isMasked())
        {
            return;
        }
        int dataOffset = getDataOffset();
        int maskOffset = dataOffset - 4;
        int mask = int32Get(buffer(), maskOffset);
        // xor a 32bit word at a time as long as possible then do remaining 0, 1, 2 or 3 bytes
        int i;
        for (i = 0; i+4 < getLength(); i+=4)
        {
            int unmasked = int32Get(buffer(), dataOffset + i) ^ mask;
            int32Put(unmaskedPayload, i, unmasked);
        }
        for (; i < getLength(); i++)
        {
            int shiftBytes = 3 - (i & 0x03);
            byte maskByte = (byte) (mask >> (8 * shiftBytes) & 0xFF);
            byte unmasked = (byte) (buffer().getByte(dataOffset + i) ^ (maskByte));
            unmaskedPayload.setMemory(i, 1, unmasked);
        }
    }

    private void validateLength()
    {
        if (getLength() > getMaxPayloadLength())
        {
            protocolError(format("%s frame payload exceeds %d bytes", getOpCode(), getMaxPayloadLength()));
        }
    }

}
