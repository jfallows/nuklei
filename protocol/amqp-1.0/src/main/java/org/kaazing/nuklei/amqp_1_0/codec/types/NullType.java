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

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.1 "null"
 */
public final class NullType extends Type
{

    private static final short WIDTH_KIND_0 = 0x40;

    @Override
    public Kind kind()
    {
        return Kind.NULL;
    }

    @Override
    public NullType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public NullType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    public Void get()
    {
        switch (widthKind())
        {
        case WIDTH_KIND_0:
            return null;
        default:
            throw new IllegalStateException();
        }
    }

    public NullType set(Void value)
    {
        widthKind(WIDTH_KIND_0);
        notifyChanged();
        return this;
    }

    public int limit()
    {
        return offset() + 1;
    }

    private void widthKind(short value)
    {
        uint8Put(mutableBuffer(), offset(), value);
    }

    private short widthKind()
    {
        return uint8Get(buffer(), offset());
    }

}
