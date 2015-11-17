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

import org.kaazing.nuklei.tcp.internal.types.StringType;
import org.kaazing.nuklei.tcp.internal.types.Type;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

public abstract class AddressType<T extends DirectBuffer> extends Type<T>
{
    public static final int KIND_DEVICE_NAME = 0x00;
    public static final int KIND_IPV4_ADDRESS = 0x01;
    public static final int KIND_IPV6_ADDRESS = 0x02;

    protected static final int FIELD_OFFSET_KIND = 0;
    protected static final int FIELD_SIZE_KIND = BitUtil.SIZE_OF_BYTE;

    protected static final int FIELD_OFFSET_ADDRESS = FIELD_OFFSET_KIND + FIELD_SIZE_KIND;
    protected static final int FIELD_SIZE_IPV4_ADDRESS = 4;
    protected static final int FIELD_SIZE_IPV6_ADDRESS = 16;

    public final int kind()
    {
        return buffer().getByte(offset() + FIELD_OFFSET_KIND) & 0xFF;
    }

    public abstract StringType<T> deviceName();

    public abstract Ipv4AddressType<T> ipv4Address();

    public abstract Ipv6AddressType<T> ipv6Address();

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

    @Override
    public String toString()
    {
        switch (kind())
        {
        case KIND_DEVICE_NAME:
            return String.format("[kind=DEVICE_NAME, deviceName=%s]", deviceName());
        case KIND_IPV4_ADDRESS:
            return String.format("[kind=IPV4_ADDRESS, ipv4Address=%s]", ipv4Address());
        case KIND_IPV6_ADDRESS:
            return String.format("[kind=IPV6_ADDRESS, ipv6Address=%s]", ipv6Address());
        default:
            return String.format("[kind=UNKNOWN]");
        }
    }
}
