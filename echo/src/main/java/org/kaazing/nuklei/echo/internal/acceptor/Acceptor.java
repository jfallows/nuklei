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
package org.kaazing.nuklei.echo.internal.acceptor;

import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.echo.internal.Context;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Acceptor implements Nukleus, Consumer<AcceptorCommand>
{
    private final RingBuffer conductorCommands;

    public Acceptor(Context context)
    {
        this.conductorCommands = context.conductorCommands();
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += conductorCommands.read(this::handleCommand);

        return weight;
    }

    @Override
    public String name()
    {
        return "conductor";
    }

    @Override
    public void accept(AcceptorCommand command)
    {
        // TODO Auto-generated method stub
    }

    private void handleCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        // TODO
    }
}
