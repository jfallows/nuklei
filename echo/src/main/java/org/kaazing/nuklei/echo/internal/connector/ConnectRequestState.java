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
package org.kaazing.nuklei.echo.internal.connector;

import java.nio.channels.SocketChannel;

public class ConnectRequestState
{
    private final ConnectorState owner;
    private final long correlationId;
    private final SocketChannel channel;

    public ConnectRequestState(
        ConnectorState owner,
        long correlationId,
        SocketChannel channel)
    {
        this.owner = owner;
        this.correlationId = correlationId;
        this.channel = channel;
    }

    public ConnectorState owner()
    {
        return owner;
    }

    public long correlationId()
    {
        return correlationId;
    }

    public SocketChannel channel()
    {
        return channel;
    }

    @Override
    public String toString()
    {
        return String.format("[owner=%s, correlationId=%d, channel=%s]", owner, correlationId, channel);
    }
}
