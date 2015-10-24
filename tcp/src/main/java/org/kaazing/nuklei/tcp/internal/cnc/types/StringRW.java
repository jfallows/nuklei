/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.tcp.internal.cnc.types;

import java.nio.charset.Charset;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class StringRW extends StringType<MutableDirectBuffer>
{
    private final StringRO readonly = new StringRO();

    public StringRW wrap(MutableDirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset);
        return this;
    }

    public StringRW set(StringType<?> value)
    {
        buffer().putBytes(offset(), value.buffer(), value.offset(), value.remaining());
        return this;
    }

    public StringRW set(String value, Charset charset)
    {
        byte[] charBytes = value.getBytes(charset);
        buffer().putByte(offset(), (byte) charBytes.length);
        buffer().putBytes(offset() + 1, charBytes);
        return this;
    }

    public StringRO asReadOnly()
    {
        readonly.wrap(buffer(), offset());
        return readonly;
    }
}
