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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static uk.co.real_logic.agrona.LangUtil.rethrowUnchecked;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.NukleusFactory;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.collections.ArrayUtil;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.AgentRunner;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.SigIntBarrier;
import uk.co.real_logic.agrona.concurrent.SleepingIdleStrategy;

public final class Reaktor implements AutoCloseable
{
    private final AgentRunner runner;

    private Reaktor(Configuration config)
    {
        NukleusFactory factory = NukleusFactory.instantiate();

        Nukleus[] nuklei = new Nukleus[0];

        for (String name : factory.names())
        {
            if (!name.endsWith(".controller"))
            {
                Nukleus nukleus = factory.create(name, config);
                nuklei = ArrayUtil.add(nuklei, nukleus);
            }
        }

        final Nukleus[] workers = nuklei;
        IdleStrategy idleStrategy = new SleepingIdleStrategy(MILLISECONDS.toNanos(50L));
        ErrorHandler errorHandler = (throwable) -> { throwable.printStackTrace(System.err); };
        Agent agent = new Agent()
        {
            @Override
            public String roleName()
            {
                return "reaktor";
            }

            @Override
            public int doWork() throws Exception
            {
                int work = 0;

                for (int i=0; i < workers.length; i++)
                {
                    work += workers[i].process();
                }

                return work;
            }
        };

        this.runner = new AgentRunner(idleStrategy, errorHandler, null, agent);
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
        // TODO: command line parameter for directory
        String directory = IoUtil.tmpDirName() + "org.kaazing.nuklei.reaktor";
        System.setProperty(Configuration.DIRECTORY_PROPERTY_NAME, directory);

        try (final Reaktor reaktor = Reaktor.launch())
        {
            System.out.println("Started in " + directory);

            new SigIntBarrier().await();
        }
    }
}
