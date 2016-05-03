/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.http.internal.util;

import uk.co.real_logic.agrona.DirectBuffer;

public final class BufferUtil
{
    public static int limitOfBytes(
        DirectBuffer buffer,
        int offset,
        int limit,
        byte[] value)
    {
        int matchedBytes = 0;

        for (int cursor = offset; cursor < limit; cursor++)
        {
            if (buffer.getByte(cursor) != value[matchedBytes])
            {
                matchedBytes = 0;
                continue;
            }

            if (value.length == ++matchedBytes)
            {
                return cursor + 1;
            }
        }

        return -1;
    }

    private BufferUtil()
    {
        // utility class, no instances
    }
}
