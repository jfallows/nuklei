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

import static uk.co.real_logic.agrona.LangUtil.rethrowUnchecked;

import org.kaazing.nuklei.Configuration;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.concurrent.AgentRunner;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.SigIntBarrier;

public final class Reaktor implements AutoCloseable
{
    private final Context context;
    private final AgentRunner runner;

    private Reaktor(Configuration config)
    {
        Context context = new Context();
        this.context = context.conclude(config);

        IdleStrategy idleStrategy = context.idleStrategy();
        ErrorHandler errorHandler = context.errorHandler();
        AtomicBuffer labelsBuffer = context.counterLabelsBuffer();
        AtomicBuffer countersBuffer = context.counterValuesBuffer();

        CountersManager countersManager = new CountersManager(labelsBuffer, countersBuffer);
        AtomicCounter errorCounter = countersManager.newCounter("errors");

        Conductor conductor = new Conductor(context);

        this.runner = new AgentRunner(idleStrategy, errorHandler, errorCounter, conductor);
    }

    public Reaktor start()
    {
        new Thread(runner).start();
        return this;
    }

    @Override
    public void close() throws Exception
    {
        try
        {
            runner.close();
            context.close();
        }
        catch (final Exception ex)
        {
            rethrowUnchecked(ex);
        }
    }

    public static Reaktor launch(final Configuration config)
    {
        Reaktor reaktor = new Reaktor(config);
        reaktor.start();
        return reaktor;
    }

    public static Reaktor launch()
    {
        return launch(new Configuration());
    }

    public static void main(final String[] args) throws Exception
    {
        try (final Reaktor reaktor = Reaktor.launch())
        {
            new SigIntBarrier().await();
        }
    }
}
