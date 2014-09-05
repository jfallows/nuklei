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
import org.kaazing.nuklei.concurrent.AtomicBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.5 "uint"
 */
public final class UIntType extends Type {

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = 1;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;

    private Accessor accessor;
    
    @Override
    public Kind kind() {
        return Kind.UINT;
    }

    @Override
    public UIntType watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public UIntType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        accessor = null;
        return this;
    }

    public <T> UIntType set(ToLongFunction<T> mutator, T value) {
        return set(mutator.applyAsLong(value));
    }

    public UIntType set(long value) {
        switch ((int) highestOneBit(value)) {
        case 0:
            uint8Put(buffer(), offset(), (short) 0x43);
            break;
        case 1:
        case 2:
        case 4:
        case 8:
        case 16:
        case 32:
        case 64:
        case 128:
            uint8Put(buffer(), offset(), (short) 0x52);
            uint8Put(buffer(), offset() + OFFSET_VALUE, (short) value);
            break;
        default:
            uint8Put(buffer(), offset(), (short) 0x70);
            uint32Put(buffer(), offset() + OFFSET_VALUE, value);
            break;
        }
        
        notifyChanged();
        return this;
    }

    public <T> T get(LongFunction<T> accessor) {
        return accessor.apply(get());
    }

    public long get() {
        return accessor().get(buffer(), offset());
    }

    public int limit() {
        return offset() + OFFSET_VALUE + accessor().width();
    }

    private Accessor accessor() {
        if (accessor == null) {
            switch (uint8Get(buffer(), offset() + OFFSET_KIND)) {
            case 0x43:
                accessor = UINT_0;
                break;
            case 0x52:
                accessor = UINT_1;
                break;
            case 0x70:
                accessor = UINT_4;
                break;
            default:
                throw new IllegalStateException();
            }
        }
        return accessor;
    }

    private interface Accessor {
        
        long get(AtomicBuffer buffer, int offset);
        
        int width();
    }

    private static final Accessor UINT_0 = new Accessor() {

        @Override
        public long get(AtomicBuffer buffer, int offset) {
            return 0;
        }

        @Override
        public int width() {
            return 0;
        }
        
    };

    private static final Accessor UINT_1 = new Accessor() {

        @Override
        public long get(AtomicBuffer buffer, int offset) {
            return uint8Get(buffer, offset + OFFSET_VALUE);
        }

        @Override
        public int width() {
            return 1;
        }
        
    };

    private static final Accessor UINT_4 = new Accessor() {

        @Override
        public long get(AtomicBuffer buffer, int offset) {
            return int32Get(buffer, offset + OFFSET_VALUE);
        }

        @Override
        public int width() {
            return 4;
        }
        
    };
}
