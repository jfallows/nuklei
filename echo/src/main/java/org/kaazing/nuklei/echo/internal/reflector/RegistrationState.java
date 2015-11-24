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
package org.kaazing.nuklei.echo.internal.reflector;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public class RegistrationState
{
    private final long referenceId;
    private final ReflectorMode mode;
    private final RingBuffer readBuffer;
    private final RingBuffer writeBuffer;

    public RegistrationState(
        long referenceId,
        ReflectorMode mode,
        RingBuffer readBuffer,
        RingBuffer writeBuffer)
    {
        this.referenceId = referenceId;
        this.mode = mode;
        this.readBuffer = readBuffer;
        this.writeBuffer = writeBuffer;
    }

    public long referenceId()
    {
        return this.referenceId;
    }

    public ReflectorMode mode()
    {
        return mode;
    }

    public RingBuffer readBuffer()
    {
        return readBuffer;
    }

    public RingBuffer writeBuffer()
    {
        return writeBuffer;
    }

    @Override
    public String toString()
    {
        return String.format("[mode=%s,referenceId=%d]", mode(), referenceId());
    }

}
