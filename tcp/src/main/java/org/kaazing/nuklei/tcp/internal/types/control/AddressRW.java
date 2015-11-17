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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import org.kaazing.nuklei.tcp.internal.types.StringRW;
import org.kaazing.nuklei.tcp.internal.types.StringType;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class AddressRW extends AddressType<MutableDirectBuffer>
{
    private final StringRW deviceName = new StringRW();
    private final Ipv4AddressRW ipv4Address = new Ipv4AddressRW();
    private final Ipv6AddressRW ipv6Address = new Ipv6AddressRW();

    public AddressRW wrap(MutableDirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset);
        return this;
    }

    @Override
    public StringRW deviceName()
    {
        return deviceName;
    }

    @Override
    public Ipv4AddressRW ipv4Address()
    {
        return ipv4Address.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS);
    }

    @Override
    public Ipv6AddressRW ipv6Address()
    {
        return ipv6Address.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS);
    }

    public AddressRW deviceName(StringType<?> deviceName)
    {
        kind((byte) KIND_DEVICE_NAME);
        this.deviceName.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS);
        this.deviceName.set(deviceName);
        return this;
    }

    public void ipAddress(InetAddress ipAddress)
    {
        if (ipAddress instanceof Inet4Address)
        {
            ipv4Address(ipAddress);
        }
        else if (ipAddress instanceof Inet6Address)
        {
            ipv6Address(ipAddress);
        }
    }

    public AddressRW ipv4Address(InetAddress ipv4Address)
    {
        kind((byte) KIND_IPV4_ADDRESS);
        ipv4Address().set(ipv4Address);
        return this;
    }

    public AddressRW ipv6Address(InetAddress ipv6Address)
    {
        kind((byte) KIND_IPV6_ADDRESS);
        ipv6Address().set(ipv6Address);
        return this;
    }

    private AddressRW kind(byte kind)
    {
        buffer().putByte(offset() + FIELD_OFFSET_KIND, kind);
        return this;
    }
}
