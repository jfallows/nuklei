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
package org.kaazing.nuklei.echo.internal.types.stream;

import static org.kaazing.nuklei.echo.internal.types.stream.Types.TYPE_ID_DATA;

import org.kaazing.nuklei.echo.internal.types.Flyweight;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class DataFW extends Flyweight
{
    private static final int FIELD_OFFSET_STREAM_ID = 0;
    private static final int FIELD_SIZE_STREAM_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_PAYLOAD = FIELD_OFFSET_STREAM_ID + FIELD_SIZE_STREAM_ID;

    private int limit;

    @Override
    public int limit()
    {
        return limit;
    }

    public DataFW wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.limit = actingLimit;

        checkLimit(limit(), actingLimit);

        return this;
    }

    public int typeId()
    {
        return TYPE_ID_DATA;
    }

    public long streamId()
    {
        return buffer().getLong(offset() + FIELD_OFFSET_STREAM_ID);
    }

    public int payloadOffset()
    {
        return offset() + FIELD_OFFSET_PAYLOAD;
    }

    public int payloadLength()
    {
        return limit() - payloadOffset();
    }

    @Override
    public String toString()
    {
        return String.format("[streamId=%d,remaining=%d]", streamId(), length());
    }

    public static final class Builder extends Flyweight.Builder<DataFW>
    {
        public Builder()
        {
            super(new DataFW());
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

        public int payloadOffset()
        {
            return offset() + FIELD_OFFSET_PAYLOAD;
        }

        public Builder payload(DirectBuffer buffer, int offset, int length)
        {
            buffer().putBytes(payloadOffset(), buffer, offset, length);
            payloadLength(length);
            return this;
        }

        public Builder payloadLength(int payloadLength)
        {
            assert (payloadLength >= 0);

            maxLimit(offset() + FIELD_OFFSET_PAYLOAD + payloadLength);
            return this;
        }
    }
}
