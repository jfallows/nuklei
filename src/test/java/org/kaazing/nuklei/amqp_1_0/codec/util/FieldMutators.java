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
package org.kaazing.nuklei.amqp_1_0.codec.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.function.AtomicBufferMutator;

public class FieldMutators {

    public static final AtomicBufferMutator<String> newMutator(final Charset charset) {
        return new AtomicBufferMutator<String>() {
            private final CharsetEncoder encoder = charset.newEncoder();
            private final int maxBytesPerChar = (int) encoder.maxBytesPerChar();
    
            @Override
            public int mutate(Mutation mutation, AtomicBuffer buffer, String value) {
                int offset = mutation.maxOffset(value.length() * maxBytesPerChar);
                ByteBuffer buf = buffer.byteBuffer();
                ByteBuffer out = buf != null ? buf.duplicate() : ByteBuffer.wrap(buffer.array());
                out.position(offset);
                encoder.reset();
                encoder.encode(CharBuffer.wrap(value), out, true);
                encoder.flush(out);
                return out.position() - offset;
            }

        };
    }
}
