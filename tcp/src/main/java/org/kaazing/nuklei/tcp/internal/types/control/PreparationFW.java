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

import static java.lang.String.format;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.InetAddress;

import org.kaazing.nuklei.tcp.internal.types.Flyweight;
import org.kaazing.nuklei.tcp.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class PreparationFW extends Flyweight
{
    private static final int FIELD_OFFSET_DESTINATION = 0;
    private static final int FIELD_SIZE_DESTINATION_REF = BitUtil.SIZE_OF_LONG;
    private static final int FIELD_SIZE_PORT = BitUtil.SIZE_OF_SHORT;

    private final StringFW destinationRO = new StringFW();
    private final StringFW sourceRO = new StringFW();
    private final AddressFW addressRO = new AddressFW();

    public PreparationFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.destinationRO.wrap(buffer, offset + FIELD_OFFSET_DESTINATION, actingLimit);
        this.sourceRO.wrap(buffer, destinationRO.limit() + FIELD_SIZE_DESTINATION_REF, actingLimit);
        this.addressRO.wrap(buffer, sourceRO.limit(), actingLimit);

        checkLimit(limit(), actingLimit);

        return this;
    }

    public int limit()
    {
        return address().limit() + FIELD_SIZE_PORT;
    }

    public StringFW destination()
    {
        return destinationRO;
    }

    public long destinationRef()
    {
        return buffer().getLong(offset() + destination().limit());
    }

    public StringFW source()
    {
        return sourceRO;
    }

    public AddressFW address()
    {
        return addressRO;
    }

    public int port()
    {
        return buffer().getShort(address().limit(), BIG_ENDIAN) & 0xFFFF;
    }

    @Override
    public String toString()
    {
        return format("[destinationRO=%s, sourceRO=%s, addressRO=%s, port=%d]", destination(), source(), address(), port());
    }

    public static final class Builder extends Flyweight.Builder<PreparationFW>
    {
        private final StringFW.Builder destinationRW = new StringFW.Builder();
        private final StringFW.Builder sourceRW = new StringFW.Builder();
        private final AddressFW.Builder addressRW = new AddressFW.Builder();

        public Builder()
        {
            super(new PreparationFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            return this;
        }

        public Builder destination(String destination)
        {
            destination().set(destination, UTF_8);
            return this;
        }

        public Builder destinationRef(long sourceRef)
        {
            buffer().putLong(offset() + destination().build().limit(), sourceRef);
            return this;
        }

        public Builder source(String source)
        {
            source().set(source, UTF_8);
            return this;
        }

        public Builder address(InetAddress ipAddress)
        {
            address().ipAddress(ipAddress);
            return this;
        }

        public Builder port(int port)
        {
            buffer().putShort(address().build().limit(), (short)(port & 0xFFFF), BIG_ENDIAN);
            return this;
        }

        public StringFW.Builder destination()
        {
            return destinationRW.wrap(buffer(), offset() + FIELD_OFFSET_DESTINATION, maxLimit());
        }

        public StringFW.Builder source()
        {
            return sourceRW.wrap(buffer(), destination().build().limit() + FIELD_SIZE_DESTINATION_REF, maxLimit());
        }

        public AddressFW.Builder address()
        {
            return addressRW.wrap(buffer(), source().build().limit(), maxLimit());
        }
    }
}
