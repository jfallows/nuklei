/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.function;

import java.util.Objects;

import uk.co.real_logic.agrona.DirectBuffer;

@FunctionalInterface
public interface StatefulMikro<T>
{
    void onMessage(
        final T state, final Object header, final int typeId,
        final DirectBuffer buffer, final int offset, final int length);

    default Mikro statefulBy(StateSupplier<T> stateful)
    {
        Objects.requireNonNull(stateful);
        return (header, typeId, buffer, offset, length) ->
        {
            T state = stateful.supply(header, typeId);
            onMessage(state, header, typeId, buffer, offset, length);
        };
    }

    @FunctionalInterface
    public interface StateSupplier<T>
    {
        T supply(Object header, int typeId);
    }
}
