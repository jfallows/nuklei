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
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.amqp_1_0.codec.types.CompositeType;

import uk.co.real_logic.agrona.DirectBuffer;

/*
 * See AMQP 1.0 specification, section 3.4 "Delivery State"
 */
public enum Outcome
{

    ACCEPTED, REJECTED, RELEASED, MODIFIED, DECLARED;

    public static final LongFunction<Outcome> READ = new LongFunction<Outcome>()
    {

        @Override
        public Outcome apply(long value)
        {
            switch ((int) value)
            {
            case 0x24:
                return ACCEPTED;
            case 0x25:
                return REJECTED;
            case 0x26:
                return RELEASED;
            case 0x27:
                return MODIFIED;
            case 0x33:
                return DECLARED;
            default:
                return null;
            }
        }
    };

    public static final ToLongFunction<Outcome> WRITE = new ToLongFunction<Outcome>()
    {

        @Override
        public long applyAsLong(Outcome value)
        {
            switch (value)
            {
            case ACCEPTED:
                return 0x24;
            case REJECTED:
                return 0x25;
            case RELEASED:
                return 0x26;
            case MODIFIED:
                return 0x27;
            case DECLARED:
                return 0x33;
            default:
                throw new IllegalStateException();
            }
        }

    };

    public static final class Described extends CompositeType.Described
    {

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

        public Described setDeliveryState(Outcome value)
        {
            setDescriptor(WRITE, value);
            return this;
        }

        public Outcome getOutcome()
        {
            return getDescriptor(READ);
        }
    }
}