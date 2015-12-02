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
package org.kaazing.nuklei.tcp.internal.connector;

import static java.lang.String.format;

public final class ConnectCommand implements ConnectorCommand
{
    private final String handler;
    private final long handlerRef;
    private final long streamId;

    public ConnectCommand(
        String handler,
        long handlerRef,
        long streamId)
    {
        this.handler = handler;
        this.handlerRef = handlerRef;
        this.streamId = streamId;
    }

    public void execute(Connector connector)
    {
        connector.doConnect(handler, handlerRef, streamId);
    }

    @Override
    public String toString()
    {
        return format("CONNECT [handler=\"%s\", handlerRef=%d, streamId=%d]", handler, handlerRef, streamId);
    }
}
