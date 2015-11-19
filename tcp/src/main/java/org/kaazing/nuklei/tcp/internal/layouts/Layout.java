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
package org.kaazing.nuklei.tcp.internal.layouts;

import uk.co.real_logic.agrona.DirectBuffer;

public abstract class Layout
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

    public final int remaining()
    {
        return limit() - offset();
    }

    protected final Layout wrap(DirectBuffer buffer, int offset)
    {
        this.buffer = buffer;
        this.offset = offset;
        return this;
    }

    protected abstract Layout wrap(DirectBuffer buffer, int offset, int actingLimit);

    protected static final void checkLimit(int limit, int actingLimit)
    {
        if (limit > actingLimit)
        {
            final String msg = String.format("maxLimit=%d is beyond actingLimit=%d", limit, actingLimit);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    public abstract static class Builder<T extends Layout>
    {
        public abstract T mapNewFile();
    }
}
