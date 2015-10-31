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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.nuklei.tcp.internal.types.control;

import java.net.InetAddress;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class Ipv6AddressRW extends Ipv6AddressType<MutableDirectBuffer>
{
    @Override
    protected Ipv6AddressRW wrap(MutableDirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset);
        return this;
    }

    public Ipv6AddressRW set(InetAddress address)
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
