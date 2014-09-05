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
 * See AMQP 1.0 specification, section 1.6.22 "list"
 */
public class ListType extends Type {

    private final Header header;
    private FlyweightBE previous;
    
    public ListType() {
        super();
        this.header = new Header();
    }

    @Override
    public Kind kind() {
        return Kind.LIST;
    }

    @Override
    public ListType watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public ListType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);

        header.wrap(buffer, offset);
        previous = header;

        return this;
    }
    
    public ListType clear() {
        limit(0, offsetBody());
        return this;
    }

    public boolean hasNext() {
        return previous.limit() < limit();
    }

    public <T extends Type> T next(T element) {
        element.wrap(buffer(), previous.limit());
        previous = element;
        return element;
    }

    public int length() {
        return header.length();
    }

    public ListType maxLength(int value) {
        header.max(value);
        return this;
    }
    
    public int count() {
        return header.count();
    }
    
    public ListType maxCount(int value) {
        header.max(value);
        return this;
    }
    
    @Override
    public int limit() {
        return header.lengthLimit() + header.length();
    }
    
    protected final void limit(int count, int limit) {
        header.count(count);
        header.length(limit - header.lengthLimit());
        notifyChanged();
    }

    protected final int offsetBody() {
        return header.limit();
    }

    private static final class Header extends FlyweightBE {

        private static final int OFFSET_LENGTH_KIND = 0;
        private static final int SIZEOF_LENGTH_KIND = 1;
        private static final int OFFSET_LENGTH = OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;

        @Override
        public Header wrap(AtomicBuffer buffer, int offset) {
            super.wrap(buffer, offset);
            return this;
        }

        public void count(int value) {
            switch (kind()) {
            case 0x45:
                if (value != 0) {
                    throw new IllegalStateException();
                }
                break;
            case 0xc0:
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
                    uint8Put(buffer(), offset() + OFFSET_LENGTH + 1, (short) value);
                    break;
                default:
                    throw new IllegalStateException();
                }
                break;
            case 0xd0:
                int32Put(buffer(), offset() + OFFSET_LENGTH + 4, value);
                break;
            default:
                throw new IllegalStateException();
            }
        }

        public int count() {
            switch (kind()) {
            case 0x45:
                return 0;
            case 0xc0:
                return uint8Get(buffer(), offset() + OFFSET_LENGTH + 1);
            case 0xd0:
                return int32Get(buffer(), offset() + OFFSET_LENGTH + 4);
            default:
                throw new IllegalStateException();
            }
        }

        public int length() {
            switch (kind()) {
            case 0x45:
                return 0;
            case 0xc0:
                return uint8Get(buffer(), offset() + OFFSET_LENGTH);
            case 0xd0:
                return int32Get(buffer(), offset() + OFFSET_LENGTH);
            default:
                throw new IllegalStateException();
            }
        }
        
        public int lengthLimit() {
            switch (kind()) {
            case 0x45:
                return offset() + OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;
            case 0xc0:
                return offset() + OFFSET_LENGTH + 1;
            case 0xd0:
                return offset() + OFFSET_LENGTH + 4;
            default:
                throw new IllegalStateException();
            }
        }
        
        public void length(int value) {
            switch (kind()) {
            case 0x45:
                if (value != 0) {
                    throw new IllegalStateException();
                }
                break;
            case 0xc0:
                switch (highestOneBit(value)) {
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
            case 0xd0:
                int32Put(buffer(), offset() + OFFSET_LENGTH, value);
                break;
            default:
                throw new IllegalStateException();
            }
        }

        public void max(int value) {
            switch (highestOneBit(value)) {
            case 0:
                kind(0x45);
                break;
            case 1:
            case 2:
            case 4:
            case 8:
            case 16:
            case 32:
            case 64:
            case 128:
                kind(0xc0);
                break;
            default:
                kind(0xd0);
                break;
            }
            
        }
        
        public int limit() {
            switch (kind()) {
            case 0x45:
                return offset() + OFFSET_LENGTH_KIND + SIZEOF_LENGTH_KIND;
            case 0xc0:
                return offset() + OFFSET_LENGTH + 2;
            case 0xd0:
                return offset() + OFFSET_LENGTH + 8;
            default:
                throw new IllegalStateException();
            }
        }

        private void kind(int kind) {
            uint8Put(buffer(), offset() + OFFSET_LENGTH_KIND, (short) kind);
        }

        private int kind() {
            return uint8Get(buffer(), offset() + OFFSET_LENGTH_KIND);
        }
    }
}
