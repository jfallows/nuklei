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

import java.util.UUID;
import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.18 "uuid"
 */
public final class UuidType extends Type
{

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = BitUtil.SIZE_OF_BYTE;

    private static final int OFFSET_MSB = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_MSB = BitUtil.SIZE_OF_LONG;

    private static final int OFFSET_LSB = OFFSET_MSB + SIZEOF_MSB;
    private static final int SIZEOF_LSB = BitUtil.SIZE_OF_LONG;

    static final int SIZEOF_UUID = SIZEOF_KIND + SIZEOF_MSB + SIZEOF_LSB;

    private static final short WIDTH_KIND_16 = 0x98;

    @Override
    public Kind kind()
    {
        return Kind.UUID;
    }

    @Override
    public UuidType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public UuidType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public UuidType set(UUID uuid)
    {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();

        widthKind(WIDTH_KIND_16);
        int64Put(mutableBuffer(), offset() + OFFSET_MSB, mostSigBits);
        int64Put(mutableBuffer(), offset() + OFFSET_LSB, leastSigBits);

        notifyChanged();
        return this;
    }

    public UUID get()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_16:
            long mostSigBits = int64Get(buffer(), offset() + OFFSET_MSB);
            long leastSigBits = int64Get(buffer(), offset() + OFFSET_LSB);
            return new UUID(mostSigBits, leastSigBits);
        default:
            throw new IllegalStateException();
        }
    }

    public int limit()
    {
        return offset() + SIZEOF_UUID;
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
