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
package org.kaazing.nuklei.tcp.internal.writer;

import java.nio.channels.SocketChannel;

import org.kaazing.nuklei.tcp.internal.Context;

import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class WriterProxy
{
    private final OneToOneConcurrentArrayQueue<WriterCommand> commandQueue;

    public WriterProxy(Context context)
    {
        this.commandQueue = context.writerCommandQueue();
    }

    public void doCapture(
            long correlationId,
            String source)
    {
        CaptureCommand response = new CaptureCommand(correlationId, source);
        if (!commandQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doUncapture(
            long correlationId,
            String source)
    {
        UncaptureCommand response = new UncaptureCommand(correlationId, source);
        if (!commandQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

    public void doRegister(
        long streamId,
        String source,
        long sourceRef,
        SocketChannel channel)
    {
        RegisterCommand response = new RegisterCommand(streamId, source, sourceRef, channel);
        if (!commandQueue.offer(response))
        {
            throw new IllegalStateException("unable to offer command");
        }
    }

}
