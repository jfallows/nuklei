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
package org.kaazing.nuklei.echo.internal.readable;

import static java.lang.String.format;

public final class UnbindCommand implements ReadableCommand
{
    private final long correlationId;
    private final long sourceRef;

    public UnbindCommand(
        long correlationId,
        long sourceRef)
    {
        this.correlationId = correlationId;
        this.sourceRef = sourceRef;
    }

    @Override
    public void execute(Readable readable)
    {
        readable.doUnbind(correlationId, sourceRef);
    }

    @Override
    public String toString()
    {
        return format("UNBIND [correlationId=%d, sourceRef=%d]", correlationId, sourceRef);
    }
}
