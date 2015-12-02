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

import java.net.InetSocketAddress;

public class ConnectorState
{
    private final String handler;
    private final long handlerRef;
    private final InetSocketAddress remoteAddress;

    public ConnectorState(
        String handler,
        long handlerRef,
        InetSocketAddress remoteAddress)
    {
        this.handlerRef = handlerRef;
        this.handler = handler;
        this.remoteAddress = remoteAddress;
    }

    public String handler()
    {
        return handler;
    }

    public long handlerRef()
    {
        return this.handlerRef;
    }

    public InetSocketAddress remoteAddress()
    {
        return remoteAddress;
    }

    @Override
    public String toString()
    {
        return String.format("[handler=\"%s\", handlerRef=%d, remoteAddress=%s]", handler, handlerRef, remoteAddress);
    }
}
