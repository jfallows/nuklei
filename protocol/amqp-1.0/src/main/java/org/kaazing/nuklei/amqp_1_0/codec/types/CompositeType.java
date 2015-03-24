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
package org.kaazing.nuklei.amqp_1_0.codec.types;

import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 1.4 "Composite Type Representation"
 */
public class CompositeType extends ListType
{

    @Override
    public CompositeType watch(Consumer<Flyweight> notifier)
    {
        super.watch(notifier);
        return this;
    }

    @Override
    public CompositeType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public <T extends CompositeType> T as(T composite)
    {
        composite.wrap(mutableBuffer(), offset(), true);
        return composite;
    }

    public static class Described extends Type
    {

        private final ULongType.Descriptor descriptor;
        private final CompositeType composite;
        private int limit;

        public Described()
        {
            descriptor = new ULongType.Descriptor().watch((owner) -> limit(owner.limit()));
            composite = new CompositeType().watch((owner) -> limit(owner.limit()));
        }

        @Override
        public Kind kind()
        {
            return Kind.DESCRIBED;
        }

        @Override
        public Described watch(Consumer<Flyweight> notifier)
        {
            super.watch(notifier);
            return this;
        }

        @Override
        public Described wrap(DirectBuffer buffer, int offset, boolean mutable)
        {
            super.wrap(buffer, offset, mutable);
            return this;
        }

        protected void limit(int limit)
        {
            this.limit = limit;
            notifyChanged();
        }

        @Override
        public int limit()
        {
            return limit;
        }

        public void limit(int count, int limit)
        {
            composite().limit(count, limit);
        }

        public Described maxLength(int value)
        {
            composite().maxLength(value);
            return this;
        }

        public Described maxCount(int value)
        {
            composite().maxCount(value);
            return this;
        }

        protected int offsetBody()
        {
            return composite().offsetBody();
        }

        public <T> Described setDescriptor(ToLongFunction<T> mutator, T value)
        {
            descriptor().set(mutator, value);
            return this;
        }

        public Described setDescriptor(long value)
        {
            descriptor().set(value);
            return this;
        }

        public <R> R getDescriptor(LongFunction<R> accessor)
        {
            return descriptor().get(accessor);
        }

        public long getDescriptor()
        {
            return descriptor().get();
        }

        public CompositeType getComposite()
        {
            return composite();
        }

        private ULongType.Descriptor descriptor()
        {
            return descriptor.wrap(buffer(), offset(), true);
        }

        private CompositeType composite()
        {
            return composite.wrap(mutableBuffer(), descriptor().limit(), true);
        }

    }
}
