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
 * See AMQP 1.0 specification, section 1.6.1 "null"
 */
public final class NullType extends Type {

    @Override
    public Kind kind() {
        return Kind.NULL;
    }

    @Override
    public NullType wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        return this;
    }
    
    public Void get() {
        switch (uint8Get(buffer(), offset())) {
        case 0x40:
            return null;
        default:
            throw new IllegalStateException();
        }
    }
    
    public void set(Void value) {
        uint8Put(buffer(), offset(), (short) 0x40);
    }
    
    public int limit() {
        return offset() + 1;
    }

}
