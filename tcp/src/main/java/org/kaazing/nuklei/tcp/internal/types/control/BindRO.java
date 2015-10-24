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

import static org.kaazing.nuklei.tcp.internal.types.Types.checkLimit;

import org.kaazing.nuklei.tcp.internal.types.StringRO;

import uk.co.real_logic.agrona.DirectBuffer;

public final class BindRO extends BindType<DirectBuffer>
{
    private final StringRO source = new StringRO();
    private final StringRO destination = new StringRO();
    private final AddressRO address = new AddressRO();

    public void wrap(DirectBuffer buffer, int offset, int actingLimit)
    {
        super.wrap(buffer, offset);

        this.source.wrap(buffer, offset + FIELD_OFFSET_SOURCE, actingLimit);
        this.destination.wrap(buffer, source.limit() + FIELD_SIZE_SOURCE_BINDING_REF, actingLimit);
        this.address.wrap(buffer, destination.limit(), actingLimit);

        checkLimit(limit(), actingLimit);
    }

    public StringRO source()
    {
        return source;
    }

    public StringRO destination()
    {
        return destination;
    }

    public AddressRO address()
    {
        return address;
    }
}
