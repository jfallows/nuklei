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

import org.kaazing.nuklei.util.Utf8Util;

import uk.co.real_logic.agrona.DirectBuffer;

/**
 * This flyweight represents a WebSocket Text or Binary frame. No validation of valid UTF8 content
 * for the Text case is done by this class, because it is not practical given that a text message
 * may be split between different frames so a multibyte UTF-8 character may be split between frames.
 * Callers should validate the payload of Text frames and any following Continuation frames
 * using the methods provided in org.kaazing.nuklei.util.Utf8Util, as in the following code example:
 * <pre>
 *      Payload payload = super.getPayload();
 *      if (getOpCode() == OpCode.TEXT)
 *      {
 *          incompleteBytes = Utf8Util.validateUTF8(payload.buffer(), payload.offset(), getLength(), (message) ->
 *          {
 *              protocolError(message);
 *          });
 *      }
 * </pre>
 *
 */
public class Data extends Frame
{
    private final int maxWsMessageSize;

    Data(int maxWsMessageSize)
    {
        this.maxWsMessageSize = maxWsMessageSize;
    }

    @Override
    public Payload getPayload()
    {
        Payload payload = super.getPayload();
        if (getOpCode() == OpCode.TEXT)
        {
            Utf8Util.validateUTF8(payload.buffer(), payload.offset(), getLength(), (message) ->
            {
                protocolError(message);
            });
        }
        return payload;
    }

    public Data wrap(DirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset, false);
        return this;
    }

    @Override
    protected int getMaxPayloadLength()
    {
        return maxWsMessageSize;
    }

}
