/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
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

package org.kaazing.nuklei.tcp.internal.types.control;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class UnboundRW extends UnboundType<MutableDirectBuffer>
{
    private final BindingRW binding = new BindingRW();

    public UnboundRW wrap(MutableDirectBuffer buffer, int offset)
    {
        super.wrap(buffer, offset);

        return this;
    }

    @Override
    public BindingRW binding()
    {
        return binding;
    }

    public UnboundRW correlationId(long correlationId)
    {
        buffer().putLong(offset() + FIELD_OFFSET_CORRELATION_ID, correlationId);
        return this;
    }

    public UnboundRW binding(BindingRO binding)
    {
        this.binding.wrap(buffer(), offset() + FIELD_OFFSET_BINDING, binding);
        return this;
    }
}
