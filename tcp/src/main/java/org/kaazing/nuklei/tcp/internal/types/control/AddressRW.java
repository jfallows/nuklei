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

import static java.nio.ByteOrder.BIG_ENDIAN;

import org.kaazing.nuklei.tcp.internal.types.StringRW;
import org.kaazing.nuklei.tcp.internal.types.StringType;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class AddressRW extends AddressType<MutableDirectBuffer>
{
    private final StringRW deviceName = new StringRW();
    private final MutableDirectBuffer ipv6Address = new UnsafeBuffer(new byte[0]);

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
    public MutableDirectBuffer ipv6Address()
    {
        return ipv6Address;
    }

    public AddressRW deviceName(StringType<?> deviceName)
    {
        kind((byte) KIND_DEVICE_NAME);
        this.deviceName.wrap(buffer(), offset() + FIELD_OFFSET_ADDRESS);
        this.deviceName.set(deviceName);
        return this;
    }

    public AddressRW ipv4Address(int address)
    {
        kind((byte) KIND_IPV4_ADDRESS);
        buffer().putInt(offset() + FIELD_OFFSET_ADDRESS, address, BIG_ENDIAN);
        return this;
    }

    public AddressRW ipv6Address(DirectBuffer ipv6Address)
    {
        kind((byte) KIND_IPV6_ADDRESS);
        buffer().putBytes(offset() + FIELD_OFFSET_ADDRESS, ipv6Address, 0, FIELD_SIZE_IPV6_ADDRESS);
        return this;
    }

    private AddressRW kind(byte kind)
    {
        buffer().putByte(offset() + FIELD_OFFSET_KIND, kind);
        return this;
    }
}
