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
package org.kaazing.nuklei.echo.internal.types.control;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.kaazing.nuklei.echo.internal.types.Flyweight;
import org.kaazing.nuklei.echo.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class BindingFW extends Flyweight
{
    private static final int FIELD_OFFSET_SOURCE = 0;
    private static final int FIELD_SIZE_SOURCE_REF = BitUtil.SIZE_OF_LONG;

    private final StringFW sourceRO = new StringFW();

    public BindingFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.sourceRO.wrap(buffer, offset + FIELD_OFFSET_SOURCE, actingLimit);

        checkLimit(limit(), actingLimit);

        return this;
    }

    public int limit()
    {
        return source().limit() + FIELD_SIZE_SOURCE_REF;
    }

    public StringFW source()
    {
        return sourceRO;
    }

    public long sourceRef()
    {
        return buffer().getLong(source().limit());
    }

    @Override
    public String toString()
    {
        return format("[source=%s, sourceRef=%d]", source(), sourceRef());
    }

    public static final class Builder extends Flyweight.Builder<BindingFW>
    {
        private final StringFW.Builder sourceRW = new StringFW.Builder();

        public Builder()
        {
            super(new BindingFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            return this;
        }

        public Builder source(String source)
        {
            source().set(source, UTF_8);
            return this;
        }

        public Builder sourceRef(long sourceRef)
        {
            buffer().putLong(source().build().limit(), sourceRef);
            return this;
        }

        public StringFW.Builder source()
        {
            return sourceRW.wrap(buffer(), offset() + FIELD_OFFSET_SOURCE, maxLimit());
        }
    }
}
