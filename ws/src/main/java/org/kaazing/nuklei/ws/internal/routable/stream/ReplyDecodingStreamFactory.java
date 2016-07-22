/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.ws.internal.routable.stream;

import org.kaazing.nuklei.ws.internal.routable.Source;
import org.kaazing.nuklei.ws.internal.routable.Target;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;

public final class ReplyDecodingStreamFactory
{
    private final Source source;

    public ReplyDecodingStreamFactory(
        Source source)
    {
        this.source = source;
    }

    public MessageHandler newStream(
        Target target,
        long targetRef,
        long targetId,
        String protocol,
        byte[] handshakeKey)
    {
        return new ReplyDecodingStream(target, targetRef, targetId, protocol, handshakeKey)::handleStream;
    }

    private final class ReplyDecodingStream
    {
        private ReplyDecodingStream(
            Target target,
            long targetRef,
            long targetId,
            String protocol,
            byte[] handshakeKey)
        {
            // TODO Auto-generated constructor stub
        }

        private void handleStream(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            // TODO:
        }
    }
}
