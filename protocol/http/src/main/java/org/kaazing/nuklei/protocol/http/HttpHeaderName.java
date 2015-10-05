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
package org.kaazing.nuklei.protocol.http;

import org.kaazing.nuklei.protocol.ProtocolUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public enum HttpHeaderName
{
    METHOD(),
    PATH(),
    STATUS(),
    VERSION(),
    HOST("Host:"),
    CONTENT_LENGTH("Content-Length:");

    private final MutableDirectBuffer buffer;
    private final MutableDirectBuffer upperCaseBuffer;
    private final MutableDirectBuffer lowerCaseBuffer;

    HttpHeaderName()
    {
        lowerCaseBuffer = upperCaseBuffer = buffer = new UnsafeBuffer(new byte[0]);
    }

    HttpHeaderName(final String name)
    {
        final byte[] bytesName = name.getBytes();

        buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(bytesName.length));
        buffer.putBytes(0, bytesName);

        upperCaseBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(bytesName.length));
        upperCaseBuffer.putBytes(0, name.toUpperCase().getBytes());

        lowerCaseBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(bytesName.length));
        lowerCaseBuffer.putBytes(0, name.toLowerCase().getBytes());
    }

    public int length()
    {
        return buffer.capacity();
    }

    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    public MutableDirectBuffer lowerCaseBuffer()
    {
        return lowerCaseBuffer;
    }

    public MutableDirectBuffer upperCaseBuffer()
    {
        return upperCaseBuffer;
    }

    public static HttpHeaderName get(final DirectBuffer buffer, final int offset)
    {
        for (final HttpHeaderName name : Singleton.STANDARD_NAMES)
        {
            if (ProtocolUtil.compareMemory(buffer, offset, name.buffer, 0, name.length()))
            {
                return name;
            }
        }

        // Match headers with case-insensitive match
        for (final HttpHeaderName name : Singleton.STANDARD_NAMES)
        {
            if (ProtocolUtil.compareCaseInsensitiveMemory(
                buffer, offset, name.lowerCaseBuffer, name.upperCaseBuffer, 0, name.length()))
            {
                return name;
            }
        }

        return null;
    }

    /**
     * Hold static versions of the values so they don't get created all the time
     */
    static class Singleton
    {
        public static final List<HttpHeaderName> ALL_NAMES = Arrays.asList(HttpHeaderName.values());

        public static final List<HttpHeaderName> STANDARD_NAMES =
            ALL_NAMES.stream().filter((v) -> v.length() > 0).collect(Collectors.toList());
    }
}
