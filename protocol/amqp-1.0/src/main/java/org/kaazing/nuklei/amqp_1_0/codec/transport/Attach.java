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
import org.kaazing.nuklei.amqp_1_0.codec.definitions.ReceiverSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Role;
import org.kaazing.nuklei.amqp_1_0.codec.definitions.SenderSettleMode;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.Source;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.Target;
import org.kaazing.nuklei.amqp_1_0.codec.types.ArrayType;
import org.kaazing.nuklei.amqp_1_0.codec.types.BooleanType;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;
import org.kaazing.nuklei.amqp_1_0.codec.types.MapType;
import org.kaazing.nuklei.amqp_1_0.codec.types.StringType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UByteType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;
import org.kaazing.nuklei.amqp_1_0.codec.types.ULongType;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 2.7.3 "Attach"
 */
public final class Attach extends CompositeType
{

    public static final ThreadLocal<Attach> LOCAL_REF = new ThreadLocal<Attach>()
    {
        @Override
        protected Attach initialValue()
        {
            return new Attach();
        }
    };

    public static final long DEFAULT_HANDLE_MAX = 4294967295L;

    private final StringType name;
    private final UIntType handle;
    private final BooleanType role;
    private final UByteType sendSettleMode;
    private final UByteType receiveSettleMode;
    private final Source source;
    private final Target target;
    private final MapType unsettled;
    private final BooleanType incompleteUnsettled;
    private final UIntType initialDeliveryCount;
    private final ULongType maxMessageSize;
    private final ArrayType offeredCapabilities;
    private final ArrayType desiredCapabilities;
    private final Fields properties;

    public Attach()
    {
        name = new StringType().watch((owner) ->
        {
            limit(1, owner.limit());
        });
        handle = new UIntType().watch((owner) ->
        {
            limit(2, owner.limit());
        });
        role = new BooleanType().watch((owner) ->
        {
            limit(3, owner.limit());
        });
        sendSettleMode = new UByteType().watch((owner) ->
        {
            limit(4, owner.limit());
        });
        receiveSettleMode = new UByteType().watch((owner) ->
        {
            limit(5, owner.limit());
        });
        source = new Source().watch((owner) ->
        {
            limit(6, owner.limit());
        });
        target = new Target().watch((owner) ->
        {
            limit(7, owner.limit());
        });
        unsettled = new MapType().watch((owner) ->
        {
            limit(8, owner.limit());
        });
        incompleteUnsettled = new BooleanType().watch((owner) ->
        {
            limit(9, owner.limit());
        });
        initialDeliveryCount = new UIntType().watch((owner) ->
        {
            limit(10, owner.limit());
        });
        maxMessageSize = new ULongType().watch((owner) ->
        {
            limit(11, owner.limit());
        });
        offeredCapabilities = new ArrayType().watch((owner) ->
        {
            limit(12, owner.limit());
        });
        desiredCapabilities = new ArrayType().watch((owner) ->
        {
            limit(13, owner.limit());
        });
        properties = new Fields().watch((owner) ->
        {
            limit(14, owner.limit());
        });
    }

    @Override
    public Attach watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Attach wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Attach maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Attach maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public Attach setName(Void value)
    {
        name().set(value);
        return this;
    }

    public <T> Attach setName(MutableDirectBufferMutator<T> mutator, T value)
    {
        name().set(mutator, value);
        return this;
    }

    public <T> T getName(DirectBufferAccessor<T> accessor)
    {
        return name().get(accessor);
    }

    public Attach setHandle(long value)
    {
        handle().set(value);
        return this;
    }

    public long getHandle()
    {
        return handle().get();
    }

    public Attach setRole(Role value)
    {
        role().set(Role.WRITE, value);
        return this;
    }

    public Role getRole()
    {
        return role().get(Role.READ);
    }

    public Attach setSendSettleMode(SenderSettleMode value)
    {
        sendSettleMode().set(SenderSettleMode.WRITE, value);
        return this;
    }

    public SenderSettleMode getSendSettleMode()
    {
        return sendSettleMode().get(SenderSettleMode.READ);
    }

    public Attach setReceiveSettleMode(ReceiverSettleMode value)
    {
        receiveSettleMode().set(ReceiverSettleMode.WRITE, value);
        return this;
    }

    public ReceiverSettleMode getReceiveSettleMode()
    {
        return receiveSettleMode().get(ReceiverSettleMode.READ);
    }

    public Source getSource()
    {
        return source();
    }

    public Target getTarget()
    {
        return target();
    }

    public MapType getUnsettled()
    {
        return unsettled();
    }

    public Attach setIncompleteUnsettled(boolean value)
    {
        incompleteUnsettled().set(value);
        return this;
    }

    public boolean getIncompleteUnsettled()
    {
        return incompleteUnsettled().get();
    }

    public Attach setInitialDeliveryCount(long value)
    {
        initialDeliveryCount().set(value);
        return this;
    }

    public long getInitialDeliveryCount()
    {
        return initialDeliveryCount().get();
    }

    public Attach setMaxMessageSize(long value)
    {
        maxMessageSize().set(value);
        return this;
    }

    public long getMaxMessageSize()
    {
        return maxMessageSize().get();
    }

    public Attach setOfferedCapabilities(ArrayType value)
    {
        offeredCapabilities().set(value);
        return this;
    }

    public ArrayType getOfferedCapabilities()
    {
        return offeredCapabilities();
    }

    public Attach setDesiredCapabilities(ArrayType value)
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

    private StringType name()
    {
        return name.wrap(mutableBuffer(), offsetBody(), true);
    }

    private UIntType handle()
    {
        return handle.wrap(mutableBuffer(), name().limit(), true);
    }

    private BooleanType role()
    {
        return role.wrap(mutableBuffer(), handle().limit(), true);
    }

    private UByteType sendSettleMode()
    {
        return sendSettleMode.wrap(mutableBuffer(), role().limit(), true);
    }

    private UByteType receiveSettleMode()
    {
        return receiveSettleMode.wrap(mutableBuffer(), sendSettleMode().limit(), true);
    }

    private Source source()
    {
        return source.wrap(mutableBuffer(), receiveSettleMode().limit(), true);
    }

    private Target target()
    {
        return target.wrap(mutableBuffer(), source().limit(), true);
    }

    private MapType unsettled()
    {
        return unsettled.wrap(buffer(), target().limit(), false);
    }

    private BooleanType incompleteUnsettled()
    {
        return incompleteUnsettled.wrap(mutableBuffer(), unsettled().limit(), true);
    }

    private UIntType initialDeliveryCount()
    {
        return initialDeliveryCount.wrap(mutableBuffer(), incompleteUnsettled().limit(), true);
    }

    private ULongType maxMessageSize()
    {
        return maxMessageSize.wrap(buffer(), initialDeliveryCount().limit(), false);
    }

    private ArrayType offeredCapabilities()
    {
        return offeredCapabilities.wrap(buffer(), maxMessageSize().limit(), false);
    }

    private ArrayType desiredCapabilities()
    {
        return desiredCapabilities.wrap(buffer(), offeredCapabilities().limit(), false);
    }

    private Fields properties()
    {
        return properties.wrap(mutableBuffer(), desiredCapabilities().limit(), true);
    }
}
