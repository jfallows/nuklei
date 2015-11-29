/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.tcp.internal.reader;

import java.nio.channels.SocketChannel;

import org.kaazing.nuklei.tcp.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class ReaderProxy
{
    private final OneToOneConcurrentArrayQueue<ReaderCommand> commandQueue;

    public ReaderProxy(Context context)
    {
        this.commandQueue = context.readerCommandQueue();
    }

    public void doCapture(
        long correlationId,
        String handler)
    {
        CaptureCommand command = new CaptureCommand(correlationId, handler);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doUncapture(
        long correlationId,
        String handler)
    {
        UncaptureCommand command = new UncaptureCommand(correlationId, handler);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doRegister(
        String handler,
        long handlerRef,
        long clientStreamId,
        long serverStreamId,
        SocketChannel channel)
    {
        RegisterCommand command = new RegisterCommand(handler, handlerRef, clientStreamId, serverStreamId, channel);
        if (!commandQueue.offer(command))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

}
