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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;

import org.kaazing.nuklei.tcp.internal.types.Flyweight;
import org.kaazing.nuklei.tcp.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class AddressFW extends Flyweight
{
    private final StringFW deviceName = new StringFW();
    private final Ipv4AddressFW ipv4Address = new Ipv4AddressFW();
    private final Ipv6AddressFW ipv6Address = new Ipv6AddressFW();

    public static final int KIND_DEVICE_NAME = 0x00;
    public static final int KIND_IPV4_ADDRESS = 0x01;
    public static final int KIND_IPV6_ADDRESS = 0x02;

    private static final int FIELD_OFFSET_KIND = 0;
    private static final int FIELD_SIZE_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int FIELD_OFFSET_ADDRESS = FIELD_OFFSET_KIND + FIELD_SIZE_KIND;
    private static final int FIELD_SIZE_IPV4_ADDRESS = 4;
    private static final int FIELD_SIZE_IPV6_ADDRESS = 16;

    public AddressFW wrap(DirectBuffer buffer, int offset, int actingLimit)
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

    public int limit()
    {
        switch (kind())
        {
        case KIND_DEVICE_NAME:
            return deviceName().limit();
        case KIND_IPV4_ADDRESS:
            return offset() + FIELD_OFFSET_ADDRESS + FIELD_SIZE_IPV4_ADDRESS;
        case KIND_IPV6_ADDRESS:
            return offset() + FIELD_OFFSET_ADDRESS + FIELD_SIZE_IPV6_ADDRESS;
        default:
            return offset();
        }
    }

    public int kind()
    {
        return buffer().getByte(offset() + FIELD_OFFSET_KIND) & 0xFF;
    }

    public StringFW deviceName()
    {
        return deviceName;
    }

    public Ipv4AddressFW ipv4Address()
    {
        return ipv4Address;
    }

    public Ipv6AddressFW ipv6Address()
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

    @Override
    public String toString()
    {
        switch (kind())
        {
        case KIND_DEVICE_NAME:
            return String.format("[kind=DEVICE_NAME, deviceNameRW=%s]", deviceName());
        case KIND_IPV4_ADDRESS:
            return String.format("[kind=IPV4_ADDRESS, ipv4AddressRW=%s]", ipv4Address());
        case KIND_IPV6_ADDRESS:
            return String.format("[kind=IPV6_ADDRESS, ipv6AddressRW=%s]", ipv6Address());
        default:
            return String.format("[kind=UNKNOWN]");
        }
    }

    public static final class Builder extends Flyweight.Builder<AddressFW>
    {
        private final StringFW.Builder deviceNameRW = new StringFW.Builder();
        private final Ipv4AddressFW.Builder ipv4AddressRW = new Ipv4AddressFW.Builder();
        private final Ipv6AddressFW.Builder ipv6AddressRW = new Ipv6AddressFW.Builder();

        public Builder()
        {
            super(new AddressFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            return this;
        }

        public Builder deviceName(String deviceName, Charset charset)
        {
            kind((byte) KIND_DEVICE_NAME);
            deviceNameRW.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS, maxLimit());
            deviceNameRW.set(deviceName, charset);
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

        public Builder ipv4Address(InetAddress ipv4Address)
        {
            kind((byte) KIND_IPV4_ADDRESS);
            ipv4Address().set(ipv4Address);
            return this;
        }

        public Builder ipv6Address(InetAddress ipv6Address)
        {
            kind((byte) KIND_IPV6_ADDRESS);
            ipv6Address().set(ipv6Address);
            return this;
        }

        public StringFW.Builder deviceName()
        {
            return deviceNameRW.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS, maxLimit());
        }

        public Ipv4AddressFW.Builder ipv4Address()
        {
            return ipv4AddressRW.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS, maxLimit());
        }

        public Ipv6AddressFW.Builder ipv6Address()
        {
            return ipv6AddressRW.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS, maxLimit());
        }

        private Builder kind(byte kind)
        {
            buffer().putByte(offset() + FIELD_OFFSET_KIND, kind);
            return this;
        }
    }
}
