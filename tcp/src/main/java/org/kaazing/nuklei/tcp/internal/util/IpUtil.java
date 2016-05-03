/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.tcp.internal.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.function.Consumer;

import org.kaazing.nuklei.tcp.internal.types.AddressFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.LangUtil;

public final class IpUtil
{
    private static final int FIELD_SIZE_IPV4_ADDRESS = 4;
    private static final int FIELD_SIZE_IPV6_ADDRESS = 16;

    private static final byte[] IPV4_ADDRESS_BYTES = new byte[FIELD_SIZE_IPV4_ADDRESS];
    private static final byte[] IPV6_ADDRESS_BYTES = new byte[FIELD_SIZE_IPV6_ADDRESS];

    private static final InetAddress UNREACHABLE = null;

    private IpUtil()
    {
        // no instances
    }

    public static InetAddress inetAddress(
        AddressFW address)
    {
        try
        {
            switch (address.kind())
            {
            case AddressFW.KIND_DEVICE_NAME:
                NetworkInterface iface = NetworkInterface.getByName(address.deviceName().asString());
                return iface.getInetAddresses().nextElement();
            case AddressFW.KIND_IPV4_ADDRESS:
                return address.ipv4Address(IpUtil::ipv4Address);
            case AddressFW.KIND_IPV6_ADDRESS:
                return address.ipv6Address(IpUtil::ipv6Address);
            default:
                throw new IllegalStateException("Unrecognized kind: " + address.kind());
            }
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
            return null;
        }
    }

    public static void ipAddress(
        InetSocketAddress ipAddress,
        Consumer<byte[]> ipv4Address,
        Consumer<byte[]> ipv6Address)
    {
        InetAddress inetAddress = ipAddress.getAddress();
        if (inetAddress instanceof Inet4Address)
        {
            ipv4Address.accept(inetAddress.getAddress());
        }
        else if (inetAddress instanceof Inet6Address)
        {
            ipv6Address.accept(inetAddress.getAddress());
        }
    }

    public static InetAddress ipv4Address(
        DirectBuffer buffer,
        int offset,
        int length)
    {
        try
        {
            buffer.getBytes(offset, IPV4_ADDRESS_BYTES, 0, IPV4_ADDRESS_BYTES.length);
            return InetAddress.getByAddress(IPV4_ADDRESS_BYTES);
        }
        catch (UnknownHostException ex)
        {
            LangUtil.rethrowUnchecked(ex);
            return UNREACHABLE;
        }
    }

    public static InetAddress ipv6Address(
        DirectBuffer buffer,
        int offset,
        int length)
    {
        try
        {
            buffer.getBytes(offset, IPV6_ADDRESS_BYTES, 0, IPV6_ADDRESS_BYTES.length);
            return InetAddress.getByAddress(IPV6_ADDRESS_BYTES);
        }
        catch (UnknownHostException ex)
        {
            LangUtil.rethrowUnchecked(ex);
            return UNREACHABLE;
        }
    }

}
