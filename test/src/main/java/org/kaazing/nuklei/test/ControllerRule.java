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
package org.kaazing.nuklei.test;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.kaazing.nuklei.Configuration.COMMAND_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.COUNTERS_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.RESPONSE_BUFFER_CAPACITY_PROPERTY_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.Controller;
import org.kaazing.nuklei.ControllerFactory;

import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.SleepingIdleStrategy;

public final class ControllerRule implements TestRule
{
    private final Class<? extends Controller>[] kinds;
    private final Map<Class<? extends Controller>, Controller> controllers;
    private final Properties properties;

    private Configuration configuration;

    @SafeVarargs
    public ControllerRule(Class<? extends Controller>... kinds)
    {
        this.kinds = requireNonNull(kinds);
        this.controllers = new HashMap<>();
        this.properties = new Properties();
    }

    public ControllerRule directory(String directory)
    {
        properties.setProperty(DIRECTORY_PROPERTY_NAME, directory);
        return this;
    }

    public ControllerRule commandBufferCapacity(int commandBufferCapacity)
    {
        properties.setProperty(COMMAND_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(commandBufferCapacity));
        return this;
    }

    public ControllerRule responseBufferCapacity(int responseBufferCapacity)
    {
        properties.setProperty(RESPONSE_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(responseBufferCapacity));
        return this;
    }

    public ControllerRule counterValuesBufferCapacity(int counterValuesBufferCapacity)
    {
        properties.setProperty(COUNTERS_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(counterValuesBufferCapacity));
        return this;
    }

    public <T extends Controller> T controller(Class<T> kind)
    {
        Controller controller = controllers.get(kind);

        if (controller == null)
        {
            throw new IllegalStateException("controller not found: " + kind.getName());
        }

        return kind.cast(controller);

    }

    private Configuration configuration()
    {
        if (configuration == null)
        {
            configuration = new Configuration(properties);
        }
        return configuration;
    }

    @Override
    public Statement apply(Statement base, Description description)
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                ControllerFactory factory = ControllerFactory.instantiate();
                Configuration config = configuration();
                final AtomicBoolean finished = new AtomicBoolean();
                final AtomicInteger errorCount = new AtomicInteger();
                final IdleStrategy idler = new SleepingIdleStrategy(MILLISECONDS.toNanos(50L));
                for (int i=0; i < kinds.length; i++)
                {
                    controllers.put(kinds[i], factory.create(kinds[i], config));
                }
                final Controller[] workers = controllers.values().toArray(new Controller[0]);
                Runnable runnable = () ->
                {
                    while (!finished.get())
                    {
                        try
                        {
                            int workCount = 0;

                            for (int i=0; i < workers.length; i++)
                            {
                                workCount += workers[i].process();
                            }

                            idler.idle(workCount);
                        }
                        catch (Exception ex)
                        {
                            errorCount.incrementAndGet();
                            ex.printStackTrace(System.err);
                        }
                    }
                    try
                    {
                        for (int i=0; i < workers.length; i++)
                        {
                            workers[i].close();
                        }
                    }
                    catch (Exception ex)
                    {
                        errorCount.incrementAndGet();
                        ex.printStackTrace(System.err);
                    }
                };
                Thread caller = new Thread(runnable);
                try
                {
                    caller.start();

                    base.evaluate();
                }
                finally
                {
                    finished.set(true);
                    caller.join();
                    assertEquals(0, errorCount.get());
                }
            }
        };
    }
}
