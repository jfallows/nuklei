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
package org.kaazing.nuklei.tcp.internal.types.control;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.kaazing.nuklei.tcp.internal.types.Flyweight;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class Ipv6AddressFW extends Flyweight
{
    private static final int FIELD_SIZE_IPV6_ADDRESS = 16;

    private final byte[] address = new byte[FIELD_SIZE_IPV6_ADDRESS];

    public Ipv6AddressFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        checkLimit(limit(), actingLimit);

        return this;
    }

    public int limit()
    {
        return offset() + FIELD_SIZE_IPV6_ADDRESS;
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

    public static final class Builder extends Flyweight.Builder<Ipv6AddressFW>
    {
        public Builder()
        {
            super(new Ipv6AddressFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            return this;
        }

        public Builder set(InetAddress address)
        {
            try
            {
                byte[] ipv6Address = address.getAddress();
                buffer().putBytes(offset(), ipv6Address, 0, FIELD_SIZE_IPV6_ADDRESS);
                return this;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
