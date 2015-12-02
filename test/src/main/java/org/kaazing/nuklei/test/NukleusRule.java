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
package org.kaazing.nuklei.test;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.kaazing.nuklei.Configuration.RESPONSE_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.COMMAND_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.COUNTER_VALUES_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.NukleusFactory;

import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.SleepingIdleStrategy;

public final class NukleusRule implements TestRule
{
    private final String name;
    private final Properties properties;

    public NukleusRule(String name)
    {
        this.name = requireNonNull(name);
        this.properties = new Properties();
    }

    public NukleusRule setDirectory(String directory)
    {
        properties.setProperty(DIRECTORY_PROPERTY_NAME, directory);
        return this;
    }

    public NukleusRule setCommandBufferCapacity(int commandBufferCapacity)
    {
        properties.setProperty(COMMAND_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(commandBufferCapacity));
        return this;
    }

    public NukleusRule setResponseBufferCapacity(int responseBufferCapacity)
    {
        properties.setProperty(RESPONSE_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(responseBufferCapacity));
        return this;
    }

    public NukleusRule setCounterValuesBufferCapacity(int counterValuesBufferCapacity)
    {
        properties.setProperty(COUNTER_VALUES_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(counterValuesBufferCapacity));
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description)
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                NukleusFactory factory = NukleusFactory.instantiate();
                Configuration config = new Configuration(properties);
                final AtomicBoolean finished = new AtomicBoolean();
                final AtomicInteger errorCount = new AtomicInteger();
                final IdleStrategy idler = new SleepingIdleStrategy(MILLISECONDS.toNanos(50L));
                final Nukleus nukleus = factory.create(name, config);
                Runnable runnable = () ->
                {
                    while (!finished.get())
                    {
                        try
                        {
                            int workCount = nukleus.process();
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
                        nukleus.close();
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