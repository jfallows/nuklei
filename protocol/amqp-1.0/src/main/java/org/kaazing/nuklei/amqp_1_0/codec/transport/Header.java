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
package org.kaazing.nuklei.amqp_1_0.codec.transport;

import org.kaazing.nuklei.FlyweightBE;

import uk.co.real_logic.agrona.DirectBuffer;

public final class Header extends FlyweightBE
{

    public static final ThreadLocal<Header> LOCAL_REF = new ThreadLocal<Header>()
    {
        @Override
        protected Header initialValue()
        {
            return new Header();
        }
    };

    // ASCII "AMQP"
    public static final int AMQP_PROTOCOL = 0x41 << 24 | 0x4d << 16 | 0x51 << 8 | 0x50;

    private static final int OFFSET_PROTOCOL = 0;
    private static final int SIZEOF_PROTOCOL = 4;

    private static final int OFFSET_PROTOCOL_ID = OFFSET_PROTOCOL + SIZEOF_PROTOCOL;
    private static final int SIZEOF_PROTOCOL_ID = 1;

    private static final int OFFSET_MAJOR_VERSION = OFFSET_PROTOCOL_ID + SIZEOF_PROTOCOL_ID;
    private static final int SIZEOF_MAJOR_VERSION = 1;

    private static final int OFFSET_MINOR_VERSION = OFFSET_MAJOR_VERSION + SIZEOF_MAJOR_VERSION;
    private static final int SIZEOF_MINOR_VERSION = 1;

    private static final int OFFSET_REVISION_VERSION = OFFSET_MINOR_VERSION + SIZEOF_MINOR_VERSION;
    private static final int SIZEOF_REVISION_VERSION = 1;

    public static final int SIZEOF_HEADER = SIZEOF_PROTOCOL + SIZEOF_PROTOCOL_ID + SIZEOF_MAJOR_VERSION +
            SIZEOF_MINOR_VERSION + SIZEOF_REVISION_VERSION;

    // unit tests
    Header()
    {
    }

    @Override
    public Header wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public Header setProtocol(int value)
    {
        int32Put(mutableBuffer(), offset() + OFFSET_PROTOCOL, value);
        return this;
    }

    public int getProtocol()
    {
        return int32Get(buffer(), offset() + OFFSET_PROTOCOL);
    }

    public Header setProtocolID(int value)
    {
        int8Put(mutableBuffer(), offset() + OFFSET_PROTOCOL_ID, (byte) value);
        return this;
    }

    public int getProtocolID()
    {
        return int8Get(buffer(), offset() + OFFSET_PROTOCOL_ID);
    }

    public Header setMajorVersion(int value)
    {
        int8Put(mutableBuffer(), offset() + OFFSET_MAJOR_VERSION, (byte) value);
        return this;
    }

    public int getMajorVersion()
    {
        return int8Get(buffer(), offset() + OFFSET_MAJOR_VERSION);
    }

    public Header setMinorVersion(int value)
    {
        int8Put(mutableBuffer(), offset() + OFFSET_MINOR_VERSION, (byte) value);
        return this;
    }

    public int getMinorVersion()
    {
        return int8Get(buffer(), offset() + OFFSET_MINOR_VERSION);
    }

    public Header setRevisionVersion(int value)
    {
        int8Put(mutableBuffer(), offset() + OFFSET_REVISION_VERSION, (byte) value);
        return this;
    }

    public int getRevisionVersion()
    {
        return int8Get(buffer(), offset() + OFFSET_REVISION_VERSION);
    }

    public int limit()
    {
        return offset() + OFFSET_REVISION_VERSION + SIZEOF_REVISION_VERSION;
    }
}
