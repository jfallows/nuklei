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
import org.kaazing.nuklei.FlyweightBE;
import org.kaazing.nuklei.concurrent.AtomicBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.23 "map"
 */
public class MapType extends Type {

    private final Length length;
    private FlyweightBE previous;
    
    public MapType() {
        length = new Length();
    }

    @Override
    public Kind kind() {
        return Kind.MAP;
    }

    @Override
    public MapType watch(Consumer<Flyweight> notifier) {
        super.watch(notifier);
        return this;
    }

    @Override
    public MapType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        length.wrap(buffer, offset);
        previous = length;
        return this;
    }

    public int length() {
        return length.value() >> 1;
    }

    public <T extends Type> boolean next(SymbolType key, T value) {
        if (value.limit() < limit()) {
            key.wrap(buffer(), previous.limit());
            value.wrap(buffer(), key.limit());
            previous = value;
            return true;
        }
        return false;
    }

    public int limit() {
        return length.limit() + length.value();
    }

    public MapType set(MapType value) {
        buffer().putBytes(offset(), value.buffer(), value.offset(), value.limit() - value.offset());
        notifyChanged();
        return this;
    }

    private static class Length extends FlyweightBE {

        private static final int OFFSET_LENGTH_KIND = 0;
        private static final int SIZEOF_LENGTH_KIND = 1;

        private static final int OFFSET_LENGTH = OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;

        private int lengthKind;
        
        @Override
        public Length wrap(AtomicBuffer buffer, int offset) {
            super.wrap(buffer, offset);

            lengthKind = uint8Get(buffer(), offset() + OFFSET_LENGTH_KIND);

            return this;
        }

        public int value() {
            switch (lengthKind) {
            case 0xc1:
                return uint8Get(buffer(), offset() + OFFSET_LENGTH);
            case 0xd1:
                return int32Get(buffer(), offset() + OFFSET_LENGTH);
            default:
                throw new IllegalStateException();
            }
        }

        public int limit() {
            switch (lengthKind) {
            case 0xc1:
                return offset() + OFFSET_LENGTH + 1;
            case 0xd1:
                return offset() + OFFSET_LENGTH + 4;
            default:
                throw new IllegalStateException();
            }
        }
    }

}
