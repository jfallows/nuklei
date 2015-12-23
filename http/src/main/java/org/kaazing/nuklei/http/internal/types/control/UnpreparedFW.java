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

import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_UNPREPARED_RESPONSE;

import java.nio.charset.StandardCharsets;

import org.kaazing.nuklei.http.internal.types.Flyweight;
import org.kaazing.nuklei.http.internal.types.HeadersFW;
import org.kaazing.nuklei.http.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class UnpreparedFW extends Flyweight
{
    private static final int FIELD_OFFSET_CORRELATION_ID = 0;
    private static final int FIELD_SIZE_CORRELATION_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_SOURCE = FIELD_OFFSET_CORRELATION_ID + FIELD_SIZE_CORRELATION_ID;
    private static final int FIELD_SIZE_DESTINATION_REF = BitUtil.SIZE_OF_LONG;

    private final StringFW sourceRO = new StringFW();
    private final StringFW destinationRO = new StringFW();
    private final HeadersFW headersRO = new HeadersFW();

    @Override
    public UnpreparedFW wrap(DirectBuffer buffer, int offset, int maxLimit)
    {
        super.wrap(buffer, offset, maxLimit);

        this.sourceRO.wrap(buffer, offset + FIELD_OFFSET_SOURCE, maxLimit);
        this.destinationRO.wrap(buffer, sourceRO.limit(), maxLimit);
        this.headersRO.wrap(buffer, destinationRO.limit() + FIELD_SIZE_DESTINATION_REF, maxLimit);

        checkLimit(limit(), maxLimit);

        return this;
    }

    @Override
    public int limit()
    {
        return headers().limit();
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

    public StringFW destination()
    {
        return destinationRO;
    }

    public long destinationRef()
    {
        return buffer().getLong(destination().limit());
    }

    public HeadersFW headers()
    {
        return headersRO;
    }

    @Override
    public String toString()
    {
        return String.format("[correlationId=%d, source=%s, destination=%s, destinationRef=%d]",
                correlationId(), source(), destination(), destinationRef());
    }

    public static final class Builder extends Flyweight.Builder<UnpreparedFW>
    {
        private final StringFW.Builder sourceRW = new StringFW.Builder();
        private final StringFW.Builder destinationRW = new StringFW.Builder();
        private final HeadersFW.Builder headersRW = new HeadersFW.Builder();

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

        public Builder destination(String destination)
        {
            destination().set(destination, StandardCharsets.UTF_8);
            headers();
            limit(destination().build().limit() + FIELD_SIZE_DESTINATION_REF);
            return this;
        }

        public Builder destinationRef(long destinationRef)
        {
            buffer().putLong(destination().build().limit(), destinationRef);
            return this;
        }

        public Builder header(String name, String value)
        {
            headersRW.header(name, value);
            limit(headersRW.limit());
            return this;
        }

        protected StringFW.Builder source()
        {
            return this.sourceRW.wrap(buffer(), offset() + FIELD_OFFSET_SOURCE, maxLimit());
        }

        protected StringFW.Builder destination()
        {
            return this.destinationRW.wrap(buffer(), source().build().limit(), maxLimit());
        }

        protected HeadersFW.Builder headers()
        {
            return this.headersRW.wrap(buffer(), destination().build().limit() + FIELD_SIZE_DESTINATION_REF, maxLimit());
        }
    }
}
