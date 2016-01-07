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
package org.kaazing.nuklei.ws.internal.types;

import java.nio.charset.StandardCharsets;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class HeaderFW extends Flyweight
{
    private static final int FIELD_OFFSET_REPRESENTATION = 0;
    private static final int FIELD_SIZE_REPRESENTATION = BitUtil.SIZE_OF_BYTE;

    private final StringFW nameRO = new StringFW();
    private final StringFW valueRO = new StringFW();

    public HeaderFW wrap(DirectBuffer buffer, int offset, int maxLimit)
    {
        super.wrap(buffer, offset, maxLimit);

        this.nameRO.wrap(buffer, offset + FIELD_OFFSET_REPRESENTATION + FIELD_SIZE_REPRESENTATION, maxLimit);
        this.valueRO.wrap(buffer, nameRO.limit(), maxLimit);

        checkLimit(limit(), maxLimit);

        return this;
    }

    @Override
    public int limit()
    {
        return valueRO.limit();
    }

    public int representation()
    {
        return buffer().getByte(offset() + FIELD_OFFSET_REPRESENTATION) >> 4;
    }

    public StringFW name()
    {
        return nameRO;
    }

    public StringFW value()
    {
        return valueRO;
    }

    @Override
    public String toString()
    {
        return String.format("[%s=\"%s\"]", name().asString(), value().asString());
    }

    public static final class Builder extends Flyweight.Builder<HeaderFW>
    {
        private final StringFW.Builder nameRW = new StringFW.Builder();
        private final StringFW.Builder valueRW = new StringFW.Builder();

        public Builder()
        {
            super(new HeaderFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);

            return this;
        }

        public Builder representation(int representation)
        {
            buffer().putByte(offset(), (byte) ((representation & 0x07) << 4));
            return this;
        }

        public Builder set(String name, String value)
        {
            // literal
            representation(0);
            name().set(name, StandardCharsets.UTF_8);
            value().set(value, StandardCharsets.UTF_8);
            return this;
        }

        protected StringFW.Builder name()
        {
            return this.nameRW.wrap(buffer(), offset() + FIELD_OFFSET_REPRESENTATION + FIELD_SIZE_REPRESENTATION, maxLimit());
        }

        protected StringFW.Builder value()
        {
            return this.valueRW.wrap(buffer(), name().build().limit(), maxLimit());
        }
    }
}
