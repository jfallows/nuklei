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
import org.kaazing.nuklei.amqp_1_0.codec.types.StringType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UShortType;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 2.7.1 "Open"
 */
public final class Open extends CompositeType
{

    public static final ThreadLocal<Open> LOCAL_REF = new ThreadLocal<Open>()
    {
        @Override
        protected Open initialValue()
        {
            return new Open();
        }
    };

    public static final long DEFAULT_MAX_FRAME_SIZE = 4294967295L;

    private final StringType containerId;
    private final StringType hostname;
    private final UIntType maxFrameSize;
    private final UShortType channelMax;
    private final UIntType idleTimeout;
    private final ArrayType outgoingLocales;
    private final ArrayType incomingLocales;
    private final ArrayType offeredCapabilities;
    private final ArrayType desiredCapabilities;
    private final Fields properties;

    public Open()
    {
        containerId = new StringType().watch((owner) ->
        {
            limit(1, owner.limit());
        });
        hostname = new StringType().watch((owner) ->
        {
            limit(2, owner.limit());
        });
        maxFrameSize = new UIntType().watch((owner) ->
        {
            limit(3, owner.limit());
        });
        channelMax = new UShortType().watch((owner) ->
        {
            limit(4, owner.limit());
        });
        idleTimeout = new UIntType().watch((owner) ->
        {
            limit(5, owner.limit());
        });
        outgoingLocales = new ArrayType().watch((owner) ->
        {
            limit(6, owner.limit());
        });
        incomingLocales = new ArrayType().watch((owner) ->
        {
            limit(7, owner.limit());
        });
        offeredCapabilities = new ArrayType().watch((owner) ->
        {
            limit(8, owner.limit());
        });
        desiredCapabilities = new ArrayType().watch((owner) ->
        {
            limit(9, owner.limit());
        });
        properties = new Fields().watch((owner) ->
        {
            limit(10, owner.limit());
        });
    }

    @Override
    public Open watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Open wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Open maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Open maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public Open setContainerId(Void value)
    {
        containerId().set(value);
        return this;
    }

    public <T> Open setContainerId(MutableDirectBufferMutator<T> mutator, T value)
    {
        containerId().set(mutator, value);
        return this;
    }

    public <T> T getContainerId(DirectBufferAccessor<T> accessor)
    {
        return containerId().get(accessor);
    }

    public Open setHostname(Void value)
    {
        hostname().set(value);
        return this;
    }

    public <T> Open setHostname(MutableDirectBufferMutator<T> mutator, T value)
    {
        hostname().set(mutator, value);
        return this;
    }

    public <T> T getHostname(DirectBufferAccessor<T> accessor)
    {
        return hostname().get(accessor);
    }

    public Open setMaxFrameSize(long value)
    {
        maxFrameSize().set(value);
        return this;
    }

    public long getMaxFrameSize()
    {
        return maxFrameSize().get();
    }

    public Open setChannelMax(int value)
    {
        channelMax().set(value);
        return this;
    }

    public int getChannelMax()
    {
        return channelMax().get();
    }

    public Open setIdleTimeout(long value)
    {
        idleTimeout().set(value);
        return this;
    }

    public long getIdleTimeout()
    {
        return idleTimeout().get();
    }

    public Open setOutgoingLocales(ArrayType value)
    {
        outgoingLocales().set(value);
        return this;
    }

    public ArrayType getOutgoingLocales()
    {
        return outgoingLocales();
    }

    public Open setIncomingLocales(ArrayType value)
    {
        incomingLocales().set(value);
        return this;
    }

    public ArrayType getIncomingLocales()
    {
        return incomingLocales();
    }

    public Open setOfferedCapabilities(ArrayType value)
    {
        offeredCapabilities().set(value);
        return this;
    }

    public ArrayType getOfferedCapabilities()
    {
        return offeredCapabilities();
    }

    public Open setDesiredCapabilities(ArrayType value)
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

    private StringType containerId()
    {
        return containerId.wrap(mutableBuffer(), offsetBody(), true);
    }

    private StringType hostname()
    {
        return hostname.wrap(mutableBuffer(), containerId().limit(), true);
    }

    private UIntType maxFrameSize()
    {
        return maxFrameSize.wrap(mutableBuffer(), hostname().limit(), true);
    }

    private UShortType channelMax()
    {
        return channelMax.wrap(mutableBuffer(), maxFrameSize().limit(), true);
    }

    private UIntType idleTimeout()
    {
        return idleTimeout.wrap(mutableBuffer(), channelMax().limit(), true);
    }

    private ArrayType outgoingLocales()
    {
        return outgoingLocales.wrap(mutableBuffer(), idleTimeout().limit(), true);
    }

    private ArrayType incomingLocales()
    {
        return incomingLocales.wrap(mutableBuffer(), outgoingLocales().limit(), true);
    }

    private ArrayType offeredCapabilities()
    {
        return offeredCapabilities.wrap(mutableBuffer(), incomingLocales().limit(), true);
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
