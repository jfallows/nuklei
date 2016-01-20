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
package org.kaazing.nuklei.ws.internal.types.stream;

import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_BEGIN;

import java.nio.charset.StandardCharsets;

import org.kaazing.nuklei.ws.internal.types.Flyweight;
import org.kaazing.nuklei.ws.internal.types.StringFW;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class WsBeginFW extends Flyweight
{
    private static final int FIELD_OFFSET_STREAM_ID = 0;
    private static final int FIELD_SIZE_STREAM_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_REFERENCE_ID = FIELD_OFFSET_STREAM_ID + FIELD_SIZE_STREAM_ID;
    private static final int FIELD_SIZE_REFERENCE_ID = BitUtil.SIZE_OF_LONG;

    private static final int FIELD_OFFSET_HEADERS = FIELD_OFFSET_REFERENCE_ID + FIELD_SIZE_REFERENCE_ID;

    private final StringFW protocolRO = new StringFW();

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

    public StringFW protocol()
    {
        return protocolRO;
    }

    @Override
    public int limit()
    {
        return protocolRO.limit();
    }

    public WsBeginFW wrap(DirectBuffer buffer, int offset, int maxLimit)
    {
        super.wrap(buffer, offset, maxLimit);

        this.protocolRO.wrap(buffer, offset + FIELD_OFFSET_HEADERS, maxLimit);

        checkLimit(limit(), maxLimit);

        return this;
    }

    @Override
    public String toString()
    {
        return String.format("[streamId=%d, referenceId=%d, protocol=%s]", streamId(), referenceId(), protocolRO.asString());
    }

    public static final class Builder extends Flyweight.Builder<WsBeginFW>
    {
        private final StringFW.Builder protocolRW = new StringFW.Builder();

        public Builder()
        {
            super(new WsBeginFW());
        }

        @Override
        public Builder wrap(MutableDirectBuffer buffer, int offset, int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);

            limit(offset + FIELD_OFFSET_HEADERS);
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

        public Builder protocol(String protocol)
        {
            if (protocol == null)
            {
                limit(offset() + FIELD_OFFSET_REFERENCE_ID + FIELD_SIZE_REFERENCE_ID);
            }
            else
            {
                protocol().set(protocol, StandardCharsets.UTF_8);
                limit(protocol().build().limit());
            }
            return this;
        }

        protected StringFW.Builder protocol()
        {
            return this.protocolRW.wrap(buffer(), offset() + FIELD_OFFSET_REFERENCE_ID + FIELD_SIZE_REFERENCE_ID, maxLimit());
        }
    }
}
