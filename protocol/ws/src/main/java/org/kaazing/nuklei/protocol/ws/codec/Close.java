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

import static java.lang.String.format;

import java.net.ProtocolException;

import org.kaazing.nuklei.ErrorHandler;

import uk.co.real_logic.agrona.DirectBuffer;

public class Close extends ControlFrame
{

    Close()
    {

    }

    public Close wrap(DirectBuffer buffer, int offset) throws ProtocolException
    {
        super.wrap(buffer, offset, false);
        return this;
    }

    public int getStatusCode()
    {
        if (getLength() < 2)
        {
            return 1005; // RFC 6455 section 7.4.1
        }
        return uint16Get(getPayload().buffer(), getPayload().offset());
    }

    /**
     * WARNING: use of this method causes garbage collection
     * @return
     */
    public String getReason()
    {
        if (getLength() < 3)
        {
            return null;
        }
        return getPayload().buffer().getStringWithoutLengthUtf8(getPayload().offset() + 2, getLength() - 2);
    }

    @Override
    public void validate(ErrorHandler errorHandler)
    {
        super.validate(errorHandler);
        if (getLength() == 1)
        {
            errorHandler.handleError("Invalid Close frame payload: length=1");
        }
        if (getLength() >= 2 && (getStatusCode() == 1005 || getStatusCode() == 1006))
        {
            errorHandler.handleError(format("Illegal Close status code %d", getStatusCode()));
        }
        // TODO: check reason (payload 3rd byte to end) is valid UTF-8
    }

}
