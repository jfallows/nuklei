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

/*
 * See AMQP 1.0 specification, section 1.6.24 "array"
 */
public final class ArrayType extends Type {

    private final Length length;
    
    private FlyweightBE previous;

    public ArrayType() {
        this.length = new Length().watch((owner) -> { /* TODO */});
    }

    @Override
    public Kind kind() {
        return Kind.ARRAY;
    }

    @Override
    public ArrayType watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public ArrayType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        length.wrap(buffer, offset);
        previous = length;
        return this;
    }

    public int length() {
        return length.get();
    }

    public boolean hasNext() {
        return previous.limit() < limit();
    }

    public <T extends Type> T next(T element) {
        element.wrap(buffer(), previous.limit());
        previous = element;
        return element;
    }
    
    public int limit() {
        return length.limit() + length.get();
    }

    public ArrayType length(int value) {
        length.set(value);
        notifyChanged();
        return this;
    }
    
    public ArrayType set(ArrayType value) {
        buffer().putBytes(offset(), value.buffer(), value.offset(), value.limit() - value.offset());
        return this;
    }

    private static class Length extends FlyweightBE {

        private static final int OFFSET_LENGTH_KIND = 0;
        private static final int SIZEOF_LENGTH_KIND = 1;

        private static final int OFFSET_LENGTH = OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;

        @Override
        public Length watch(Consumer<Flyweight> observer) {
            super.watch(observer);
            return this;
        }

        @Override
        public Length wrap(AtomicBuffer buffer, int offset) {
            super.wrap(buffer, offset);
            return this;
        }

        public void set(int value) {
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
                lengthKind(0xe0);
                uint8Put(buffer(), offset() + OFFSET_LENGTH, (short) value);
                break;
            default:
                lengthKind(0xf0);
                int32Put(buffer(), offset() + OFFSET_LENGTH, value);
                break;
            }
        }

        public int get() {
            switch (lengthKind()) {
            case 0xe0:
                return uint8Get(buffer(), offset() + OFFSET_LENGTH);
            case 0xf0:
                return int32Get(buffer(), offset() + OFFSET_LENGTH);
            default:
                throw new IllegalStateException();
            }
        }

        public int limit() {
            switch (lengthKind()) {
            case 0xe0:
                return offset() + OFFSET_LENGTH + 1;
            case 0xf0:
                return offset() + OFFSET_LENGTH + 4;
            default:
                throw new IllegalStateException();
            }
        }

        private int lengthKind() {
            return uint8Get(buffer(), offset() + OFFSET_LENGTH_KIND);
        }
        
        private void lengthKind(int lengthKind) {
            switch (lengthKind) {
            case 0xe0:
            case 0xf0:
                break;
            default:
                throw new IllegalStateException();
            }
            uint8Put(buffer(), offset() + OFFSET_LENGTH_KIND, (short) lengthKind);
        }
    }
}
