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
import org.kaazing.nuklei.http.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class UnpreparedFW extends Flyweight
{
    private static final int FIELD_OFFSET_CORRELATION_ID = 0;
    private static final int FIELD_SIZE_CORRELATION_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_DESTINATION = FIELD_OFFSET_CORRELATION_ID + FIELD_SIZE_CORRELATION_ID;
    private static final int FIELD_SIZE_DESTINATION_REF = BitUtil.SIZE_OF_LONG;

    private final StringFW destinationRO = new StringFW();
    private final StringFW handlerRO = new StringFW();

    @Override
    public UnpreparedFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.destinationRO.wrap(buffer, offset + FIELD_OFFSET_DESTINATION, actingLimit);
        this.handlerRO.wrap(buffer, destinationRO.limit() + FIELD_SIZE_DESTINATION_REF, actingLimit);

        checkLimit(limit(), actingLimit);

        return this;
    }

    @Override
    public int limit()
    {
        return handler().limit();
    }

    public int typeId()
    {
        return TYPE_ID_UNPREPARED_RESPONSE;
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

    public StringFW handler()
    {
        return handlerRO;
    }

    public long headersOffset()
    {
        return handlerRO.limit();
    }

    @Override
    public String toString()
    {
        return String.format("[correlationId=%d, destination=\"%s\", destinationRef=%d, handler=\"%s\"]",
                correlationId(), destination(), destinationRef(), handler());
    }

    public static final class Builder extends Flyweight.Builder<UnpreparedFW>
    {
        private final StringFW.Builder destinationRW = new StringFW.Builder();
        private final StringFW.Builder handlerRW = new StringFW.Builder();

        public Builder()
        {
            super(new UnpreparedFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);

            this.destinationRW.wrap(buffer, offset + FIELD_OFFSET_DESTINATION, maxLimit);

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

        public Builder handler(String handler)
        {
            handler().set(handler, StandardCharsets.UTF_8);
            return this;
        }

        protected StringFW.Builder destination()
        {
            return this.destinationRW.wrap(buffer(), offset() + FIELD_OFFSET_DESTINATION, maxLimit());
        }

        protected StringFW.Builder handler()
        {
            return this.handlerRW.wrap(buffer(), destination().build().limit() + FIELD_SIZE_DESTINATION_REF, maxLimit());
        }
    }
}
