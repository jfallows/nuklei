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

import org.kaazing.nuklei.http.internal.types.Flyweight;
import org.kaazing.nuklei.http.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class BindFW extends Flyweight
{
    private static final int FIELD_OFFSET_CORRELATION_ID = 0;
    private static final int FIELD_SIZE_CORRELATION_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_SOURCE = FIELD_OFFSET_CORRELATION_ID + FIELD_SIZE_CORRELATION_ID;
    private static final int FIELD_SIZE_SOURCE_REF = BitUtil.SIZE_OF_LONG;

    private final StringFW sourceRO = new StringFW();
    private final StringFW handlerRO = new StringFW();

    @Override
    public BindFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.sourceRO.wrap(buffer, offset + FIELD_OFFSET_SOURCE, actingLimit);
        this.handlerRO.wrap(buffer, sourceRO.limit() + FIELD_SIZE_SOURCE_REF, actingLimit);

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
        return TYPE_ID_BIND_COMMAND;
    }

    public long correlationId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_CORRELATION_ID);
    }

    public StringFW source()
    {
        return sourceRO;
    }

    public long sourceRef()
    {
        return buffer().getLong(source().limit());
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
        return String.format("[correlationId=%d, source=\"%s\", sourceRef=%d, handler=\"%s\"]",
                correlationId(), source(), sourceRef(), handler());
    }

    public static final class Builder extends Flyweight.Builder<BindFW>
    {
        private final StringFW.Builder sourceRW = new StringFW.Builder();
        private final StringFW.Builder handlerRW = new StringFW.Builder();

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

        public Builder sourceRef(long sourceRef)
        {
            buffer().putLong(source().build().limit(), sourceRef);
            return this;
        }

        public Builder handler(String handler)
        {
            handler().set(handler, StandardCharsets.UTF_8);
            return this;
        }

        protected StringFW.Builder source()
        {
            return this.sourceRW.wrap(buffer(), offset() + FIELD_OFFSET_SOURCE, maxLimit());
        }

        protected StringFW.Builder handler()
        {
            return this.handlerRW.wrap(buffer(), source().build().limit() + FIELD_SIZE_SOURCE_REF, maxLimit());
        }
    }
}
