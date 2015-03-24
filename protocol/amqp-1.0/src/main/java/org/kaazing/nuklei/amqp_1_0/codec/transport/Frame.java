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

import static java.lang.String.format;
import static java.nio.ByteOrder.BIG_ENDIAN;

import org.kaazing.nuklei.FlyweightBE;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.Performative;
import org.kaazing.nuklei.amqp_1_0.codec.types.DynamicType;
import org.kaazing.nuklei.amqp_1_0.codec.types.ULongType;

import uk.co.real_logic.agrona.DirectBuffer;

public final class Frame extends FlyweightBE
{

    public static final ThreadLocal<Frame> LOCAL_REF = new ThreadLocal<Frame>()
    {
        @Override
        protected Frame initialValue()
        {
            return new Frame();
        }
    };

    private static final int OFFSET_LENGTH = 0;
    private static final int SIZEOF_LENGTH = 4;

    private static final int OFFSET_DATA_OFFSET = OFFSET_LENGTH + SIZEOF_LENGTH;
    private static final int SIZEOF_DATA_OFFSET = 1;

    private static final int OFFSET_TYPE = OFFSET_DATA_OFFSET + SIZEOF_DATA_OFFSET;
    private static final int SIZEOF_TYPE = 1;

    private static final int OFFSET_CHANNEL = OFFSET_TYPE + SIZEOF_TYPE;
    private static final int SIZEOF_CHANNEL = 2;

    private static final int OFFSET_PERFORMATIVE = OFFSET_CHANNEL + SIZEOF_CHANNEL;

    private final ULongType.Descriptor performative;
    private final DynamicType body;

    public Frame()
    {
        performative = new ULongType.Descriptor();
        body = new DynamicType().watch((owner) -> setLength(owner.limit() - offset()));
    }

    @Override
    public Frame wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public int bodyOffset()
    {
        return body().offset();
    }

    public void bodyChanged()
    {
        body().notifyChanged();
    }

    public Frame setLength(long value)
    {
        uint32Put(mutableBuffer(), offset() + OFFSET_LENGTH, value, BIG_ENDIAN);
        return this;
    }

    public long getLength()
    {
        return uint32Get(mutableBuffer(), offset() + OFFSET_LENGTH, BIG_ENDIAN);
    }

    public Frame setDataOffset(int value)
    {
        uint8Put(mutableBuffer(), offset() + OFFSET_DATA_OFFSET, (short) value);
        return this;
    }

    public int getDataOffset()
    {
        return uint8Get(buffer(), offset() + OFFSET_DATA_OFFSET);
    }

    public Frame setType(int value)
    {
        uint8Put(mutableBuffer(), offset() + OFFSET_TYPE, (short) value);
        return this;
    }

    public int getType()
    {
        return uint8Get(buffer(), offset() + OFFSET_TYPE);
    }

    public Frame setChannel(int value)
    {
        uint16Put(mutableBuffer(), offset() + OFFSET_CHANNEL, value);
        return this;
    }

    public int getChannel()
    {
        return uint16Get(buffer(), offset() + OFFSET_CHANNEL);
    }

    public Frame setPerformative(Performative value)
    {
        performative().set(Performative.WRITE, value);
        return this;
    }

    public Performative getPerformative()
    {
        return performative().get(Performative.READ);
    }

    public int limit()
    {
        long frameLength = uint32Get(buffer(), offset() + OFFSET_LENGTH);
        if ((frameLength & 0x80000000) != 0)
        {
            throw new RuntimeException(format("Unsuported frame length: %d", frameLength));
        }

        return (offset() + (int)frameLength);
    }

    private ULongType.Descriptor performative()
    {
        return performative.wrap(mutableBuffer(), offset() + OFFSET_PERFORMATIVE, true);
    }

    private DynamicType body()
    {
        return body.wrap(mutableBuffer(), performative().limit(), true);
    }
}
