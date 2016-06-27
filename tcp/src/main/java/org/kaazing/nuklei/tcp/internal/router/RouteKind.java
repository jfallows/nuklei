/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.tcp.internal.router;

import uk.co.real_logic.agrona.concurrent.AtomicCounter;

public enum RouteKind
{
    BIND
    {
        @Override
        public final long nextRef(
            AtomicCounter counter)
        {
            // positive, even, non-zero
            counter.increment();
            return counter.get() << 1L;
        }
    },
    PREPARE
    {
        @Override
        public final long nextRef(
            AtomicCounter counter)
        {
            // positive, odd
            return (counter.increment() << 1L) | 1L;
        }
    };

    public static final int BIND_ID = BIND.ordinal();
    public static final int PREPARE_ID = PREPARE.ordinal();

    public abstract long nextRef(
        AtomicCounter counter);

    public static RouteKind of(
        long ref)
    {
        switch ((int)ref & 0x01)
        {
        case 0:
            return BIND;
        case 1:
            return PREPARE;
        default:
            throw new IllegalArgumentException();
        }
    }
}
