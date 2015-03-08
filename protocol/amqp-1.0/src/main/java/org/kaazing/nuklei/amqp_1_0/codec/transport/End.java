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
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 2.7.8 "End"
 */
public final class End extends CompositeType
{

    public static final ThreadLocal<End> LOCAL_REF = new ThreadLocal<End>()
    {
        @Override
        protected End initialValue()
        {
            return new End();
        }
    };

    private final Error error;

    public End()
    {
        error = new Error().watch((owner) ->
        {
            limit(1, owner.limit());
        });
    }

    @Override
    public End watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public End wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public End maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public End maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public Error getError()
    {
        return error();
    }

    public boolean hasError()
    {
        return error.offset() < limit();
    }

    private Error error()
    {
        return error.wrap(mutableBuffer(), offsetBody(), true);
    }
}
