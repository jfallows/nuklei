/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
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

package org.kaazing.nuklei.tcp.internal;

import static uk.co.real_logic.agrona.BitUtil.toHex;

import java.nio.ByteOrder;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Conductor implements Agent
{
    private final RingBuffer toNukleusCommands;
    private final MessageHandler onNukleusCommandFunc;
    private final BroadcastTransmitter toControllerResponses;

    Conductor(Context context)
    {
        this.toNukleusCommands = context.toNukleusCommands();
        this.onNukleusCommandFunc = this::onNukleusCommand;
        this.toControllerResponses = context.toControllerResponses();
    }

    @Override
    public int doWork() throws Exception
    {
        int workCount = 0;

        workCount += toNukleusCommands.read(onNukleusCommandFunc);

        return workCount;
    }

    @Override
    public String roleName()
    {
        return "conductor";
    }

    private void onNukleusCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        // TODO: state machine
        System.out.println("msgTypeId " + msgTypeId);
        byte[] dst = new byte[length];
        buffer.getBytes(index, dst);
        System.out.println("payload " + toHex(dst));

        UnsafeBuffer responseBuffer = new UnsafeBuffer(new byte[1024]);
        long correlationId = buffer.getLong(index);
        responseBuffer.putLong(0, correlationId);
        responseBuffer.putByte(0x08, (byte) 0x07);
        responseBuffer.putStringWithoutLengthUtf8(0x09, "nukleus");
        responseBuffer.putInt(0x10, 0x01, ByteOrder.BIG_ENDIAN);

        toControllerResponses.transmit(0x40000001, responseBuffer, 0, 0x14);
    }
}
