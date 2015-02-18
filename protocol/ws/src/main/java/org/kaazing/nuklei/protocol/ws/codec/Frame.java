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

import java.net.ProtocolException;

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
            return uint8Get(buffer(), offset + 1) << 8 | uint8Get(buffer(), offset + 2);
        case 127:
            return uint8Get(buffer(), offset + 1) << 56 | uint8Get(buffer(), offset + 2) << 48
                    | uint8Get(buffer(), offset + 3) << 40 | uint8Get(buffer(), offset + 4) << 32
                    | uint8Get(buffer(), offset + 5) << 24 | uint8Get(buffer(), offset + 6) << 16
                    | uint8Get(buffer(), offset + 7) << 8 | uint8Get(buffer(), offset + 8);
        default:
            return length;
        }
    }

    public Payload getPayload()
    {
        if (getLength() == 0)
        {
            return null;
        }
        if (payload.buffer() != null)
        {
            return payload;
        }
        if (!isMasked())
        {
            payload.setBuffer(buffer());
            payload.setOffset(getDataOffset());
        }
        else
        {
            if (unmaskedPayload == null)
            {
                unmaskedPayload = new UnsafeBuffer(new byte[getMaxPayloadLength()]);
            }
            unmask(unmaskedPayload);
            payload.setBuffer(unmaskedPayload);
            payload.setOffset(0);
        }
        return payload;
    }

    public boolean isFin()
    {
        return (uint8Get(buffer(), offset()) & FIN_MASK) != 0;
    }

    public boolean isMasked()
    {
        return (byte0() & MASKED_MASK) != 0;
    }

    protected static class Payload
    {
        private int offset;
        private DirectBuffer buffer;

        Payload()
        {
        }

        public int offset()
        {
            return offset;
        }

        public DirectBuffer buffer()
        {
            return buffer;
        }

        void setBuffer(DirectBuffer buffer)
        {
            this.buffer = buffer;
        }

        void setOffset(int offset)
        {
            this.offset = offset;
        }
    }

    static OpCode getOpCode(DirectBuffer buffer, int offset)
    {
        return OpCode.fromInt(uint8Get(buffer, offset) & OP_CODE_MASK);
    }

    static void protocolError(String message) throws ProtocolException
    {
        // TODO: generalized protocol error handling
        throw new ProtocolException(message);
    }

    /**
     * TODO: state machine should validate the following: <li>If this is a Continuation frame: previous frame's fin must
     * not have been set <li>If this is not a Continuation frame: previous frame's fin must have been set <li>If from
     * client (presumably this is always the case): must be masked (and vice versa) <li>If from server: must not be
     * masked (and vice versa)
     */
    @Override
    protected Flyweight wrap(final DirectBuffer buffer, final int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        payload.setBuffer(null);
        return this;
    }

    protected abstract int getMaxPayloadLength();

    protected void validate() throws ProtocolException
    {
        if ((byte0() & RSV_BITS_MASK) != 0)
        {
            protocolError("Reserved bits are set in first byte");
        }
    }

    private int byte0()
    {
        return uint8Get(buffer(), offset());
    }

    private int getDataOffset()
    {
        int offset = offset() + LENGTH_OFFSET + 1;
        int lengthByte1 = uint8Get(buffer(), offset) & LENGTH_BYTE_1_MASK;
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

    private void unmask(MutableDirectBuffer unmaskedPayload)
    {
        if (!isMasked())
        {
            return;
        }
        int dataOffset = getDataOffset();
        int maskOffset = dataOffset - 4;
        for (int i = 0; i < getLength(); i++)
        {
            byte unmasked = (byte) (buffer().getByte(dataOffset + i) ^ buffer().getByte(maskOffset + i % 4) & 0xFF);
            unmaskedPayload.setMemory(i, 1, unmasked);
        }
    }

}
