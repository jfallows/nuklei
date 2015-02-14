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

import org.kaazing.nuklei.FlyweightBE;

import uk.co.real_logic.agrona.DirectBuffer;

public class Frame extends FlyweightBE
{

    public static final ThreadLocal<Frame> LOCAL_REF = new ThreadLocal<Frame>()
    {
        @Override
        protected Frame initialValue()
        {
            return new Frame();
        }
    };

    private static final byte FIN_MASK = (byte) 0b10000000;
    private static final byte RSV_BITS_MASK = 0b01110000;
    private static final byte OP_CODE_MASK = 0x0F;
    private static final byte MASKED_MASK = (byte) 0b10000000;
    private static final byte LENGTH_BYTE_1_MASK = 0b01111111;
    private static final int LENGTH_OFFSET = 1;

    private final Close close = new Close();

    Frame()
    {
    }

    public Frame wrap(DirectBuffer buffer, int offset) throws ProtocolException
    {
        validate();
        switch(getOpCode()) {
        case BINARY:
            //return data.wrap(buffer, offset);
            break;
        case CLOSE:
            return close.wrap(buffer, offset);
        case CONTINUATION:
            break;
        case PING:
            break;
        case PONG:
            break;
        case TEXT:
            break;
        default:
            break;
        }
        return this; // TODO: remove this
    }

    @Override
    protected Frame wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, false);
        return this;
    }

    public OpCode getOpCode()
    {
        return OpCode.fromInt(byte0() & OP_CODE_MASK);
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
            return uint8Get(buffer(), offset + 1) << 56 |
                   uint8Get(buffer(), offset + 2) << 48 |
                   uint8Get(buffer(), offset + 3) << 40 |
                   uint8Get(buffer(), offset + 4) << 32 |
                   uint8Get(buffer(), offset + 5) << 24 |
                   uint8Get(buffer(), offset + 6) << 16 |
                   uint8Get(buffer(), offset + 7) << 8  |
                   uint8Get(buffer(), offset + 8);
        default:
            return length;
        }
    }

    public boolean isFin()
    {
        return (byte0() & FIN_MASK) != 0;
    }

    public boolean isMasked()
    {
        return (byte0() & MASKED_MASK) != 0;
    }

    private int byte0()
    {
        return uint8Get(buffer(), offset());
    }

    private static void protocolError(String message) throws ProtocolException
    {
        // TODO: generalized protocol error handling
        throw new ProtocolException(message);
    }

    /**
     * TODO: state machine should validate the following:
     * <li> If this is a Continuation frame: previous frame's fin must not have been set
     * <li> If this is not a Continuation frame: previous frame's fin must have been set
     * <li> If from client (presumably this is always the case): must be masked (and vice versa)
     * <li> If from server: must not be masked (and vice versa)
     */
    private void validate() throws ProtocolException
    {
        if ((byte0() & RSV_BITS_MASK) != 0)
        {
            protocolError("Reserved bits are set in first byte");
        }

        OpCode opcode = getOpCode();
        switch (opcode)
        {
        case PING:
        case PONG:
        case CLOSE:
            if (!isFin())
            {
                protocolError("Expected FIN for " + opcode + " frame");
            }
            break;
        default:
            break;
        }
    }

}
