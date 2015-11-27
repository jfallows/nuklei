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

public final class PreparationFW extends Flyweight
{
    private static final int FIELD_OFFSET_DESTINATION = 0;
    private static final int FIELD_SIZE_DESTINATION_REF = BitUtil.SIZE_OF_LONG;

    private final StringFW destinationRO = new StringFW();

    public PreparationFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.destinationRO.wrap(buffer, offset + FIELD_OFFSET_DESTINATION, actingLimit);

        checkLimit(limit(), actingLimit);

        return this;
    }

    public int limit()
    {
        return destination().limit() + FIELD_SIZE_DESTINATION_REF;
    }

    public StringFW destination()
    {
        return destinationRO;
    }

    public long destinationRef()
    {
        return buffer().getLong(destination().limit());
    }

    @Override
    public String toString()
    {
        return format("[destination=%s, destinationRef=%d]", destination(), destinationRef());
    }

    public static final class Builder extends Flyweight.Builder<PreparationFW>
    {
        private final StringFW.Builder destinationRW = new StringFW.Builder();

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

        public Builder destinationRef(long destinationRef)
        {
            buffer().putLong(destination().build().limit(), destinationRef);
            return this;
        }

        public StringFW.Builder destination()
        {
            return destinationRW.wrap(buffer(), offset() + FIELD_OFFSET_DESTINATION, maxLimit());
        }
    }
}
