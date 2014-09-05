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

import java.math.BigDecimal;

import org.kaazing.nuklei.concurrent.AtomicBuffer;

/*
 * See AMQP 1.0 specification, section 1.6.15 "decimal128"
 */
public final class Decimal128Type extends Type {

    private static final int OFFSET_KIND = 0;
    private static final int SIZEOF_KIND = 1;

    private static final int OFFSET_HIGH_VALUE = OFFSET_KIND + SIZEOF_KIND;
    private static final int SIZEOF_HIGH_VALUE = 8;

    private static final int OFFSET_LOW_VALUE = OFFSET_HIGH_VALUE + SIZEOF_HIGH_VALUE;
    private static final int SIZEOF_LOW_VALUE = 8;

    @Override
    public Kind kind() {
        return Kind.DECIMAL128;
    }

    @Override
    public Decimal128Type wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);

        switch (uint8Get(buffer, offset + OFFSET_KIND)) {
        case 0x94:
            break;
        default:
            throw new IllegalStateException();
        }

        return this;
    }

    public BigDecimal get() {
        int64Get(buffer(), offset() + OFFSET_HIGH_VALUE);
        int64Get(buffer(), offset() + OFFSET_LOW_VALUE);
        throw new UnsupportedOperationException();
    }

    public int limit() {
        return offset() + OFFSET_LOW_VALUE + SIZEOF_LOW_VALUE;
    }
}
