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
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.3 "ubyte"
 */
public final class UByteType extends Type
{

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_VALUE = BitUtil.SIZE_OF_BYTE;

    static final int SIZEOF_UBYTE = SIZEOF_KIND + SIZEOF_VALUE;

    private static final short WIDTH_KIND_1 = 0x50;

    @Override
    public Kind kind()
    {
        return Kind.UBYTE;
    }

    @Override
    public UByteType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public UByteType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public <T> UByteType set(ToIntFunction<T> mutator, T value)
    {
        return set(mutator.applyAsInt(value));
    }

    public UByteType set(int value)
    {
        widthKind(WIDTH_KIND_1);
        uint8Put(mutableBuffer(), offset() + OFFSET_VALUE, (short) value);
        notifyChanged();
        return this;
    }

    public <T> T get(IntFunction<T> accessor)
    {
        return accessor.apply(get());
    }

    public int get()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_1:
            return uint8Get(buffer(), offset() + OFFSET_VALUE);
        default:
            throw new IllegalStateException();
        }
    }

    public int limit()
    {
        return offset() + OFFSET_VALUE + SIZEOF_VALUE;
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
