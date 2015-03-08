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

import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Fields;
import org.kaazing.nuklei.amqp_1_0.codec.types.ArrayType;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UShortType;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 2.7.2 "Begin"
 */
public final class Begin extends CompositeType
{

    public static final ThreadLocal<Begin> LOCAL_REF = new ThreadLocal<Begin>()
    {
        @Override
        protected Begin initialValue()
        {
            return new Begin();
        }
    };

    public static final long DEFAULT_HANDLE_MAX = 4294967295L;

    private final UShortType remoteChannel;
    private final UIntType nextOutgoingId;
    private final UIntType incomingWindow;
    private final UIntType outgoingWindow;
    private final UIntType handleMax;
    private final ArrayType offeredCapabilities;
    private final ArrayType desiredCapabilities;
    private final Fields properties;

    public Begin()
    {
        remoteChannel = new UShortType().watch((owner) ->
        {
            limit(1, owner.limit());
        });
        nextOutgoingId = new UIntType().watch((owner) ->
        {
            limit(2, owner.limit());
        });

        incomingWindow = new UIntType().watch((owner) ->
        {
            limit(3, owner.limit());
        });

        outgoingWindow = new UIntType().watch((owner) ->
        {
            limit(4, owner.limit());
        });

        handleMax = new UIntType().watch((owner) ->
        {
            limit(5, owner.limit());
        });

        offeredCapabilities = new ArrayType().watch((owner) ->
        {
            limit(6, owner.limit());
        });

        desiredCapabilities = new ArrayType().watch((owner) ->
        {
            limit(7, owner.limit());
        });

        properties = new Fields().watch((owner) ->
        {
            limit(8, owner.limit());
        });
    }

    @Override
    public Begin watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Begin wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Begin maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Begin maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public Begin setRemoteChannel(int value)
    {
        remoteChannel().set(value);
        return this;
    }

    public long getRemoteChannel()
    {
        return remoteChannel().get();
    }

    public Begin setNextOutgoingId(long value)
    {
        nextOutgoingId().set(value);
        return this;
    }

    public long getNextOutgoingId()
    {
        return nextOutgoingId().get();
    }

    public Begin setIncomingWindow(long value)
    {
        incomingWindow().set(value);
        return this;
    }

    public long getIncomingWindow()
    {
        return incomingWindow().get();
    }

    public Begin setOutgoingWindow(long value)
    {
        outgoingWindow().set(value);
        return this;
    }

    public long getOutgoingWindow()
    {
        return outgoingWindow().get();
    }

    public Begin setHandleMax(long value)
    {
        handleMax().set(value);
        return this;
    }

    public long getHandleMax()
    {
        return handleMax().get();
    }

    public Begin setOfferedCapabilities(ArrayType value)
    {
        offeredCapabilities().set(value);
        return this;
    }

    public ArrayType getOfferedCapabilities()
    {
        return offeredCapabilities();
    }

    public Begin setDesiredCapabilities(ArrayType value)
    {
        desiredCapabilities().set(value);
        return this;
    }

    public ArrayType getDesiredCapabilities()
    {
        return desiredCapabilities();
    }

    public Fields getProperties()
    {
        return properties();
    }

    private UShortType remoteChannel()
    {
        return remoteChannel.wrap(mutableBuffer(), offsetBody(), true);
    }

    private UIntType nextOutgoingId()
    {
        return nextOutgoingId.wrap(mutableBuffer(), remoteChannel().limit(), true);
    }

    private UIntType incomingWindow()
    {
        return incomingWindow.wrap(mutableBuffer(), nextOutgoingId().limit(), true);
    }

    private UIntType outgoingWindow()
    {
        return outgoingWindow.wrap(mutableBuffer(), incomingWindow().limit(), true);
    }

    private UIntType handleMax()
    {
        return handleMax.wrap(mutableBuffer(), outgoingWindow().limit(), true);
    }

    private ArrayType offeredCapabilities()
    {
        return offeredCapabilities.wrap(mutableBuffer(), handleMax().limit(), true);
    }

    private ArrayType desiredCapabilities()
    {
        return desiredCapabilities.wrap(mutableBuffer(), offeredCapabilities().limit(), true);
    }

    private Fields properties()
    {
        return properties.wrap(mutableBuffer(), desiredCapabilities().limit(), true);
    }
}
