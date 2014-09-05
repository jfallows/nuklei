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
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.function.AtomicBufferAccessor;
import org.kaazing.nuklei.function.AtomicBufferMutator;

/*
 * See AMQP 1.0 specification, section 1.6.19 "binary"
 */
public final class BinaryType extends Type {

    private static final int OFFSET_LENGTH_KIND = 0;
    private static final int SIZEOF_LENGTH_KIND = 1;

    private int offsetValue;
    private int sizeofValue;
    private int limit;
    
    @Override
    public BinaryType watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public Kind kind() {
        return Kind.BINARY;
    }

    @Override
    public BinaryType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);

        int lengthKind = uint8Get(buffer, offset + OFFSET_LENGTH_KIND);
        switch (lengthKind) {
        case 0xa0:
            int offsetLengthA0 = OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;
            int sizeofLengthA0 = 1;
            sizeofValue = uint8Get(buffer, offset + offsetLengthA0);
            offsetValue = offsetLengthA0 + sizeofLengthA0;
            limit = offset + OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND + sizeofLengthA0 + sizeofValue;
            break;
        case 0xb0:
            int offsetLengthB0 = OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;
            int sizeofLengthB0 = 4;
            sizeofValue = int32Get(buffer, offset + offsetLengthB0);
            offsetValue = offsetLengthB0 + sizeofLengthB0;
            limit = offset + OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND + sizeofLengthB0 + sizeofValue;
            break;
        default:
            // unexpected
            break;
        }

        return this;
    }
    
    public <T> T get(AtomicBufferAccessor<T> accessor) {
        return accessor.access(buffer(), offset() + offsetValue, sizeofValue);
    }
    
    public <T> BinaryType set(AtomicBufferMutator<T> mutator, T value) {
        throw new UnsupportedOperationException();
//        notifyChanged();
//        return this;
    }

    public BinaryType set(BinaryType value) {
        buffer().putBytes(offset(), value.buffer(), value.offset(), value.limit() - value.offset());
        notifyChanged();
        return this;
    }

    public int limit() {
        return limit;
    }
}
