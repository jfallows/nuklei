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

package org.kaazing.nuklei.tcp.internal.types.control;

import org.kaazing.nuklei.tcp.internal.types.StringRW;
import org.kaazing.nuklei.tcp.internal.types.StringType;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class BoundRW extends BoundType<MutableDirectBuffer>
{
    private final StringRW destination = new StringRW();

    public BoundRW wrap(MutableDirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset);
        return this;
    }

    public BoundRW correlationId(long correlationId)
    {
        buffer().putLong(offset() + FIELD_OFFSET_CORRELATION_ID, correlationId);
        return this;
    }

    public BoundRW destination(StringType<?> destination)
    {
        this.destination.wrap(buffer(), offset() + FIELD_OFFSET_DESTINATION);
        this.destination.set(destination);
        return this;
    }

    public BoundRW bindingRef(long bindingRef)
    {
        buffer().putLong(destination.limit(), bindingRef);
        return this;
    }

    @Override
    public StringRW destination()
    {
        return destination;
    }

    public int limit()
    {
        return destination.limit() + FIELD_SIZE_BINDING_REF;
    }
}
