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
package org.kaazing.nuklei.echo.internal.reader;


public final class BoundResponse implements ReaderCommand
{
    private final long correlationId;
    private final String sourceName;
    private final long referenceId;

    public BoundResponse(
        String sourceName,
        long correlationId,
        long referenceId)
    {
        this.correlationId = correlationId;
        this.sourceName = sourceName;
        this.referenceId = referenceId;
    }

    public void execute(Reader reader)
    {
        reader.onBoundResponse(sourceName, correlationId, referenceId);
    }
}
