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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.types;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

public abstract class StringType<T extends DirectBuffer> extends Type<T>
{
    private static final int FIELD_OFFSET_LENGTH = 0;
    private static final int FIELD_SIZE_LENGTH = BitUtil.SIZE_OF_BYTE;

    public int length()
    {
        return buffer().getByte(offset() + FIELD_OFFSET_LENGTH) & 0xFF;
    }

    public int limit()
    {
        return offset() + FIELD_SIZE_LENGTH + length();
    }

    public String asString()
    {
        return buffer().getStringWithoutLengthUtf8(offset() + FIELD_SIZE_LENGTH, length());
    }

    @Override
    public String toString()
    {
        return String.format("\"%s\"", asString());
    }
}
