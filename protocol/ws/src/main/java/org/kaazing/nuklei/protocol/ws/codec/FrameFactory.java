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
package org.kaazing.nuklei.protocol.ws.codec;

import java.net.ProtocolException;

import org.kaazing.nuklei.FlyweightBE;

import uk.co.real_logic.agrona.DirectBuffer;

public class FrameFactory extends FlyweightBE
{
    private final Close close = new Close();
    private final Continuation continuation;
    private final Data data;
    private final Ping ping = new Ping();
    private final Pong pong = new Pong();

    public FrameFactory(int maxWsMessageSize)
    {
        continuation = new Continuation(maxWsMessageSize);
        data = new Data(maxWsMessageSize);
    }

    public Frame wrap(DirectBuffer buffer, int offset) throws ProtocolException
    {
        Frame frame = null;
        switch(Frame.getOpCode(buffer, offset))
        {
        case BINARY:
            frame = data.wrap(buffer, offset);
            break;
        case CLOSE:
            frame = close.wrap(buffer, offset);
            break;
        case CONTINUATION:
            frame = continuation.wrap(buffer, offset);
            break;
        case PING:
            frame = ping.wrap(buffer, offset);
            break;
        case PONG:
            frame = pong.wrap(buffer, offset);
            break;
        case TEXT:
            frame = data.wrap(buffer, offset);
            break;
        default:
            Frame.protocolError(null);
            break;
        }
        return frame;
    }

}
