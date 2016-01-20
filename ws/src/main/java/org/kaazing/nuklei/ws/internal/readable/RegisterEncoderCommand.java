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

public final class RegisterEncoderCommand implements ReadableCommand
{
    private final long destinationInitialStreamId;
    private final long sourceInitialStreamId;
    private final long sourceReplyStreamId;
    private final RingBuffer sourceRoute;
    private final byte[] handshakeKey;

    public RegisterEncoderCommand(
        long destinationInitialStreamId,
        long sourceInitialStreamId,
        long sourceReplyStreamId,
        RingBuffer sourceRoute, byte[] handshakeKey)
    {
        this.destinationInitialStreamId = destinationInitialStreamId;
        this.sourceInitialStreamId = sourceInitialStreamId;
        this.sourceReplyStreamId = sourceReplyStreamId;
        this.sourceRoute = sourceRoute;
        this.handshakeKey = handshakeKey;
    }

    @Override
    public void execute(Readable source)
    {
        source.doRegisterEncoder(destinationInitialStreamId, sourceInitialStreamId,
                                 sourceReplyStreamId, sourceRoute, handshakeKey);
    }

    @Override
    public String toString()
    {
        return format("REGISTER_ENCODER [destinationInitialStreamId=%d, sourceReplyStreamId=%d]",
                destinationInitialStreamId, sourceReplyStreamId);
    }
}
