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
package org.kaazing.nuklei.echo.internal.types;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public abstract class Flyweight
{
    private DirectBuffer buffer;
    private int offset;

    public final DirectBuffer buffer()
    {
        return buffer;
    }

    public final int offset()
    {
        return offset;
    }

    public abstract int limit();

    public final int length()
    {
        return limit() - offset();
    }

    protected final Flyweight wrap(DirectBuffer buffer, int offset)
    {
        this.buffer = buffer;
        this.offset = offset;
        return this;
    }

    protected abstract Flyweight wrap(DirectBuffer buffer, int offset, int actingLimit);

    protected static final void checkLimit(int limit, int actingLimit)
    {
        if (limit > actingLimit)
        {
            final String msg = String.format("maxLimit=%d is beyond actingLimit=%d", limit, actingLimit);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    public abstract static class Builder<T extends Flyweight>
    {
        private final T flyweight;

        private MutableDirectBuffer buffer;
        private int offset;
        private int maxLimit;

        public final int maxLimit()
        {
            return maxLimit;
        }

        public final T build()
        {
            flyweight.wrap(buffer, offset, maxLimit);
            return flyweight;
        }

        protected Builder(T flyweight)
        {
            this.flyweight = flyweight;
        }

        protected final T flyweight()
        {
            return flyweight;
        }

        protected final MutableDirectBuffer buffer()
        {
            return buffer;
        }

        protected final int offset()
        {
            return offset;
        }

        protected final void maxLimit(int limit)
        {
            this.maxLimit = limit;
        }

        protected Builder<T> wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            this.buffer = buffer;
            this.offset = offset;
            this.maxLimit = maxLimit;
            return this;
        }
    }
}
