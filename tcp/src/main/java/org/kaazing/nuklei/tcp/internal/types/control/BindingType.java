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
package org.kaazing.nuklei.tcp.internal.types.control;

import static java.nio.ByteOrder.BIG_ENDIAN;

import org.kaazing.nuklei.tcp.internal.types.StringType;
import org.kaazing.nuklei.tcp.internal.types.Type;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

public abstract class BindingType<T extends DirectBuffer> extends Type<T>
{
    protected static final int FIELD_OFFSET_SOURCE = 0;

    protected static final int FIELD_SIZE_SOURCE_BINDING_REF = BitUtil.SIZE_OF_LONG;

    protected static final int FIELD_SIZE_PORT = BitUtil.SIZE_OF_SHORT;

    public abstract StringType<T> source();

    public final long sourceBindingRef()
    {
        return buffer().getLong(offset() + source().limit());
    }

    public abstract StringType<T> destination();

    public abstract AddressType<T> address();

    public final int port()
    {
        return buffer().getShort(address().limit(), BIG_ENDIAN) & 0xFFFF;
    }

    public final int limit()
    {
        return address().limit() + FIELD_SIZE_PORT;
    }

    @Override
    public String toString()
    {
        return String.format("[source=%s, destination=%s, address=%s, port=%d]", source(), destination(), address(), port());
    }
}
