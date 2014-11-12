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
import org.kaazing.nuklei.amqp_1_0.codec.types.ListType;
import org.kaazing.nuklei.amqp_1_0.codec.types.StringType;
import org.kaazing.nuklei.amqp_1_0.codec.types.SymbolType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;
import org.kaazing.nuklei.function.AtomicBufferAccessor;
import org.kaazing.nuklei.function.AtomicBufferMutator;

import uk.co.real_logic.agrona.MutableDirectBuffer;

/*
 * See AMQP 1.0 specification, section 3.5.3 "Source"
 */
public final class Source extends ListType {

    private final StringType address;
    private final UIntType durable;
    private final SymbolType expiryPolicy;
    private final UIntType timeout;
    private final BooleanType dynamic;
    private final NodeProperties dynamicNodeProperties;
    private final SymbolType distributionMode;
    private final FilterSet.Embedded<Source> filter;
    private final Outcome.Described defaultOutcome;
    private final ArrayType outcomes;
    private final ArrayType capabilities;

    public Source() {
        address = new StringType().watch((owner) -> { limit(1, owner.limit()); });
        durable = new UIntType().watch((owner) -> { limit(2, owner.limit()); });
        expiryPolicy = new SymbolType().watch((owner) -> { limit(3, owner.limit()); });
        timeout = new UIntType().watch((owner) -> { limit(4, owner.limit()); });
        dynamic = new BooleanType().watch((owner) -> { limit(5, owner.limit()); });
        dynamicNodeProperties = new NodeProperties().watch((owner) -> { limit(6, owner.limit()); });
        distributionMode = new SymbolType().watch((owner) -> { limit(7, owner.limit()); });
        filter = new FilterSet.Embedded<>(this).watch((owner) -> { limit(8, owner.limit()); });
        defaultOutcome = new Outcome.Described().watch((owner) -> { limit(9, owner.limit()); });
        outcomes = new ArrayType().watch((owner) -> { limit(10, owner.limit()); });
        capabilities = new ArrayType().watch((owner) -> { limit(11, owner.limit()); });
    }

    @Override
    public Source watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public Source wrap(MutableDirectBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        return this;
    }
    
    @Override
    public Source maxLength(int value) {
        super.maxLength(value);
        return this;
    }

    @Override
    public Source maxCount(int value) {
        super.maxCount(value);
        return this;
    }

    public Source setAddress(Void value) {
        address().set(value);
        return this;
    }
    
    public <T> Source setAddress(AtomicBufferMutator<T> mutator, T value) {
        address().set(mutator, value);
        return this;
    }
    
    public <T> T getAddress(AtomicBufferAccessor<T> accessor) {
        return address().get(accessor);
    }

    public Source setDurable(TerminusDurability value) {
        durable().set(TerminusDurability.WRITE, value);
        return this;
    }
    
    public TerminusDurability getDurable() {
        return durable().get(TerminusDurability.READ);
    }

    public Source setExpiryPolicy(TerminusExpiryPolicy value) {
        expiryPolicy().set(TerminusExpiryPolicy.WRITE, value);
        return this;
    }
    
    public TerminusExpiryPolicy getExpiryPolicy() {
        return expiryPolicy().get(TerminusExpiryPolicy.READ);
    }
    
    public Source setTimeout(long value) {
        timeout().set(value);
        return this;
    }
    
    public long getTimeout() {
        return timeout().get();
    }
    
    public Source setDynamic(boolean value) {
        dynamic().set(value);
        return this;
    }
    
    public boolean getDynamic() {
        return dynamic().get();
    }

    public NodeProperties getDynamicNodeProperties() {
        return dynamicNodeProperties();
    }

    public Source setDistributionMode(DistributionMode value) {
        distributionMode().set(DistributionMode.WRITE, value);
        return this;
    }
    
    public DistributionMode getDistributionMode() {
        return distributionMode().get(DistributionMode.READ);
    }

    public FilterSet.Embedded<Source> getFilter() {
        return filter();
    }
    
    public Outcome.Described getDefaultOutcome() {
        return defaultOutcome();
    }
    
    public Source setOutcomes(ArrayType value) {
        outcomes().set(value);
        return this;
    }
    
    public ArrayType getOutcomes() {
        return outcomes();
    }

    public Source setCapabilities(ArrayType value) {
        capabilities().set(value);
        return this;
    }
    
    public ArrayType getCapabilities() {
        return capabilities();
    }

    private StringType address() {
        return address.wrap(buffer(), offsetBody());
    }

    private UIntType durable() {
        return durable.wrap(buffer(), address().limit());
    }

    private SymbolType expiryPolicy() {
        return expiryPolicy.wrap(buffer(), durable().limit());
    }
    
    private UIntType timeout() {
        return timeout.wrap(buffer(), expiryPolicy().limit());
    }
    
    private BooleanType dynamic() {
        return dynamic.wrap(buffer(), timeout().limit());
    }
    
    private NodeProperties dynamicNodeProperties() {
        return dynamicNodeProperties.wrap(buffer(), dynamic().limit());
    }
    
    private SymbolType distributionMode() {
        return distributionMode.wrap(buffer(), dynamicNodeProperties().limit());
    }
    
    private FilterSet.Embedded<Source> filter() {
        return filter.wrap(buffer(), distributionMode().limit());
    }
    
    private Outcome.Described defaultOutcome() {
        return defaultOutcome.wrap(buffer(), filter().limit());
    }
    
    private ArrayType outcomes() {
        return outcomes.wrap(buffer(), defaultOutcome().limit());
    }
    
    private ArrayType capabilities() {
        return capabilities.wrap(buffer(), outcomes().limit());
    }
}
