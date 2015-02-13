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

import org.kaazing.nuklei.FlyweightBE;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class Frame extends FlyweightBE
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

    private Frame()
    {
    }

    @Override
    public Frame wrap(MutableDirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset);
        validate();
        return this;
    }

    public boolean isFin()
    {
        return (byte0() & FIN_MASK) != 0;
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

    public int byte0()
    {
        return uint8Get(buffer(), offset());
    }

    private static void protocolError(String message)
    {
        // TODO: generalized protocol error handling
        throw new IllegalArgumentException(message);
    }

    private void validate()
    {
        if ((byte0() & RSV_BITS_MASK) != 0)
        {
            protocolError("Reserved bits are set in first byte");
        }
        OpCode opcode = getOpCode();

        // TODO: State machine should validate against maximumWsMessageSize, boolean previousFrameFin, boolean fromClient,
        // to achieve the following:
//        switch (opcode)
//        {
//        case CONTINUATION:
//            if (previousFrameFin)
//            {
//                protocolError("Not expecting CONTINUATION frame");
//            }
//            break;
//
//        case TEXT:
//        case BINARY:
//            if (!previousFrameFin)
//            {
//                protocolError("Expecting CONTINUATION frame, but got " + opcode + " frame");
//            }
//            boolean masked = (byte0() & MASKED_MASK) != 0;
//            if (fromClient && !masked) {
//                protocolError("WebSocket frame from client must be masked");
//            }
//            if (!fromClient && masked) {
//                protocolError("WebSocket frame from server must not be masked");
//            }
//            break;
//
//        case PING:
//        case PONG:
//        case CLOSE:
//            if (!isFin())
//            {
//                protocolError("Expected FIN for " + opcode + " frame");
//            }
//            break;
//        }
    }

}
