/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.ws.internal.types.stream;

import static java.lang.Integer.highestOneBit;

import org.kaazing.nuklei.ws.internal.types.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class WsFrameFW extends Flyweight
{
    private static final int FIELD_OFFSET_FLAGS_AND_OPCODE = 0;
    private static final int FIELD_SIZE_FLAGS_AND_OPCODE = BitUtil.SIZE_OF_BYTE;

    private static final int FIELD_OFFSET_MASK_AND_LENGTH = FIELD_OFFSET_FLAGS_AND_OPCODE + FIELD_SIZE_FLAGS_AND_OPCODE;

    private static final int FIELD_SIZE_MASKING_KEY = BitUtil.SIZE_OF_INT;

    private final AtomicBuffer payloadRO = new UnsafeBuffer(new byte[0]);

    public boolean fin()
    {
        return (buffer().getByte(offset() + FIELD_OFFSET_FLAGS_AND_OPCODE) & 0x80) != 0x00;
    }

    public boolean rsv1()
    {
        return (buffer().getByte(offset() + FIELD_OFFSET_FLAGS_AND_OPCODE) & 0x40) != 0x00;
    }

    public boolean rsv2()
    {
        return (buffer().getByte(offset() + FIELD_OFFSET_FLAGS_AND_OPCODE) & 0x20) != 0x00;
    }

    public boolean rsv3()
    {
        return (buffer().getByte(offset() + FIELD_OFFSET_FLAGS_AND_OPCODE) & 0x10) != 0x00;
    }

    public int opcode()
    {
        return buffer().getByte(offset() + FIELD_OFFSET_FLAGS_AND_OPCODE) & 0x0f;
    }

    public boolean mask()
    {
        return (buffer().getByte(offset() + FIELD_OFFSET_MASK_AND_LENGTH) & 0x80) != 0;
    }

    public int maskingKey()
    {
        return buffer().getInt(offset() + FIELD_OFFSET_FLAGS_AND_OPCODE + FIELD_SIZE_FLAGS_AND_OPCODE + lengthSize());
    }

    public DirectBuffer payload()
    {
        return payloadRO;
    }

    @Override
    public int limit()
    {
        return payloadOffset() + lengthValue();
    }

    public WsFrameFW wrap(DirectBuffer buffer, int offset, int maxLimit)
    {
        super.wrap(buffer, offset, maxLimit);

        int lengthValue = lengthValue();
        if (lengthValue != 0)
        {
            payloadRO.wrap(buffer, payloadOffset(), lengthValue);
        }

        checkLimit(limit(), maxLimit);

        return this;
    }

    @Override
    public String toString()
    {
        return String.format("[fin=%s, opcode=%d, payload.length=%d]", fin(), opcode(), lengthValue());
    }

    private int payloadOffset()
    {
        int payloadOffset = offset() + FIELD_SIZE_FLAGS_AND_OPCODE + lengthSize();

        if (mask())
        {
            payloadOffset += FIELD_SIZE_MASKING_KEY;
        }

        return payloadOffset;
    }

    private int lengthSize()
    {
        switch (buffer().getByte(offset() + FIELD_OFFSET_MASK_AND_LENGTH) & 0x7f)
        {
        case 0x7e:
            return 3;

        case 0x7f:
            return 9;

        default:
            return 1;
        }
    }

    private int lengthValue()
    {
        int length = buffer().getByte(offset() + FIELD_OFFSET_MASK_AND_LENGTH) & 0x7f;

        switch (length)
        {
        case 0x7e:
            return buffer().getShort(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1) & 0xffff;

        case 0x7f:
            long length8bytes = buffer().getLong(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1);
            if (length8bytes >> 17L != 0L)
            {
                throw new IllegalStateException("frame payload too long");
            }
            return (int) length8bytes & 0xffffffff;

        default:
            return length;
        }
    }

    public static final class Builder extends Flyweight.Builder<WsFrameFW>
    {
        public Builder()
        {
            super(new WsFrameFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            return this;
        }

        public Builder flagsAndOpcode(int flagsAndOpcode)
        {
            buffer().putByte(offset() + FIELD_OFFSET_FLAGS_AND_OPCODE, (byte) flagsAndOpcode);
            return this;
        }

        public Builder payload(DirectBuffer buffer)
        {
            return payload(buffer, 0, buffer.capacity());
        }

        public Builder payload(DirectBuffer buffer, int offset, int length)
        {
            switch (highestOneBit(length))
            {
            case 0:
            case 1:
            case 2:
            case 4:
            case 8:
            case 16:
            case 32:
                buffer().putByte(offset() + FIELD_OFFSET_MASK_AND_LENGTH, (byte) length);
                buffer().putBytes(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1, buffer, offset, length);
                limit(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1 + length);
                break;
            case 64:
                switch (length)
                {
                case 126:
                case 127:
                    buffer().putByte(offset() + FIELD_OFFSET_MASK_AND_LENGTH, (byte) 126);
                    buffer().putShort(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1, (short) length);
                    buffer().putBytes(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 3, buffer, offset, length);
                    limit(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 3 + length);
                    break;
                default:
                    buffer().putByte(offset() + FIELD_OFFSET_MASK_AND_LENGTH, (byte) length);
                    buffer().putBytes(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1, buffer, offset, length);
                    limit(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1 + length);
                    break;
                }
                break;
            case 132:
                buffer().putByte(offset() + FIELD_OFFSET_MASK_AND_LENGTH, (byte) 126);
                buffer().putShort(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1, (short) length);
                buffer().putBytes(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 3, buffer, offset, length);
                limit(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 3 + length);
                break;
            default:
                buffer().putByte(offset() + FIELD_OFFSET_MASK_AND_LENGTH, (byte) 127);
                buffer().putLong(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 1, length);
                buffer().putBytes(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 9, buffer, offset, length);
                limit(offset() + FIELD_OFFSET_MASK_AND_LENGTH + 9 + length);
                break;
            }

            return this;
        }
    }
}
