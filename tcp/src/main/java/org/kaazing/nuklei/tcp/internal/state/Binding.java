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

package org.kaazing.nuklei.tcp.internal.state;

import org.kaazing.nuklei.tcp.internal.types.control.BindingRO;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class Binding
{
    public BindingState state;

    private final long reference;

    private final MutableDirectBuffer buffer;
    private int offset;
    private int limit;

    public Binding(long reference)
    {
        this.reference = reference;
        this.buffer = new UnsafeBuffer(new byte[128]);
    }

    public long reference()
    {
        return this.reference;
    }

    public void binding(BindingRO binding)
    {
        buffer.putBytes(0, binding.buffer(), binding.offset(), binding.remaining());
        limit = binding.remaining();
    }

    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    public int offset()
    {
        return offset;
    }

    public int limit()
    {
        return limit;
    }
}
