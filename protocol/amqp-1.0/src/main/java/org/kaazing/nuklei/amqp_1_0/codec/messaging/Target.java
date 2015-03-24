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
package org.kaazing.nuklei.amqp_1_0.codec.messaging;

import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.amqp_1_0.codec.types.ArrayType;
import org.kaazing.nuklei.amqp_1_0.codec.types.BooleanType;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;
import org.kaazing.nuklei.amqp_1_0.codec.types.StringType;
import org.kaazing.nuklei.amqp_1_0.codec.types.SymbolType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 3.5.3 "Source"
 */
public final class Target extends CompositeType.Described
{

    private final StringType address;
    private final UIntType durable;
    private final SymbolType expiryPolicy;
    private final UIntType timeout;
    private final BooleanType dynamic;
    private final NodeProperties dynamicNodeProperties;
    private final SymbolType distributionMode;
    private final FilterSet.Embedded<Target> filter;
    private final Outcome.Described defaultOutcome;
    private final ArrayType outcomes;
    private final ArrayType capabilities;

    public Target()
    {
        address = new StringType().watch((owner) ->
        {
            limit(1, owner.limit());
        });
        durable = new UIntType().watch((owner) ->
        {
            limit(2, owner.limit());
        });
        expiryPolicy = new SymbolType().watch((owner) ->
        {
            limit(3, owner.limit());
        });
        timeout = new UIntType().watch((owner) ->
        {
            limit(4, owner.limit());
        });
        dynamic = new BooleanType().watch((owner) ->
        {
            limit(5, owner.limit());
        });
        dynamicNodeProperties = new NodeProperties().watch((owner) ->
        {
            limit(6, owner.limit());
        });
        distributionMode = new SymbolType().watch((owner) ->
        {
            limit(7, owner.limit());
        });
        filter = new FilterSet.Embedded<>(this).watch((owner) ->
        {
            limit(8, owner.limit());
        });
        defaultOutcome = new Outcome.Described().watch((owner) ->
        {
            limit(9, owner.limit());
        });
        outcomes = new ArrayType().watch((owner) ->
        {
            limit(10, owner.limit());
        });
        capabilities = new ArrayType().watch((owner) ->
        {
            limit(11, owner.limit());
        });
    }

    public Target setDescriptor()
    {
        super.setDescriptor(0x29L);
        return this;
    }

    @Override
    public Target watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Target wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Target maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Target maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public Target setAddress(Void value)
    {
        address().set(value);
        return this;
    }

    public <T> Target setAddress(MutableDirectBufferMutator<T> mutator, T value)
    {
        address().set(mutator, value);
        return this;
    }

    public <T> T getAddress(DirectBufferAccessor<T> accessor)
    {
        return address().get(accessor);
    }

    public Target setDurable(TerminusDurability value)
    {
        durable().set(TerminusDurability.WRITE, value);
        return this;
    }

    public TerminusDurability getDurable()
    {
        return durable().get(TerminusDurability.READ);
    }

    public Target setExpiryPolicy(TerminusExpiryPolicy value)
    {
        expiryPolicy().set(TerminusExpiryPolicy.WRITE, value);
        return this;
    }

    public TerminusExpiryPolicy getExpiryPolicy()
    {
        return expiryPolicy().get(TerminusExpiryPolicy.READ);
    }

    public Target setTimeout(long value)
    {
        timeout().set(value);
        return this;
    }

    public long getTimeout()
    {
        return timeout().get();
    }

    public Target setDynamic(boolean value)
    {
        dynamic().set(value);
        return this;
    }

    public boolean getDynamic()
    {
        return dynamic().get();
    }

    public NodeProperties getDynamicNodeProperties()
    {
        return dynamicNodeProperties();
    }

    public Target setDistributionMode(DistributionMode value)
    {
        distributionMode().set(DistributionMode.WRITE, value);
        return this;
    }

    public DistributionMode getDistributionMode()
    {
        return distributionMode().get(DistributionMode.READ);
    }

    public FilterSet.Embedded<Target> setFilter()
    {
        return filter();
    }

    public Outcome.Described getDefaultOutcome()
    {
        return defaultOutcome();
    }

    public Target setOutcomes(ArrayType value)
    {
        outcomes().set(value);
        return this;
    }

    public ArrayType getOutcomes()
    {
        return outcomes();
    }

    public Target setCapabilities(ArrayType value)
    {
        capabilities().set(value);
        return this;
    }

    public ArrayType getCapabilities()
    {
        return capabilities();
    }

    private StringType address()
    {
        return address.wrap(mutableBuffer(), offsetBody(), true);
    }

    private UIntType durable()
    {
        return durable.wrap(mutableBuffer(), address().limit(), true);
    }

    private SymbolType expiryPolicy()
    {
        return expiryPolicy.wrap(mutableBuffer(), durable().limit(), true);
    }

    private UIntType timeout()
    {
        return timeout.wrap(mutableBuffer(), expiryPolicy().limit(), true);
    }

    private BooleanType dynamic()
    {
        return dynamic.wrap(mutableBuffer(), timeout().limit(), true);
    }

    private NodeProperties dynamicNodeProperties()
    {
        return dynamicNodeProperties.wrap(mutableBuffer(), dynamic().limit(), true);
    }

    private SymbolType distributionMode()
    {
        return distributionMode.wrap(mutableBuffer(), dynamicNodeProperties().limit(), true);
    }

    private FilterSet.Embedded<Target> filter()
    {
        return filter.wrap(mutableBuffer(), distributionMode().limit(), true);
    }

    private Outcome.Described defaultOutcome()
    {
        return defaultOutcome.wrap(mutableBuffer(), filter().limit(), true);
    }

    private ArrayType outcomes()
    {
        return outcomes.wrap(mutableBuffer(), defaultOutcome().limit(), true);
    }

    private ArrayType capabilities()
    {
        return capabilities.wrap(mutableBuffer(), outcomes().limit(), true);
    }
}
