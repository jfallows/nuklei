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

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.math.MathContext.DECIMAL128;

import java.math.BigDecimal;
import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.15 "decimal128"
 */
public final class Decimal128Type extends Type
{

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = 1;

    private static final int OFFSET_HIGH_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_HIGH_VALUE = 8;

    private static final int OFFSET_LOW_VALUE = OFFSET_HIGH_VALUE + SIZEOF_HIGH_VALUE;
    private static final int SIZEOF_LOW_VALUE = 8;

    static final int SIZEOF_DECIMAL128 = SIZEOF_KIND + SIZEOF_LOW_VALUE + SIZEOF_HIGH_VALUE;

    private static final short WIDTH_KIND_16 = 0x94;

    @Override
    public Kind kind()
    {
        return Kind.DECIMAL128;
    }

    @Override
    public Decimal128Type wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Decimal128Type watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    public Decimal128Type set(BigDecimal value)
    {
        widthKind(WIDTH_KIND_16);
        int64Put(mutableBuffer(), offset() + OFFSET_HIGH_VALUE, toLongHighBits(value));
        int64Put(mutableBuffer(), offset() + OFFSET_LOW_VALUE, toLongLowBits(value));
        notifyChanged();
        return this;
    }

    public BigDecimal get()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_16:
            long high = int64Get(buffer(), offset() + OFFSET_HIGH_VALUE);
            long low = int64Get(buffer(), offset() + OFFSET_LOW_VALUE);
            return fromLongBits(high, low);
        default:
            throw new IllegalStateException();
        }
    }

    public int limit()
    {
        return offset() + SIZEOF_DECIMAL128;
    }

    private void widthKind(short value)
    {
        uint8Put(mutableBuffer(), offset() + OFFSET_KIND, value);
    }

    private int widthKind()
    {
        return uint8Get(buffer(), offset() + OFFSET_KIND);
    }

    private long toLongHighBits(BigDecimal value)
    {
        // TODO: use IEEE 754 decimal128 format (not binary128)
        return doubleToLongBits(value.doubleValue());
    }

    private long toLongLowBits(BigDecimal value)
    {
        // TODO: use IEEE 754 decimal128 format (not binary128)
        return doubleToLongBits(value.doubleValue());
    }

    private BigDecimal fromLongBits(long high, long low)
    {
        // TODO: use IEEE 754 decimal128 format (not binary128)
        return new BigDecimal(longBitsToDouble(low), DECIMAL128);
    }

}
