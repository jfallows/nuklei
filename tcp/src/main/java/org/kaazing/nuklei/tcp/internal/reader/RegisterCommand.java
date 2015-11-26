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

public final class RegisterCommand implements ReaderCommand
{
    private final long streamId;
    private final String handler;
    private final long handlerRef;
    private final SocketChannel channel;

    public RegisterCommand(
        long streamId,
        String handler,
        long handlerRef,
        SocketChannel channel)
    {
        this.streamId = streamId;
        this.handler = handler;
        this.handlerRef = handlerRef;
        this.channel = channel;
    }

    @Override
    public void execute(Reader reader)
    {
        reader.doRegister(streamId, handler, handlerRef, channel);
    }
}
