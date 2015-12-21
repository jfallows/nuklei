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

import static org.kaazing.nuklei.tcp.internal.types.control.Types.TYPE_ID_UNPREPARED_RESPONSE;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import org.kaazing.nuklei.tcp.internal.types.Flyweight;
import org.kaazing.nuklei.tcp.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class UnpreparedFW extends Flyweight
{
    private static final int FIELD_OFFSET_CORRELATION_ID = 0;
    private static final int FIELD_SIZE_CORRELATION_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_SOURCE = FIELD_OFFSET_CORRELATION_ID + FIELD_SIZE_CORRELATION_ID;

    private final StringFW sourceRO = new StringFW();
    private final BindingFW bindingRO = new BindingFW();

    @Override
    public UnpreparedFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.sourceRO.wrap(buffer, offset + FIELD_OFFSET_SOURCE, actingLimit);
        this.bindingRO.wrap(buffer, sourceRO.limit(), actingLimit);

        checkLimit(limit(), actingLimit);

        return this;
    }

    @Override
    public int limit()
    {
        return bindingRO.limit();
    }

    public int typeId()
    {
        return TYPE_ID_UNPREPARED_RESPONSE;
    }

    public long correlationId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_CORRELATION_ID);
    }

    public StringFW source()
    {
        return sourceRO;
    }

    public AddressFW address()
    {
        return bindingRO.address();
    }

    public int port()
    {
        return bindingRO.port();
    }

    @Override
    public String toString()
    {
        return String.format("[correlationId=%d, source=%s, address=%s, port=%d]",
                correlationId(), source(), address(), port());
    }

    public static final class Builder extends Flyweight.Builder<UnpreparedFW>
    {
        private final StringFW.Builder sourceRW = new StringFW.Builder();
        private final BindingFW.Builder bindingRW = new BindingFW.Builder();

        public Builder()
        {
            super(new UnpreparedFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            return this;
        }

        public Builder correlationId(long correlationId)
        {
            buffer().putLong(offset() + FIELD_OFFSET_CORRELATION_ID, correlationId);
            return this;
        }

        public Builder source(String source)
        {
            source().set(source, StandardCharsets.UTF_8);
            return this;
        }

        public Builder address(InetAddress ipAddress)
        {
            binding().address(ipAddress);
            return this;
        }

        public Builder port(int port)
        {
            binding().port(port);
            return this;
        }

        protected StringFW.Builder source()
        {
            return this.sourceRW.wrap(buffer(), offset() + FIELD_OFFSET_SOURCE, maxLimit());
        }

        protected BindingFW.Builder binding()
        {
            return this.bindingRW.wrap(buffer(), source().build().limit(), maxLimit());
        }
    }
}
