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
import org.kaazing.nuklei.function.BooleanFunction;
import org.kaazing.nuklei.function.ToBooleanFunction;

/*
 * See AMQP 1.0 specification, section 1.6.2 "boolean"
 */
public final class BooleanType extends Type {

    private static final int OFFSET_KIND = 0;

    private Accessor accessor;
    
    @Override
    public Kind kind() {
        return Kind.BOOLEAN;
    }

    @Override
    public BooleanType watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public BooleanType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        accessor = null;        
        return this;
    }

    public <T> BooleanType set(ToBooleanFunction<T> mutator, T value) {
        return set(mutator.applyAsBoolean(value));
    }

    public BooleanType set(BooleanType value) {
        buffer().putBytes(offset(), value.buffer(), value.offset(), value.limit() - value.offset());
        return this;
    }

    public <T> T get(BooleanFunction<T> accessor) {
        return accessor.apply(get());
    }

    public boolean get() {
        return accessor().get(buffer(), offset());
    }
    
    public BooleanType set(boolean value) {
        // TODO: support zero-or-one
        if (value) {
            // true
            uint8Put(buffer(), offset() + OFFSET_KIND, (short) 0x41);
        }
        else {
            // false
            uint8Put(buffer(), offset() + OFFSET_KIND, (short) 0x42);
        }

        notifyChanged();
        return this;
    }

    public int limit() {
        return offset() + accessor().width();
    }

    private Accessor accessor() {
        if (accessor == null) {
            switch (uint8Get(buffer(), offset() + OFFSET_KIND)) {
            case 0x41:
                accessor = TRUE;
                break;
            case 0x42:
                accessor = FALSE;
                break;
            case 0x56:
                accessor = ZERO_OR_ONE;
                break;
            default:
                throw new IllegalStateException();
            }
        }
        return accessor;
    }
    
    private interface Accessor {
        
        boolean get(AtomicBuffer buffer, int offset);
        
        int width();
    }
    
    private static final Accessor FALSE = new Accessor() {

        @Override
        public boolean get(AtomicBuffer buffer, int offset) {
            return false;
        }

        @Override
        public int width() {
            return 0;
        }
        
    };
    
    private static final Accessor TRUE = new Accessor() {

        @Override
        public boolean get(AtomicBuffer buffer, int offset) {
            return true;
        }

        @Override
        public int width() {
            return 0;
        }
        
    };
    
    private static final Accessor ZERO_OR_ONE = new Accessor() {

        @Override
        public boolean get(AtomicBuffer buffer, int offset) {
            switch (uint8Get(buffer, offset)) {
            case 0x00:
                return false;
            case 0x01:
                return true;
            default:
                throw new IllegalStateException();
            }
        }

        @Override
        public int width() {
            return 1;
        }
        
    };
    
}
