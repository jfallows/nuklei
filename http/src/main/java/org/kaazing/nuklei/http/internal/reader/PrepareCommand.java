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
package org.kaazing.nuklei.http.internal.reader;

import static java.lang.String.format;

public final class PrepareCommand implements ReaderCommand
{
    private final long correlationId;
    private final String destination;
    private final long destinationRef;
    private final String handler;
    private final Object headers;

    public PrepareCommand(
        long correlationId,
        String destination,
        long destinationRef,
        String handler,
        Object headers)
    {
        this.correlationId = correlationId;
        this.destination = destination;
        this.destinationRef = destinationRef;
        this.handler = handler;
        this.headers = headers;
    }

    @Override
    public void execute(Reader reader)
    {
        reader.doPrepare(correlationId, destination, destinationRef, handler, headers);
    }

    @Override
    public String toString()
    {
        return format("PREPARE [correlationId=%d, destination=\"%s\", destinationRef=%d, handler=\"%s\", headers=%s]",
                correlationId, destination, destinationRef, handler, headers);
    }
}
