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
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.FlyweightBE;
import org.kaazing.nuklei.concurrent.AtomicBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.6 "ulong"
 */
public final class ULongType extends Type {

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = 1;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;

    private Accessor accessor;
    
    @Override
    public Kind kind() {
        return Kind.ULONG;
    }

    @Override
    public ULongType watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public ULongType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        accessor = null;
        return this;
    }

    public <R> R get(LongFunction<R> accessor) {
        return accessor.apply(accessor().get(buffer(), offset()));
    }
    
    public long get() {
        return accessor().get(buffer(), offset());
    }

    public <T> ULongType set(ToLongFunction<T> mutator, T value) {
        return set(mutator.applyAsLong(value));
    }
    
    public ULongType set(long value) {
        if (value == 0L) {
            uint8Put(buffer(), offset() + OFFSET_KIND, (short) 0x44);
        }
        else if ((value | 0xffL) == 0xffL) {
            uint8Put(buffer(), offset() + OFFSET_KIND, (short) 0x53);
            uint8Put(buffer(), offset() + OFFSET_VALUE, (short) (value & 0xff));
        }
        else {
            uint8Put(buffer(), offset() + OFFSET_KIND, (short) 0x80);
            int64Put(buffer(), offset() + OFFSET_VALUE, value);
        }

        notifyChanged();
        return this;
    }

    public int limit() {
        return offset() + OFFSET_VALUE + accessor().width();
    }

    private Accessor accessor() {
        if (accessor == null) {
            switch (uint8Get(buffer(), offset() + OFFSET_KIND)) {
            case 0x44:
                accessor = ULONG_0;
                break;
            case 0x53:
                accessor = ULONG_1;
                break;
            case 0x80:
                accessor = ULONG_8;
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

    private static final Accessor ULONG_0 = new Accessor() {

        @Override
        public long get(AtomicBuffer buffer, int offset) {
            return 0;
        }

        @Override
        public int width() {
            return 0;
        }
        
    };

    private static final Accessor ULONG_1 = new Accessor() {

        @Override
        public long get(AtomicBuffer buffer, int offset) {
            return uint8Get(buffer, offset + OFFSET_VALUE);
        }

        @Override
        public int width() {
            return 1;
        }
        
    };

    private static final Accessor ULONG_8 = new Accessor() {

        @Override
        public long get(AtomicBuffer buffer, int offset) {
            return int64Get(buffer, offset + OFFSET_VALUE);
        }

        @Override
        public int width() {
            return 8;
        }
        
    };

    /*
     * See AMQP 1.0 specification, section 1.5 "Descriptor Values"
     */
    public static class Descriptor extends FlyweightBE {

        private static final int OFFSET_CODE = 1;

        private final ULongType code;
        
        public Descriptor() {
            this.code = new ULongType();
        }

        @Override
        public Descriptor wrap(AtomicBuffer buffer, int offset) {
            super.wrap(buffer, offset);

            code.wrap(buffer, offset + OFFSET_CODE);

            return this;
        }
        
        public <T> Descriptor set(ToLongFunction<T> mutator, T value) {
            code.set(mutator, value);
            return this;
        }

        public Descriptor set(long value) {
            code.set(value);
            return this;
        }

        public <R> R get(LongFunction<R> accessor) {
            return code.get(accessor);
        }

        public long get() {
            return code.get();
        }

        public int limit() {
            return code.limit();
        }
    }
}
