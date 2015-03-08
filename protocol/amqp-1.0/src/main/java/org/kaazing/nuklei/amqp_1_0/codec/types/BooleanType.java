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
import org.kaazing.nuklei.function.BooleanFunction;
import org.kaazing.nuklei.function.ToBooleanFunction;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.2 "boolean"
 */
public final class BooleanType extends Type
{

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_VALUE_MAX = BitUtil.SIZE_OF_BYTE;

    static final int SIZEOF_BOOLEAN_MAX = SIZEOF_KIND + SIZEOF_VALUE_MAX;

    private static final short WIDTH_KIND_0_TRUE = 0x41;
    private static final short WIDTH_KIND_0_FALSE = 0x42;
    private static final short WIDTH_KIND_1 = 0x56;

    private static final short WIDTH_KIND_1_VALUE_FALSE = 0x00;
    private static final short WIDTH_KIND_1_VALUE_TRUE = 0x01;

    @Override
    public Kind kind()
    {
        return Kind.BOOLEAN;
    }

    @Override
    public BooleanType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public BooleanType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public <T> BooleanType set(ToBooleanFunction<T> mutator, T value)
    {
        return set(mutator.applyAsBoolean(value));
    }

    public <T> T get(BooleanFunction<T> accessor)
    {
        return accessor.apply(get());
    }

    public BooleanType set(boolean value)
    {
        // TODO: support zero-or-one
        if (value)
        {
            widthKind(WIDTH_KIND_0_TRUE);
        }
        else
        {
            widthKind(WIDTH_KIND_0_FALSE);
        }

        notifyChanged();
        return this;
    }

    public boolean get()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_0_TRUE:
            return true;
        case WIDTH_KIND_0_FALSE:
            return false;
        case WIDTH_KIND_1:
            switch (uint8Get(buffer(), offset() + OFFSET_VALUE))
            {
            case WIDTH_KIND_1_VALUE_FALSE:
                return false;
            case WIDTH_KIND_1_VALUE_TRUE:
                return true;
            default:
                throw new IllegalStateException();
            }
        default:
            throw new IllegalStateException();
        }
    }

    public int limit()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_0_TRUE:
        case WIDTH_KIND_0_FALSE:
            return offset() + OFFSET_VALUE;
        case WIDTH_KIND_1:
            return offset() + OFFSET_VALUE + 1;
        default:
            throw new IllegalStateException();
        }
    }

    private void widthKind(short value)
    {
        uint8Put(mutableBuffer(), offset() + OFFSET_KIND, value);
    }

    private short widthKind()
    {
        return uint8Get(buffer(), offset() + OFFSET_KIND);
    }

}
