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
import org.kaazing.nuklei.amqp_1_0.codec.types.BooleanType;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;
import org.kaazing.nuklei.concurrent.AtomicBuffer;

/*
 * See AMQP 1.0 specification, section 2.7.4 "Flow"
 */
public final class Flow extends CompositeType {

    private final UIntType nextIncomingId;
    private final UIntType incomingWindow;
    private final UIntType nextOutgoingId;
    private final UIntType outgoingWindow;
    private final UIntType handle;
    private final UIntType deliveryCount;
    private final UIntType linkCredit;
    private final UIntType available;
    private final BooleanType drain;
    private final BooleanType echo;
    private final Fields properties;

    public Flow() {
        nextIncomingId = new UIntType().watch((owner) -> { limit(1, owner.limit()); });;
        incomingWindow = new UIntType().watch((owner) -> { limit(2, owner.limit()); });;
        nextOutgoingId = new UIntType().watch((owner) -> { limit(3, owner.limit()); });;
        outgoingWindow = new UIntType().watch((owner) -> { limit(4, owner.limit()); });;
        handle = new UIntType().watch((owner) -> { limit(5, owner.limit()); });;
        deliveryCount = new UIntType().watch((owner) -> { limit(6, owner.limit()); });
        linkCredit = new UIntType().watch((owner) -> { limit(7, owner.limit()); });
        available = new UIntType().watch((owner) -> { limit(8, owner.limit()); });
        drain = new BooleanType().watch((owner) -> { limit(9, owner.limit()); });
        echo = new BooleanType().watch((owner) -> { limit(10, owner.limit()); });
        properties = new Fields().watch((owner) -> { limit(11, owner.limit()); });
    }

    @Override
    public Flow watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public Flow wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        return this;
    }
    
    @Override
    public Flow maxLength(int value) {
        super.maxLength(value);
        return this;
    }

    @Override
    public Flow maxCount(int value) {
        super.maxCount(value);
        return this;
    }

    public Flow setNextOutgoingId(long value) {
        nextOutgoingId().set(value);
        return this;
    }
    
    public long getNextOutgoingId() {
        return nextOutgoingId().get();
    }

    public Flow setIncomingWindow(long value) {
        incomingWindow().set(value);
        return this;
    }
    
    public long getIncomingWindow() {
        return incomingWindow().get();
    }

    public Flow setOutgoingWindow(long value) {
        outgoingWindow().set(value);
        return this;
    }
    
    public long getOutgoingWindow() {
        return outgoingWindow().get();
    }


    public Flow setHandle(long value) {
        handle().set(value);
        return this;
    }
    
    public long getHandle() {
        return handle().get();
    }
    
    public Flow setDeliveryCount(long value) {
        deliveryCount().set(value);
        return this;
    }
    
    public long getDeliveryCount() {
        return deliveryCount().get();
    }
    
    public Flow setLinkCredit(long value) {
        linkCredit().set(value);
        return this;
    }
    
    public long getLinkCredit() {
        return linkCredit().get();
    }
    
    public Flow setAvailable(long value) {
        available().set(value);
        return this;
    }
    
    public long getAvailable() {
        return available().get();
    }
    
    public Flow setDrain(boolean value) {
        drain().set(value);
        return this;
    }
    
    public boolean getDrain() {
        return drain().get();
    }

    public Flow setEcho(boolean value) {
        echo().set(value);
        return this;
    }
    
    public boolean getEcho() {
        return echo().get();
    }

    public Fields getProperties() {
        return properties();
    }

    private UIntType nextIncomingId() {
        return nextIncomingId.wrap(buffer(), offsetBody());
    }
    
    private UIntType incomingWindow() {
        return incomingWindow.wrap(buffer(), nextIncomingId().limit());
    }
    
    private UIntType nextOutgoingId() {
        return nextOutgoingId.wrap(buffer(), incomingWindow().limit());
    }
    
    private UIntType outgoingWindow() {
        return outgoingWindow.wrap(buffer(), nextOutgoingId().limit());
    }

    private UIntType handle() {
        return handle.wrap(buffer(), outgoingWindow().limit());
    }
    
    private UIntType deliveryCount() {
        return deliveryCount.wrap(buffer(), handle().limit());
    }

    private UIntType linkCredit() {
        return linkCredit.wrap(buffer(), deliveryCount().limit());
    }

    private UIntType available() {
        return available.wrap(buffer(), linkCredit().limit());
    }

    private BooleanType drain() {
        return drain.wrap(buffer(), available().limit());
    }
    
    private BooleanType echo() {
        return echo.wrap(buffer(), drain().limit());
    }
    
    private Fields properties() {
        return properties.wrap(buffer(), echo().limit());
    }
}
