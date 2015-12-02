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
package org.kaazing.nuklei.echo.internal.types.control;

import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_UNBOUND_RESPONSE;

import org.kaazing.nuklei.echo.internal.types.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class UnboundFW extends Flyweight
{
    private static final int FIELD_OFFSET_CORRELATION_ID = 0;
    private static final int FIELD_SIZE_CORRELATION_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_BINDING = FIELD_OFFSET_CORRELATION_ID + FIELD_SIZE_CORRELATION_ID;

    private final BindingFW bindingRO = new BindingFW();

    @Override
    public UnboundFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.bindingRO.wrap(buffer, offset + FIELD_OFFSET_BINDING, actingLimit);

        checkLimit(limit(), actingLimit);

        return this;
    }

    @Override
    public int limit()
    {
        return binding().limit();
    }

    public int typeId()
    {
        return TYPE_ID_UNBOUND_RESPONSE;
    }

    public long correlationId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_CORRELATION_ID);
    }

    public BindingFW binding()
    {
        return bindingRO;
    }

    @Override
    public String toString()
    {
        return String.format("[correlationId=%d, bindingRO=%s]", correlationId(), binding());
    }

    public static final class Builder extends Flyweight.Builder<UnboundFW>
    {
        private final BindingFW.Builder bindingRW = new BindingFW.Builder();

        public Builder()
        {
            super(new UnboundFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);

            this.bindingRW.wrap(buffer, offset + FIELD_OFFSET_BINDING, maxLimit);

            return this;
        }

        public Builder correlationId(long correlationId)
        {
            buffer().putLong(offset() + FIELD_OFFSET_CORRELATION_ID, correlationId);
            return this;
        }

        public BindingFW.Builder binding()
        {
            return bindingRW;
        }
    }
}
