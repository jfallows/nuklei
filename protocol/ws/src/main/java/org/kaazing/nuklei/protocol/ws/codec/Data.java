/*
 * Copyright 2015 Kaazing Corporation, All rights reserved.
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

import org.kaazing.nuklei.ErrorHandler;
import org.kaazing.nuklei.util.Utf8Util;

import uk.co.real_logic.agrona.DirectBuffer;

public class Data extends Frame
{
    private final int maxWsMessageSize;

    Data(int maxWsMessageSize)
    {
        this.maxWsMessageSize = maxWsMessageSize;
    }

    @Override
    public void validate(ErrorHandler errorHandler)
    {
        super.validate(errorHandler);
        Utf8Util.validateUTF8(buffer(), offset() + getDataOffset(), getLength(), errorHandler);
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
