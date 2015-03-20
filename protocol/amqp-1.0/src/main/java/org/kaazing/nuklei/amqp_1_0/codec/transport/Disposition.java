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
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Role;
import org.kaazing.nuklei.amqp_1_0.codec.messaging.DeliveryState;
import org.kaazing.nuklei.amqp_1_0.codec.types.BooleanType;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 2.7.6 "Disposition"
 */
public final class Disposition extends CompositeType
{

    public static final ThreadLocal<Disposition> LOCAL_REF = new ThreadLocal<Disposition>()
    {
        @Override
        protected Disposition initialValue()
        {
            return new Disposition();
        }
    };

    private final BooleanType role;
    private final UIntType first;
    private final UIntType last;
    private final BooleanType settled;
    private final DeliveryState.Described state;
    private final BooleanType batchable;

    public Disposition()
    {
        role = new BooleanType().watch((owner) ->
        {
            limit(1, owner.limit());
        });
        first = new UIntType().watch((owner) ->
        {
            limit(2, owner.limit());
        });
        last = new UIntType().watch((owner) ->
        {
            limit(3, owner.limit());
        });

        settled = new BooleanType().watch((owner) ->
        {
            limit(4, owner.limit());
        });
        state = new DeliveryState.Described().watch((owner) ->
        {
            limit(5, owner.limit());
        });
        batchable = new BooleanType().watch((owner) ->
        {
            limit(6, owner.limit());
        });
    }

    @Override
    public Disposition watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Disposition wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Disposition maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Disposition maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public Disposition setRole(Role value)
    {
        role().set(Role.WRITE, value);
        return this;
    }

    public Role getRole()
    {
        return role().get(Role.READ);
    }

    public Disposition setFirst(long value)
    {
        first().set(value);
        return this;
    }

    public long getFirst()
    {
        return first().get();
    }

    public Disposition setLast(long value)
    {
        last().set(value);
        return this;
    }

    public long getLast()
    {
        return last().get();
    }

    public Disposition setSettled(boolean value)
    {
        settled().set(value);
        return this;
    }

    public boolean getSettled()
    {
        return settled().get();
    }

    public DeliveryState.Described getState()
    {
        return state();
    }

    public Disposition setBatchable(boolean value)
    {
        batchable().set(value);
        return this;
    }

    public boolean getBatchable()
    {
        return batchable().get();
    }

    private BooleanType role()
    {
        return role.wrap(mutableBuffer(), offsetBody(), true);
    }

    private UIntType first()
    {
        return first.wrap(mutableBuffer(), role().limit(), true);
    }

    private UIntType last()
    {
        return last.wrap(mutableBuffer(), first().limit(), true);
    }

    private BooleanType settled()
    {
        return settled.wrap(mutableBuffer(), last().limit(), true);
    }

    private DeliveryState.Described state()
    {
        return state.wrap(mutableBuffer(), settled().limit(), true);
    }

    private BooleanType batchable()
    {
        return batchable.wrap(mutableBuffer(), state().limit(), true);
    }
}
