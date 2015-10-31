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

import static org.kaazing.nuklei.tcp.internal.types.Types.checkLimit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

import org.kaazing.nuklei.tcp.internal.types.StringRO;

import uk.co.real_logic.agrona.DirectBuffer;

public final class AddressRO extends AddressType<DirectBuffer>
{
    private final StringRO deviceName = new StringRO();
    private final Ipv4AddressRO ipv4Address = new Ipv4AddressRO();
    private final Ipv6AddressRO ipv6Address = new Ipv6AddressRO();

    public AddressRO wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        switch (kind())
        {
        case KIND_DEVICE_NAME:
            deviceName.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS, actingLimit);
            break;
        case KIND_IPV4_ADDRESS:
            ipv4Address.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS, actingLimit);
            break;
        case KIND_IPV6_ADDRESS:
            ipv6Address.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS, FIELD_SIZE_IPV6_ADDRESS);
            break;
        }

        checkLimit(limit(), actingLimit);
        return this;
    }

    @Override
    public StringRO deviceName()
    {
        return deviceName;
    }

    @Override
    public Ipv4AddressRO ipv4Address()
    {
        return ipv4Address;
    }

    @Override
    public Ipv6AddressRO ipv6Address()
    {
        return ipv6Address;
    }

    public InetAddress asInetAddress()
    {
        try
        {
            switch (kind())
            {
            case KIND_DEVICE_NAME:
                NetworkInterface iface = NetworkInterface.getByName(deviceName.asString());
                return iface.getInetAddresses().nextElement();
            case KIND_IPV4_ADDRESS:
                return ipv4Address.get();
            case KIND_IPV6_ADDRESS:
                return ipv6Address.get();
            default:
                throw new IllegalStateException("Unrecognized kind: " + kind());
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }
}
