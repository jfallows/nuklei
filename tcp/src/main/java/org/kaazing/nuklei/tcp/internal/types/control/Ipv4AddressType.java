/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.nuklei.tcp.internal.types.control;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.kaazing.nuklei.tcp.internal.types.Type;

import uk.co.real_logic.agrona.DirectBuffer;

public abstract class Ipv4AddressType<T extends DirectBuffer> extends Type<T>
{
    protected static final int FIELD_SIZE_IPV4_ADDRESS = 4;

    private final byte[] address = new byte[FIELD_SIZE_IPV4_ADDRESS];

    public int limit()
    {
        return offset() + FIELD_SIZE_IPV4_ADDRESS;
    }

    public InetAddress get()
    {
        try
        {
            buffer().getBytes(offset(), address, 0, address.length);
            return InetAddress.getByAddress(address);
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s", get());
    }
}
