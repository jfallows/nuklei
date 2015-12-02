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
package org.kaazing.nuklei.tcp.internal.types.stream;

import static org.kaazing.nuklei.tcp.internal.types.stream.Types.TYPE_ID_BEGIN;

import org.kaazing.nuklei.tcp.internal.types.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class BeginFW extends Flyweight
{
    private static final int FIELD_OFFSET_STREAM_ID = 0;
    private static final int FIELD_SIZE_STREAM_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_REFERENCE_ID = FIELD_OFFSET_STREAM_ID + FIELD_SIZE_STREAM_ID;
    private static final int FIELD_SIZE_REFERENCE_ID = BitUtil.SIZE_OF_LONG;

    public int typeId()
    {
        return TYPE_ID_BEGIN;
    }

    public long streamId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_STREAM_ID);
    }

    public long referenceId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_REFERENCE_ID);
    }

    @Override
    public int limit()
    {
        return offset() + FIELD_OFFSET_REFERENCE_ID + FIELD_SIZE_REFERENCE_ID;
    }

    public BeginFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        checkLimit(limit(), actingLimit);

        return this;
    }

    @Override
    public String toString()
    {
        return String.format("[streamId=%d, referenceId=%d]", streamId(), referenceId());
    }

    public static final class Builder extends Flyweight.Builder<BeginFW>
    {
        public Builder()
        {
            super(new BeginFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            return this;
        }

        public Builder streamId(long streamId)
        {
            buffer().putLong(offset() + FIELD_OFFSET_STREAM_ID, streamId);
            return this;
        }

        public Builder referenceId(long referenceId)
        {
            buffer().putLong(offset() + FIELD_OFFSET_REFERENCE_ID, referenceId);
            return this;
        }
    }
}
