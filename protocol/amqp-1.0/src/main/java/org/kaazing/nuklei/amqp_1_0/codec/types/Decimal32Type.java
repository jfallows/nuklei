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

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;
import static java.math.MathContext.DECIMAL32;

import java.math.BigDecimal;
import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.13 "decimal32"
 */
public final class Decimal32Type extends Type
{

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_VALUE_MAX = BitUtil.SIZE_OF_INT;

    static final int SIZEOF_INT_MAX = SIZEOF_KIND + SIZEOF_VALUE_MAX;

    private static final short WIDTH_KIND_4 = 0x74;

    @Override
    public Kind kind()
    {
        return Kind.DECIMAL32;
    }

    @Override
    public Decimal32Type watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Decimal32Type wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public Decimal32Type set(BigDecimal value)
    {
        widthKind(WIDTH_KIND_4);
        int32Put(mutableBuffer(), offset() + OFFSET_VALUE, toIntBits(value));
        notifyChanged();
        return this;
    }

    public BigDecimal get()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_4:
            return fromIntBits(int32Get(buffer(), offset() + OFFSET_VALUE));
        default:
            throw new IllegalStateException();
        }
    }

    public int limit()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_4:
            return offset() + OFFSET_VALUE + 4;
        default:
            throw new IllegalStateException();
        }
    }

    private void widthKind(short value)
    {
        uint8Put(mutableBuffer(), offset() + OFFSET_KIND, value);
    }

    private int widthKind()
    {
        return uint8Get(buffer(), offset() + OFFSET_KIND);
    }

    private int toIntBits(BigDecimal value)
    {
        // TODO: use IEEE 754 decimal32 format (not binary32)
        return floatToIntBits(value.floatValue());
    }

    private BigDecimal fromIntBits(int value)
    {
        // TODO: use IEEE 754 decimal32 format (not binary32)
        return new BigDecimal(intBitsToFloat(value), DECIMAL32);
    }
}
