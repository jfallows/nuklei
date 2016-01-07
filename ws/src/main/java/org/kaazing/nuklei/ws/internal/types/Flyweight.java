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

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public abstract class Flyweight
{
    private DirectBuffer buffer;
    private int offset;
    private int maxLimit;

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

    protected final int maxLimit()
    {
        return maxLimit;
    }

    protected Flyweight wrap(DirectBuffer buffer, int offset, int maxLimit)
    {
        this.buffer = buffer;
        this.offset = offset;
        this.maxLimit = maxLimit;
        return this;
    }

    protected static final void checkLimit(int limit, int maxLimit)
    {
        if (limit > maxLimit)
        {
            final String msg = String.format("limit=%d is beyond maxLimit=%d", limit, maxLimit);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    public abstract static class Builder<T extends Flyweight>
    {
        private final T flyweight;

        private MutableDirectBuffer buffer;
        private int offset;
        private int limit;
        private int maxLimit;

        public final int limit()
        {
            return limit;
        }

        public final int maxLimit()
        {
            return maxLimit;
        }

        public final T build()
        {
            flyweight.wrap(buffer, offset, limit);
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

        protected final void limit(int limit)
        {
            this.limit = limit;
        }

        protected Builder<T> wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            this.buffer = buffer;
            this.offset = offset;
            this.limit = maxLimit;
            this.maxLimit = maxLimit;
            return this;
        }
    }
}
