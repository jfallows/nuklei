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
package org.kaazing.nuklei.ws.internal.readable;

import static java.lang.String.format;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class RegisterDecoderCommand implements ReadableCommand
{
    private final long destinationInitialStreamId;
    private final long sourceInitialStreamId;
    private final RingBuffer sourceRoute;

    public RegisterDecoderCommand(
        long destinationInitialStreamId,
        long sourceInitialStreamId,
        RingBuffer sourceRoute)
    {
        this.destinationInitialStreamId = destinationInitialStreamId;
        this.sourceInitialStreamId = sourceInitialStreamId;
        this.sourceRoute = sourceRoute;
    }

    @Override
    public void execute(Readable source)
    {
        source.doRegisterDecoder(destinationInitialStreamId, sourceInitialStreamId, sourceRoute);
    }

    @Override
    public String toString()
    {
        return format("REGISTER_DECODER [destinationInitialStreamId=%d, sourceInitialStreamId=%d]",
                destinationInitialStreamId, sourceInitialStreamId);
    }
}
