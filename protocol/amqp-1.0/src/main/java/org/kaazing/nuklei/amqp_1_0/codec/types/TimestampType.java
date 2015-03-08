/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
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
package org.kaazing.nuklei.amqp_1_0.codec.types;

import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.17 "timestamp"
 */
public final class TimestampType extends Type
{

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_VALUE = BitUtil.SIZE_OF_LONG;

    static final int SIZEOF_TIMESTAMP = SIZEOF_KIND + SIZEOF_VALUE;

    private static final short WIDTH_KIND_8 = 0x83;

    @Override
    public Kind kind()
    {
        return Kind.TIMESTAMP;
    }

    @Override
    public TimestampType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public TimestampType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public TimestampType set(long value)
    {
        uint8Put(mutableBuffer(), offset() + OFFSET_KIND, WIDTH_KIND_8);
        int64Put(mutableBuffer(), offset() + OFFSET_VALUE, value);
        notifyChanged();
        return this;
    }

    public long get()
    {
        switch (uint8Get(buffer(), offset() + OFFSET_KIND))
        {
        case WIDTH_KIND_8:
            return int64Get(buffer(), offset() + OFFSET_VALUE);
        default:
            throw new IllegalStateException();
        }
    }

    public int limit()
    {
        return offset() + SIZEOF_TIMESTAMP;
    }
}
