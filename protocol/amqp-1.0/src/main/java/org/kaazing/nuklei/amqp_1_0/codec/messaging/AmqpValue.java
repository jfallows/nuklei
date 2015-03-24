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
import org.kaazing.nuklei.amqp_1_0.codec.types.DynamicType;
import org.kaazing.nuklei.amqp_1_0.codec.types.StringType;
import org.kaazing.nuklei.amqp_1_0.codec.types.Type;
import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 3.2.8 "AMQP Value"
 */
public class AmqpValue extends Type
{
    private final DynamicType value;
    private final StringType stringType;

    public AmqpValue()
    {
        value = new DynamicType().watch((owner) -> notifyChanged());
        stringType = new StringType().watch((owner) -> notifyChanged());
    }

    @Override
    public AmqpValue watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public AmqpValue wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    @Override
    public int limit()
    {
        return value(false).limit();
    }

    public <T> T getValue(DirectBufferAccessor<T> accessor)
    {
        switch (value(false).kind())
        {
        case STRING:
            stringType.wrap(buffer(), offset(), false);
            return stringType.get(accessor);
        default:
            throw new RuntimeException("Not implemented yet");
        }
    }

    public <T> AmqpValue setValue(MutableDirectBufferMutator<T> mutator, T newValue)
    {
        // FIXME: proper type detection to set the kind of the value dynamic type
        if (newValue instanceof String)
        {
            stringType.wrap(buffer(), offset(), true);
            stringType.set(mutator, newValue);
            return this;
        }

        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public Kind kind()
    {
        return Kind.AMQPVALUE;
    }

    private DynamicType value(boolean mutable)
    {
        return value.wrap(buffer(), offset(), mutable);
    }
}
