/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.tcp.internal.types;

import static java.lang.String.format;

import java.nio.charset.Charset;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class StringFW extends Flyweight
{
    private static final int FIELD_OFFSET_LENGTH = 0;
    private static final int FIELD_SIZE_LENGTH = BitUtil.SIZE_OF_BYTE;

    @Override
    public int limit()
    {
        return offset() + FIELD_SIZE_LENGTH + length0();
    }

    public String asString()
    {
        return buffer().getStringWithoutLengthUtf8(offset() + FIELD_SIZE_LENGTH, length0());
    }

    @Override
    public StringFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);
        checkLimit(limit(), actingLimit);
        return this;
    }

    @Override
    public String toString()
    {
        return format("\"%s\"", asString());
    }

    private int length0()
    {
        return buffer().getByte(offset() + FIELD_OFFSET_LENGTH) & 0xFF;
    }

    public static final class Builder extends Flyweight.Builder<StringFW>
    {
        public Builder()
        {
            super(new StringFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            return this;
        }

        public Builder set(StringFW value)
        {
            buffer().putBytes(offset(), value.buffer(), value.offset(), value.length());
            return this;
        }

        public Builder set(String value, Charset charset)
        {
            byte[] charBytes = value.getBytes(charset);

            MutableDirectBuffer buffer = buffer();
            int offset = offset();

            buffer.putByte(offset, (byte) charBytes.length);
            buffer.putBytes(offset + 1, charBytes);

            return this;
        }
    }
}
