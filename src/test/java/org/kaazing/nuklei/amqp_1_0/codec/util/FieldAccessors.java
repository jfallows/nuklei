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
import java.nio.charset.CharsetDecoder;

import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.function.AtomicBufferAccessor;

public class FieldAccessors {

    public static final AtomicBufferAccessor<String> newAccessor(final Charset charset) {
        return new AtomicBufferAccessor<String>() {
            private final CharsetDecoder decoder = charset.newDecoder();
    
            @Override
            public String access(AtomicBuffer buffer, int offset, int size) {
                ByteBuffer buf = buffer.byteBuffer();
                ByteBuffer in = buf != null ? buf.duplicate() : ByteBuffer.wrap(buffer.array());
                in.position(offset);
                in.limit(offset + size);
                CharBuffer out = CharBuffer.allocate(size);
                decoder.reset();
                decoder.decode(in, out, true);
                decoder.flush(out);
                out.flip();
                return out.toString();
            }
        };
    }
}
