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
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.function.AtomicBufferAccessor;
import org.kaazing.nuklei.function.AtomicBufferMutator;
import org.kaazing.nuklei.function.AtomicBufferMutator.Mutation;

/*
 * See AMQP 1.0 specification, section 1.6.20 "string"
 */
public final class StringType extends Type {

    private final Nullable nullable;
    private final Length length;
    
    public StringType() {
        length = new Length();
        nullable = new Nullable();
    }

    @Override
    public Kind kind() {
        return Kind.STRING;
    }

    @Override
    public StringType watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public StringType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        nullable.wrap(buffer, offset);
        length.wrap(buffer, offset);
        return this;
    }

    public <T> T get(AtomicBufferAccessor<T> accessor) {
        return nullable.get() ? null : accessor.access(buffer(), length.limit(), length.get());
    }
    
    public StringType set(Void value) {
        nullable.set();
        notifyChanged();
        return this;
    }

    public <T> StringType set(AtomicBufferMutator<T> mutator, T value) {
        length.set(mutator.mutate(length.maxOffset(), buffer(), value));
        notifyChanged();
        return this;
    }

    public StringType set(StringType value) {
        buffer().putBytes(offset(), value.buffer(), value.offset(), value.limit() - value.offset());
        notifyChanged();
        return this;
    }

    public int limit() {
        return nullable.get() ? nullable.limit() : length.limit() + length.get();
    }

    private static final class Nullable extends FlyweightBE {

        public boolean get() {
            return uint8Get(buffer(), offset()) == 0x40;
        }

        public void set() {
            uint8Put(buffer(), offset(), (short) 0x40);
        }

        @Override
        public int limit() {
            return offset() + 1;
        }
        
    }
    
    private static final class Length extends FlyweightBE {

        private static final int OFFSET_LENGTH_KIND = 0;
        private static final int SIZEOF_LENGTH_KIND = 1;
        private static final int OFFSET_LENGTH = OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;
        
        private final Mutation maxOffset = (value) -> { max(value); return limit(); };

        @Override
        public Length wrap(AtomicBuffer buffer, int offset) {
            super.wrap(buffer, offset);
            return this;
        }
        
        public Mutation maxOffset() {
            return maxOffset;
        }

        public int get() {
            switch (lengthKind()) {
            case 0xa1:
                return uint8Get(buffer(), offset() + OFFSET_LENGTH);
            case 0xb1:
                return int32Get(buffer(), offset() + OFFSET_LENGTH);
            default:
                throw new IllegalStateException();
            }
        }
        
        public void set(int value) {
            switch (lengthKind()) {
            case 0xa1:
                switch (highestOneBit(value)) {
                case 0:
                case 1:
                case 2:
                case 4:
                case 8:
                case 16:
                case 32:
                case 64:
                case 128:
                    uint8Put(buffer(), offset() + OFFSET_LENGTH, (short) value);
                    break;
                default:
                    throw new IllegalStateException();
                }
                break;
            case 0xb1:
                int32Put(buffer(), offset() + OFFSET_LENGTH, value);
                break;
            default:
                throw new IllegalStateException();
            }
        }
        
        public void max(int value) {
            switch (highestOneBit(value)) {
            case 0:
            case 1:
            case 2:
            case 4:
            case 8:
            case 16:
            case 32:
            case 64:
            case 128:
                lengthKind(0xa1);
                break;
            default:
                lengthKind(0xb1);
                break;
            }
            
        }
        
        public int limit() {
            switch (lengthKind()) {
            case 0xa1:
                return offset() + OFFSET_LENGTH + 1;
            case 0xb1:
                return offset() + OFFSET_LENGTH + 4;
            default:
                throw new IllegalStateException();
            }
        }

        private void lengthKind(int lengthKind) {
            uint8Put(buffer(), offset() + OFFSET_LENGTH_KIND, (short) lengthKind);
        }

        private int lengthKind() {
            return uint8Get(buffer(), offset() + OFFSET_LENGTH_KIND);
        }
    }
}
