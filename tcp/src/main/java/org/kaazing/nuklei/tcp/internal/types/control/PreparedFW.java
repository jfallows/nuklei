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
package org.kaazing.nuklei.tcp.internal.types.control;

import static org.kaazing.nuklei.tcp.internal.types.control.Types.TYPE_ID_PREPARED_RESPONSE;

import org.kaazing.nuklei.tcp.internal.types.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class PreparedFW extends Flyweight
{
    private static final int FIELD_OFFSET_CORRELATION_ID = 0;
    private static final int FIELD_SIZE_CORRELATION_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_REFERENCE_ID = FIELD_OFFSET_CORRELATION_ID + FIELD_SIZE_CORRELATION_ID;
    private static final int FIELD_SIZE_REFERENCE_ID = BitUtil.SIZE_OF_LONG;

    public PreparedFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        checkLimit(limit(), actingLimit);

        return this;
    }

    public int typeId()
    {
        return TYPE_ID_PREPARED_RESPONSE;
    }

    public long correlationId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_CORRELATION_ID);
    }

    public long referenceId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_REFERENCE_ID);
    }

    public int limit()
    {
        return offset() + FIELD_OFFSET_REFERENCE_ID + FIELD_SIZE_REFERENCE_ID;
    }

    @Override
    public String toString()
    {
        return String.format("[correlationId=%d, referenceId=%d]", correlationId(), referenceId());
    }

    public static final class Builder extends Flyweight.Builder<PreparedFW>
    {
        public Builder()
        {
            super(new PreparedFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);

            return this;
        }

        public Builder correlationId(long correlationId)
        {
            buffer().putLong(offset() + FIELD_OFFSET_CORRELATION_ID, correlationId);
            return this;
        }

        public Builder referenceId(long referenceId)
        {
            buffer().putLong(offset() + FIELD_OFFSET_REFERENCE_ID, referenceId);
            return this;
        }
    }
}
