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
public enum Performative
{

    OPEN, BEGIN, ATTACH, FLOW, TRANSFER, DISPOSITION, DETACH, END, CLOSE;

    public static final LongFunction<Performative> READ = new LongFunction<Performative>()
    {

        @Override
        public Performative apply(long value)
        {
            switch ((int) value)
            {
            case 0x10:
                return OPEN;
            case 0x11:
                return BEGIN;
            case 0x12:
                return ATTACH;
            case 0x13:
                return FLOW;
            case 0x14:
                return TRANSFER;
            case 0x15:
                return DISPOSITION;
            case 0x16:
                return DETACH;
            case 0x17:
                return END;
            case 0x18:
                return CLOSE;
            default:
                return null;
            }
        }
    };

    public static final ToLongFunction<Performative> WRITE = new ToLongFunction<Performative>()
    {

        @Override
        public long applyAsLong(Performative value)
        {
            switch (value)
            {
            case OPEN:
                return 0x10;
            case BEGIN:
                return 0x11;
            case ATTACH:
                return 0x12;
            case FLOW:
                return 0x13;
            case TRANSFER:
                return 0x14;
            case DISPOSITION:
                return 0x15;
            case DETACH:
                return 0x16;
            case END:
                return 0x17;
            case CLOSE:
                return 0x18;
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

        public Described setPerformative(Performative value)
        {
            setDescriptor(WRITE, value);
            return this;
        }

        public Performative getPerformative()
        {
            return getDescriptor(READ);
        }
    }
}