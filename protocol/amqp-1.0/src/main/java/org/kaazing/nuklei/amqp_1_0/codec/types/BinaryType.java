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
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.19 "binary"
 */
public final class BinaryType extends Type
{

    private static final int OFFSET_LENGTH_KIND = 0;
    private static final int SIZEOF_LENGTH_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int OFFSET_LENGTH = OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;
    static final int SIZEOF_LENGTH_MAX = BitUtil.SIZE_OF_INT;

    private static final short WIDTH_KIND_1 = 0xa0;
    private static final short WIDTH_KIND_4 = 0xb0;

    @Override
    public Kind kind()
    {
        return Kind.BINARY;
    }

    @Override
    public BinaryType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public BinaryType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public <T> T get(DirectBufferAccessor<T> accessor)
    {

        switch (widthKind())
        {
        case WIDTH_KIND_1:
            return accessor.access(buffer(), lengthLimit(), uint8Get(buffer(), offset() + OFFSET_LENGTH));
        case WIDTH_KIND_4:
            return accessor.access(buffer(), lengthLimit(), int32Get(buffer(), offset() + OFFSET_LENGTH));
        default:
            throw new IllegalStateException();
        }
    }

    public <T> BinaryType set(MutableDirectBufferMutator<T> mutator, T value)
    {
        length(mutator.mutate((length) ->
        {
            maxLength(length);
            return lengthLimit();
        }, mutableBuffer(), value));
        notifyChanged();
        return this;
    }

    private void length(long length)
    {
        switch (widthKind())
        {
        case WIDTH_KIND_1:
            uint8Put(mutableBuffer(), offset() + OFFSET_LENGTH, (short) length);
            break;
        case WIDTH_KIND_4:
            uint32Put(mutableBuffer(), offset() + OFFSET_LENGTH, length);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    public int limit()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_1:
            return lengthLimit() + uint8Get(buffer(), offset() + OFFSET_LENGTH);
        case WIDTH_KIND_4:
            return lengthLimit() + int32Get(buffer(), offset() + OFFSET_LENGTH);
        default:
            throw new IllegalStateException();
        }
    }

    private void widthKind(short widthKind)
    {
        uint8Put(mutableBuffer(), offset() + OFFSET_LENGTH_KIND, widthKind);
    }

    private short widthKind()
    {
        return uint8Get(buffer(), offset() + OFFSET_LENGTH_KIND);
    }

    private void maxLength(int value)
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
            widthKind(WIDTH_KIND_1);
            break;
        default:
            widthKind(WIDTH_KIND_4);
            break;
        }
    }

    private int lengthLimit()
    {

        switch (widthKind())
        {
        case WIDTH_KIND_1:
            return offset() + OFFSET_LENGTH + BitUtil.SIZE_OF_BYTE;
        case WIDTH_KIND_4:
            return offset() + OFFSET_LENGTH + BitUtil.SIZE_OF_INT;
        default:
            throw new IllegalStateException();
        }
    }

}
