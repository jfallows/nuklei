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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.reader;

import java.nio.channels.SocketChannel;

import org.kaazing.nuklei.tcp.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class ReaderProxy
{
    private final OneToOneConcurrentArrayQueue<ReaderCommand> commandQueue;

    public ReaderProxy(Context context)
    {
        this.commandQueue = context.readerCommandQueue();
    }

    public void doRegister(long connectionId, long bindingRef, SocketChannel channel, RingBuffer inputBuffer)
    {
        RegisterCommand response = new RegisterCommand(bindingRef, connectionId, channel, inputBuffer);
        if (!commandQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

}
