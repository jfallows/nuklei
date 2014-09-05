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

/*
 * See AMQP 1.0 specification, section 1.6.4 "ushort"
 */
public final class UShortType extends Type {

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = 1;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_VALUE = 2;

    @Override
    public Kind kind() {
        return Kind.USHORT;
    }

    @Override
    public UShortType watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public UShortType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        return this;
    }

    public UShortType set(int value) {
        uint8Put(buffer(), offset() + OFFSET_KIND, (short) 0x60);
        uint16Put(buffer(), offset() + OFFSET_VALUE, value);
        notifyChanged();
        return this;
    }

    public int get() {
        switch (uint8Get(buffer(), offset() + OFFSET_KIND)) {
        case 0x60:
            return uint16Get(buffer(), offset() + OFFSET_VALUE);
        default:
            throw new IllegalStateException();
        }
    }

    public int limit() {
        return offset() + OFFSET_VALUE + SIZEOF_VALUE;
    }
}
