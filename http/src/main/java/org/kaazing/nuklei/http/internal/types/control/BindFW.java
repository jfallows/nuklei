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
package org.kaazing.nuklei.http.internal.types.control;

import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_BIND_COMMAND;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.kaazing.nuklei.http.internal.types.Flyweight;
import org.kaazing.nuklei.http.internal.types.HeadersFW;
import org.kaazing.nuklei.http.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class BindFW extends Flyweight
{
    private static final int FIELD_OFFSET_CORRELATION_ID = 0;
    private static final int FIELD_SIZE_CORRELATION_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_DESTINATION = FIELD_OFFSET_CORRELATION_ID + FIELD_SIZE_CORRELATION_ID;
    private static final int FIELD_SIZE_DESTINATION_REF = BitUtil.SIZE_OF_LONG;

    private final StringFW destinationRO = new StringFW();
    private final StringFW sourceRO = new StringFW();
    private final HeadersFW headersRO = new HeadersFW();

    @Override
    public BindFW wrap(DirectBuffer buffer, int offset, int maxLimit)
    {
        super.wrap(buffer, offset, maxLimit);

        this.destinationRO.wrap(buffer, offset + FIELD_OFFSET_DESTINATION, maxLimit);
        this.sourceRO.wrap(buffer, destinationRO.limit() + FIELD_SIZE_DESTINATION_REF, maxLimit);
        this.headersRO.wrap(buffer, sourceRO.limit(), maxLimit);

        checkLimit(limit(), maxLimit);

        return this;
    }

    @Override
    public int limit()
    {
        return headersRO.limit();
    }

    public int typeId()
    {
        return TYPE_ID_BIND_COMMAND;
    }

    public long correlationId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_CORRELATION_ID);
    }

    public StringFW destination()
    {
        return destinationRO;
    }

    public long destinationRef()
    {
        return buffer().getLong(destination().limit());
    }

    public StringFW source()
    {
        return sourceRO;
    }

    public HeadersFW headers()
    {
        return headersRO;
    }

    @Override
    public String toString()
    {
        return String.format("[correlationId=%d, destination=%s, destinationRef=%d, source=%s]",
                correlationId(), destination(), destinationRef(), source());
    }

    public static final class Builder extends Flyweight.Builder<BindFW>
    {
        private final StringFW.Builder destinationRW = new StringFW.Builder();
        private final StringFW.Builder sourceRW = new StringFW.Builder();
        private final HeadersFW.Builder headersRW = new HeadersFW.Builder();

        public Builder()
        {
            super(new BindFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);

            return this;
        }

        public <E> Builder iterate(
            Iterable<E> iterable,
            Consumer<E> action)
        {
            iterable.forEach(action);
            return this;
        }

        public Builder correlationId(long correlationId)
        {
            buffer().putLong(offset() + FIELD_OFFSET_CORRELATION_ID, correlationId);
            return this;
        }

        public Builder destination(String destination)
        {
            destination().set(destination, StandardCharsets.UTF_8);
            return this;
        }

        public Builder destinationRef(long destinationRef)
        {
            buffer().putLong(destination().build().limit(), destinationRef);
            return this;
        }

        public Builder source(String source)
        {
            source().set(source, StandardCharsets.UTF_8);
            headers(source().build().limit());
            limit(source().build().limit());
            return this;
        }

        public Builder header(String name, String value)
        {
            headersRW.header(name, value);
            limit(headersRW.limit());
            return this;
        }

        protected StringFW.Builder destination()
        {
            return this.destinationRW.wrap(buffer(), offset() + FIELD_OFFSET_DESTINATION, maxLimit());
        }

        protected StringFW.Builder source()
        {
            return this.sourceRW.wrap(buffer(), destination().build().limit() + FIELD_SIZE_DESTINATION_REF, maxLimit());
        }

        protected HeadersFW.Builder headers(int offset)
        {
            return this.headersRW.wrap(buffer(), offset, maxLimit());
        }
    }
}
