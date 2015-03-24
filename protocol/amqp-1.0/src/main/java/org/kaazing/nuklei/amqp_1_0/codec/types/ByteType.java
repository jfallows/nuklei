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
 * See AMQP 1.0 specification, section 1.6.7 "byte"
 */
public final class ByteType extends Type
{

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_VALUE = 1;

    static final int SIZEOF_BYTE = SIZEOF_KIND + SIZEOF_VALUE;

    private static final short WIDTH_KIND_1 = 0x51;

    @Override
    public Kind kind()
    {
        return Kind.BYTE;
    }

    @Override
    public ByteType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public ByteType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public ByteType set(byte value)
    {
        widthKind(WIDTH_KIND_1);
        int8Put(mutableBuffer(), offset() + OFFSET_VALUE, value);
        notifyChanged();
        return this;
    }

    public byte get()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_1:
            return int8Get(buffer(), offset() + OFFSET_VALUE);
        default:
            throw new IllegalStateException();
        }
    }

    public int limit()
    {
        return offset() + SIZEOF_BYTE;
    }

    private short widthKind()
    {
        return uint8Get(buffer(), offset() + OFFSET_KIND);
    }

    private void widthKind(short value)
    {
        uint8Put(mutableBuffer(), offset() + OFFSET_KIND, value);
    }
}
