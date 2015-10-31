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
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.InetAddress;

import org.kaazing.nuklei.tcp.internal.types.StringRW;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class BindingRW extends BindingType<MutableDirectBuffer>
{
    private final StringRW source = new StringRW();
    private final StringRW destination = new StringRW();
    private final AddressRW address = new AddressRW();

    public BindingRW wrap(MutableDirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset);

        return this;
    }

    public BindingRW source(String source)
    {
        source().set(source, UTF_8);
        return this;
    }

    public BindingRW sourceBindingRef(long sourceBindingRef)
    {
        buffer().putLong(offset() + source().limit(), sourceBindingRef);
        return this;
    }

    public BindingRW destination(String destination)
    {
        destination().set(destination, UTF_8);
        return this;
    }

    public BindingRW address(InetAddress ipAddress)
    {
        address().ipAddress(ipAddress);
        return this;
    }

    public BindingRW port(int port)
    {
        buffer().putShort(address().limit(), (short)(port & 0xFFFF), BIG_ENDIAN);
        return this;
    }

    @Override
    public StringRW source()
    {
        return source.wrap(buffer(), offset() + FIELD_OFFSET_SOURCE);
    }

    @Override
    public StringRW destination()
    {
        return destination.wrap(buffer(), source.limit() + FIELD_SIZE_SOURCE_BINDING_REF);
    }

    @Override
    public AddressRW address()
    {
        return address.wrap(buffer(), destination.limit());
    }
}
