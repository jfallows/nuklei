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
import org.kaazing.nuklei.amqp_1_0.codec.definitions.Error;
import org.kaazing.nuklei.amqp_1_0.codec.types.BooleanType;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;
import org.kaazing.nuklei.amqp_1_0.codec.types.UIntType;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 2.7.7 "Detach"
 */
public final class Detach extends CompositeType
{

    public static final ThreadLocal<Detach> LOCAL_REF = new ThreadLocal<Detach>()
    {
        @Override
        protected Detach initialValue()
        {
            return new Detach();
        }
    };

    private final UIntType handle;
    private final BooleanType closed;
    private final Error error;

    public Detach()
    {
        handle = new UIntType().watch((owner) ->
        {
            limit(1, owner.limit());
        });
        closed = new BooleanType().watch((owner) ->
        {
            limit(2, owner.limit());
        });
        error = new Error().watch((owner) ->
        {
            limit(3, owner.limit());
        });
    }

    @Override
    public Detach watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Detach wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Detach maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Detach maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public Detach setHandle(long value)
    {
        handle().set(value);
        return this;
    }

    public long getHandle()
    {
        return handle().get();
    }

    public Detach setClosed(boolean value)
    {
        closed().set(value);
        return this;
    }

    public boolean getClosed()
    {
        return closed().get();
    }

    public Error getError()
    {
        return error();
    }

    private UIntType handle()
    {
        return handle.wrap(mutableBuffer(), offsetBody(), true);
    }

    private BooleanType closed()
    {
        return closed.wrap(mutableBuffer(), handle().limit(), true);
    }

    private Error error()
    {
        return error.wrap(mutableBuffer(), closed().limit(), true);
    }
}
