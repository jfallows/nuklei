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
package org.kaazing.nuklei.ws.internal.util.function;

import java.util.Objects;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface LongObjectBiConsumer<T> extends BiConsumer<Long, T>
{
    void accept(long value, T t);

    @Override
    default void accept(Long value, T t)
    {
        this.accept(value.longValue(), t);
    }

    default LongObjectBiConsumer<T> andThen(
        LongObjectBiConsumer<? super T> after)
    {
        Objects.requireNonNull(after);

        return (l, r) ->
        {
            accept(l, r);
            after.accept(l, r);
        };
    }
}
