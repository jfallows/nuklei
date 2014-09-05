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

import org.kaazing.nuklei.concurrent.AtomicBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.12 "double"
 */
public final class DoubleType extends Type {

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = 1;

    private static final int OFFSET_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_VALUE = 8;

    @Override
    public Kind kind() {
        return Kind.DOUBLE;
    }

    @Override
    public DoubleType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);

        switch (uint8Get(buffer, offset + OFFSET_KIND)) {
        case 0x82:
            break;
        default:
            throw new IllegalStateException();
        }

        return this;
    }

    public double get() {
        return doubleGet(buffer(), offset() + OFFSET_VALUE);
    }

    public int limit() {
        return offset() + OFFSET_VALUE + SIZEOF_VALUE;
    }
}
