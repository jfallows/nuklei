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

import static java.lang.Long.highestOneBit;

import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.FlyweightBE;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.6 "ulong"
 */
public final class ULongType extends Type
{

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_VALUE_MAX = BitUtil.SIZE_OF_LONG;

    static final int SIZEOF_ULONG_MAX = SIZEOF_KIND + SIZEOF_VALUE_MAX;

    private static final short WIDTH_KIND_0 = 0x44;
    private static final short WIDTH_KIND_1 = 0x53;
    private static final short WIDTH_KIND_8 = 0x80;

    @Override
    public Kind kind()
    {
        return Kind.ULONG;
    }

    @Override
    public ULongType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public ULongType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public <R> R get(LongFunction<R> accessor)
    {
        return accessor.apply(get());
    }

    public long get()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_0:
            return 0L;
        case WIDTH_KIND_1:
            return uint8Get(buffer(), offset() + OFFSET_VALUE);
        case WIDTH_KIND_8:
            return int64Get(buffer(), offset() + OFFSET_VALUE);
        default:
            throw new IllegalStateException();
        }
    }

    public <T> ULongType set(ToLongFunction<T> mutator, T value)
    {
        return set(mutator.applyAsLong(value));
    }

    public ULongType set(long value)
    {
        switch ((int) highestOneBit(value))
        {
        case 0:
            widthKind(WIDTH_KIND_0);
            break;
        case 1:
        case 2:
        case 4:
        case 8:
        case 16:
        case 32:
        case 64:
        case 128:
            widthKind(WIDTH_KIND_1);
            uint8Put(mutableBuffer(), offset() + OFFSET_VALUE, (short) value);
            break;
        default:
            widthKind(WIDTH_KIND_8);
            int64Put(mutableBuffer(), offset() + OFFSET_VALUE, value);
            break;
        }

        notifyChanged();
        return this;
    }

    public int limit()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_0:
            return offset() + OFFSET_VALUE;
        case WIDTH_KIND_1:
            return offset() + OFFSET_VALUE + 1;
        case WIDTH_KIND_8:
            return offset() + OFFSET_VALUE + 8;
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

    /*
     * See AMQP 1.0 specification, section 1.5 "Descriptor Values"
     */
    public static class Descriptor extends FlyweightBE
    {

        private static final int OFFSET_CODE = 1;

        private final ULongType code;

        public Descriptor()
        {
            this.code = new ULongType().watch((owner) -> notifyChanged());
        }

        @Override
        public Descriptor watch(Consumer<Flyweight> observer)
        {
            super.watch(observer);
            return this;
        }

        @Override
        public Descriptor wrap(DirectBuffer buffer, int offset, boolean mutable)
        {
            super.wrap(buffer, offset, mutable);

            code.wrap(buffer, offset + OFFSET_CODE, mutable);

            return this;
        }

        public <T> Descriptor set(ToLongFunction<T> mutator, T value)
        {
            code.set(mutator, value);
            return this;
        }

        public Descriptor set(long value)
        {
            mutableBuffer().putByte(offset(), (byte)0x00);
            code.set(value);
            return this;
        }

        public <R> R get(LongFunction<R> accessor)
        {
            return code.get(accessor);
        }

        public long get()
        {
            return code.get();
        }

        public int limit()
        {
            return code.limit();
        }
    }
}
