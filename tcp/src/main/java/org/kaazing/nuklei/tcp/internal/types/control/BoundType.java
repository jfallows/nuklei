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

import org.kaazing.nuklei.tcp.internal.types.Type;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

public abstract class BoundType<T extends DirectBuffer> extends Type<T>
{
    public static final int BOUND_TYPE_ID = 0x40000001;

    protected static final int FIELD_OFFSET_CORRELATION_ID = 0;
    protected static final int FIELD_SIZE_CORRELATION_ID = BitUtil.SIZE_OF_LONG;

    protected static final int FIELD_OFFSET_BINDING_REF = FIELD_OFFSET_CORRELATION_ID + FIELD_SIZE_CORRELATION_ID;
    protected static final int FIELD_SIZE_BINDING_REF = BitUtil.SIZE_OF_LONG;

    public final int typeId()
    {
        return BOUND_TYPE_ID;
    }

    public final long correlationId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_CORRELATION_ID);
    }

    public final long bindingRef()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_BINDING_REF);
    }

    @Override
    public final int limit()
    {
        return offset() + FIELD_OFFSET_BINDING_REF + FIELD_SIZE_BINDING_REF;
    }

    @Override
    public final String toString()
    {
        return String.format("[correlationId=%d, bindingRef=%d]", correlationId(), bindingRef());
    }
}
