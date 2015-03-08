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

import static java.lang.Integer.highestOneBit;

import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.FlyweightBE;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;
import org.kaazing.nuklei.function.MutableDirectBufferMutator.Mutation;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.21 "symbol"
 */
public final class SymbolType extends Type
{

    private final Length length;

    public SymbolType()
    {
        length = new Length();
    }

    @Override
    public Kind kind()
    {
        return Kind.SYMBOL;
    }

    @Override
    public SymbolType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public SymbolType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        length.wrap(buffer, offset, mutable);
        return this;
    }

    public <T> T get(DirectBufferAccessor<T> accessor)
    {
        return accessor.access(buffer(), length.limit(), length.get());
    }

    public <T> SymbolType set(MutableDirectBufferMutator<T> mutator, T value)
    {
        length.set(mutator.mutate(length.maxOffset(), mutableBuffer(), value));
        notifyChanged();
        return this;
    }

    public SymbolType set(SymbolType value)
    {
        mutableBuffer().putBytes(offset(), value.buffer(), value.offset(), value.limit() - value.offset());
        notifyChanged();
        return this;
    }

    public int limit()
    {
        return length.limit() + length.get();
    }

    private static final class Length extends FlyweightBE
    {

        private static final int OFFSET_LENGTH_KIND = 0;
        private static final int SIZEOF_LENGTH_KIND = BitUtil.SIZE_OF_BYTE;
        private static final int OFFSET_LENGTH = OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;

        private static final short WIDTH_KIND_1 = 0xa3;
        private static final short WIDTH_KIND_4 = 0xb3;

        private final Mutation maxOffset = (value) ->
        {
            max(value);
            return limit();
        };

        @Override
        public Length wrap(DirectBuffer buffer, int offset, boolean mutable)
        {
            super.wrap(buffer, offset, mutable);
            return this;
        }

        public Mutation maxOffset()
        {
            return maxOffset;
        }

        public int get()
        {
            switch (widthKind())
            {
            case WIDTH_KIND_1:
                return uint8Get(buffer(), offset() + OFFSET_LENGTH);
            case WIDTH_KIND_4:
                return int32Get(buffer(), offset() + OFFSET_LENGTH);
            default:
                throw new IllegalStateException();
            }
        }

        public void set(int value)
        {
            switch (widthKind())
            {
            case WIDTH_KIND_1:
                switch (highestOneBit(value))
                {
                case 0:
                case 1:
                case 2:
                case 4:
                case 8:
                case 16:
                case 32:
                case 64:
                case 128:
                    uint8Put(mutableBuffer(), offset() + OFFSET_LENGTH, (short) value);
                    break;
                default:
                    throw new IllegalStateException();
                }
                break;
            case WIDTH_KIND_4:
                int32Put(mutableBuffer(), offset() + OFFSET_LENGTH, value);
                break;
            default:
                throw new IllegalStateException();
            }
        }

        public void max(int value)
        {
            switch (highestOneBit(value))
            {
            case 0:
            case 1:
            case 2:
            case 4:
            case 8:
            case 16:
            case 32:
            case 64:
            case 128:
                lengthKind(WIDTH_KIND_1);
                break;
            default:
                lengthKind(WIDTH_KIND_4);
                break;
            }

        }

        public int limit()
        {
            switch (widthKind())
            {
            case WIDTH_KIND_1:
                return offset() + OFFSET_LENGTH + 1;
            case WIDTH_KIND_4:
                return offset() + OFFSET_LENGTH + 4;
            default:
                throw new IllegalStateException();
            }
        }

        private void lengthKind(short lengthKind)
        {
            uint8Put(mutableBuffer(), offset() + OFFSET_LENGTH_KIND, lengthKind);
        }

        private int widthKind()
        {
            return uint8Get(buffer(), offset() + OFFSET_LENGTH_KIND);
        }
    }

    /*
     * See AMQP 1.0 specification, section 1.5 "Descriptor Values"
     */
    public static final class Descriptor extends FlyweightBE
    {

        private static final int OFFSET_CODE = 1;

        private final SymbolType code;

        public Descriptor()
        {
            this.code = new SymbolType();
        }

        @Override
        public Descriptor wrap(DirectBuffer buffer, int offset, boolean mutable)
        {
            super.wrap(buffer, offset, true);

            code.wrap(buffer, offset + OFFSET_CODE, true);

            return this;
        }

        public <T> T get(DirectBufferAccessor<T> accessor)
        {
            return code.get(accessor);
        }

        public int limit()
        {
            return code.limit();
        }
    }
}
