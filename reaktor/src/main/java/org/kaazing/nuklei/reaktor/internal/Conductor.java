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
package org.kaazing.nuklei.reaktor.internal;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Conductor implements Agent
{
    private final RingBuffer toConductorCommands;
    private final MessageHandler onConductorCommandFunc;

    Conductor(Context context)
    {
        this.toConductorCommands = context.toConductorCommands();
        this.onConductorCommandFunc = this::onConductorCommand;
    }

    @Override
    public int doWork() throws Exception
    {
        int workCount = 0;

        workCount += toConductorCommands.read(onConductorCommandFunc);

        return workCount;
    }

    @Override
    public String roleName()
    {
        return "conductor";
    }

    private void onConductorCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        // TODO
    }
}
