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
import org.kaazing.nuklei.amqp_1_0.codec.definitions.ReceiverSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.DeliveryState;
import org.kaazing.nuklei.amqp_1_0.codec.types.BinaryType;
import org.kaazing.nuklei.amqp_1_0.codec.types.BooleanType;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UByteType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.MutableDirectBuffer;

/*
 * See AMQP 1.0 specification, section 2.7.5 "Transfer"
 */
public final class Transfer extends CompositeType {

    public static final ThreadLocal<Transfer> LOCAL_REF = new ThreadLocal<Transfer>() {
        @Override
        protected Transfer initialValue() {
            return new Transfer();
        }
    };

    private final UIntType handle;
    private final UIntType deliveryId;
    private final BinaryType deliveryTag;
    private final UIntType messageFormat;
    private final BooleanType settled;
    private final BooleanType more;
    private final UByteType receiveSettleMode;
    private final DeliveryState.Described deliveryState;
    private final BooleanType resume;
    private final BooleanType aborted;
    private final BooleanType batchable;

    // TODO: payload

    public Transfer() {
        handle = new UIntType().watch((owner) -> { limit(1, owner.limit()); });
        deliveryId = new UIntType().watch((owner) -> { limit(2, owner.limit()); });
        deliveryTag = new BinaryType().watch((owner) -> { limit(3, owner.limit()); });
        messageFormat = new UIntType().watch((owner) -> { limit(4, owner.limit()); });;
        settled = new BooleanType().watch((owner) -> { limit(5, owner.limit()); });
        more = new BooleanType().watch((owner) -> { limit(6, owner.limit()); });
        receiveSettleMode = new UByteType().watch((owner) -> { limit(7, owner.limit()); });
        deliveryState = new DeliveryState.Described().watch((owner) -> { limit(8, owner.limit()); });
        resume = new BooleanType().watch((owner) -> { limit(9, owner.limit()); });
        aborted = new BooleanType().watch((owner) -> { limit(10, owner.limit()); });
        batchable = new BooleanType().watch((owner) -> { limit(11, owner.limit()); });
    }

    @Override
    public Transfer watch(Consumer<Flyweight> observer) {
        super.watch(observer);
        return this;
    }

    @Override
    public Transfer wrap(MutableDirectBuffer buffer, int offset) {
        super.wrap(buffer, offset);
        return this;
    }
    
    @Override
    public Transfer maxLength(int value) {
        super.maxLength(value);
        return this;
    }

    @Override
    public Transfer maxCount(int value) {
        super.maxCount(value);
        return this;
    }

    public Transfer setHandle(long value) {
        handle().set(value);
        return this;
    }
    
    public long getHandle() {
        return handle().get();
    }
    
    public Transfer setDeliveryId(long value) {
        deliveryId().set(value);
        return this;
    }
    
    public long getDeliveryId() {
        return deliveryId().get();
    }
    

    public <T> Transfer setDeliveryTag(MutableDirectBufferMutator<T> mutator, T value) {
        deliveryTag().set(mutator, value);
        return this;
    }
    
    public <T> T getDeliveryTag(DirectBufferAccessor<T> accessor) {
        return deliveryTag().get(accessor);
    }

    public Transfer setMessageFormat(long value) {
        messageFormat().set(value);
        return this;
    }
    
    public long getMessageFormat() {
        return messageFormat().get();
    }

    public Transfer setSettled(boolean value) {
        settled().set(value);
        return this;
    }
    
    public boolean getSettled() {
        return settled().get();
    }

    public Transfer setMore(boolean value) {
        more().set(value);
        return this;
    }
    
    public boolean getMore() {
        return more().get();
    }

    public Transfer setReceiveSettleMode(ReceiverSettleMode value) {
        receiveSettleMode().set(ReceiverSettleMode.WRITE, value);
        return this;
    }
    
    public ReceiverSettleMode getReceiveSettleMode() {
        return receiveSettleMode().get(ReceiverSettleMode.READ);
    }

    public DeliveryState.Described getDeliveryState() {
        return deliveryState();
    }
    
    public Transfer setResume(boolean value) {
        resume().set(value);
        return this;
    }
    
    public boolean getResume() {
        return resume().get();
    }

    public Transfer setAborted(boolean value) {
        aborted().set(value);
        return this;
    }
    
    public boolean getAborted() {
        return aborted().get();
    }

    public Transfer setBatchable(boolean value) {
        batchable().set(value);
        return this;
    }
    
    public boolean getBatchable() {
        return batchable().get();
    }

    private UIntType handle() {
        return handle.wrap(buffer(), offsetBody());
    }

    private UIntType deliveryId() {
        return deliveryId.wrap(buffer(), handle().limit());
    }
    
    private BinaryType deliveryTag() {
        return deliveryTag.wrap(buffer(), deliveryId().limit());
    }
        
    private UIntType messageFormat() {
        return messageFormat.wrap(buffer(), deliveryTag().limit());
    }

    private BooleanType settled() {
        return settled.wrap(buffer(), messageFormat().limit());
    }
    
    private BooleanType more() {
        return more.wrap(buffer(), settled().limit());
    }

    private UByteType receiveSettleMode() {
        return receiveSettleMode.wrap(buffer(), more().limit());
    }

    private DeliveryState.Described deliveryState() {
        return deliveryState.wrap(buffer(), receiveSettleMode().limit());
    }

    private BooleanType resume() {
        return resume.wrap(buffer(), deliveryState().limit());
    }

    private BooleanType aborted() {
        return aborted.wrap(buffer(), resume().limit());
    }

    private BooleanType batchable() {
        return batchable.wrap(buffer(), aborted().limit());
    }

}
