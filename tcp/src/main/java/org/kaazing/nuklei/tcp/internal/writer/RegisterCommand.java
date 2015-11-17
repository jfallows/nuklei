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

package org.kaazing.nuklei.tcp.internal.writer;

import java.nio.channels.SocketChannel;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class RegisterCommand implements WriterCommand
{
    private final long bindingRef;
    private final long connectionId;
    private final SocketChannel channel;
    private final RingBuffer outputBuffer;

    public RegisterCommand(
        long bindingRef,
        long connectionId,
        SocketChannel channel,
        RingBuffer outputBuffer)
    {
        this.bindingRef = bindingRef;
        this.connectionId = connectionId;
        this.channel = channel;
        this.outputBuffer = outputBuffer;
    }

    @Override
    public void execute(Writer writer)
    {
        writer.doRegister(bindingRef, connectionId, channel, outputBuffer);
    }
}
