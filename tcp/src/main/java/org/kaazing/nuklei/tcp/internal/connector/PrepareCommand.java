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

import java.net.InetSocketAddress;

public final class PrepareCommand implements ConnectorCommand
{
    private final long correlationId;
    private final String source;
    private final InetSocketAddress remoteAddress;

    public PrepareCommand(
        long correlationId,
        String source,
        InetSocketAddress remoteAddress)
    {
        this.correlationId = correlationId;
        this.source = source;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void execute(Connector connector)
    {
        connector.doPrepare(correlationId, source, remoteAddress);
    }

    @Override
    public String toString()
    {
        return format("PREPARE [correlationId=%d, source=\"%s\", remoteAddress=%s]", correlationId, source, remoteAddress);
    }
}
