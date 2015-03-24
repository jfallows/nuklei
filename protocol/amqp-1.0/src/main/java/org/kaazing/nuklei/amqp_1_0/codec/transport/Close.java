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
 * See AMQP 1.0 specification, section 2.7.9 "Close"
 */
public final class Close extends CompositeType
{

    public static final ThreadLocal<Close> LOCAL_REF = new ThreadLocal<Close>()
    {
        @Override
        protected Close initialValue()
        {
            return new Close();
        }
    };

    private final Error error;

    // unit tests
    Close()
    {
        error = new Error().watch((owner) ->
        {
            limit(1, owner.limit());
        });
    }

    @Override
    public Close watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public Close wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public Close maxLength(int value)
    {
        super.maxLength(value);
        return this;
    }

    @Override
    public Close maxCount(int value)
    {
        super.maxCount(value);
        return this;
    }

    public Error getError()
    {
        return error();
    }

    private Error error()
    {
        return error.wrap(mutableBuffer(), offsetBody(), true);
    }
}
