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

public final class UncaptureCommand implements ReaderCommand
{
    private final long correlationId;
    private final String handler;

    public UncaptureCommand(
        long correlationId,
        String handler)
    {
        this.correlationId = correlationId;
        this.handler = handler;
    }

    @Override
    public void execute(Reader reader)
    {
        reader.doUncapture(correlationId, handler);
    }
}
