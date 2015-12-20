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
package org.kaazing.nuklei.http.internal.types;

import java.util.function.Consumer;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class HeadersFW extends Flyweight
{
    private final HeaderFW headerRO = new HeaderFW();

    public HeadersFW wrap(DirectBuffer buffer, int offset, int maxLimit)
    {
        super.wrap(buffer, offset, maxLimit);

        forEach((header) -> {});

        checkLimit(limit(), maxLimit);

        return this;
    }

    @Override
    public int limit()
    {
        return headerRO.limit();
    }

    public HeadersFW forEach(Consumer<HeaderFW> consumer)
    {
        int offset = offset();

        while (offset < maxLimit())
        {
            consumer.accept(headerRO.wrap(buffer(), offset, maxLimit()));
            offset = headerRO.limit();
        }

        return this;
    }

    public static final class Builder extends Flyweight.Builder<HeadersFW>
    {
        private final HeaderFW.Builder headerRW = new HeaderFW.Builder();

        public Builder()
        {
            super(new HeadersFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);

            headerRW.wrap(buffer, offset, maxLimit);

            return this;
        }

        public Builder header(String name, String value)
        {
            headerRW.set(name, value);
            limit(headerRW.build().limit());

            headerRW.wrap(buffer(), limit(), maxLimit());
            return this;
        }
    }
}
