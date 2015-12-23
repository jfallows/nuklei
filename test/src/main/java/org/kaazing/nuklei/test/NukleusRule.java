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
import static org.kaazing.nuklei.Configuration.COMMAND_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.COUNTERS_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.RESPONSE_BUFFER_CAPACITY_PROPERTY_NAME;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;
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
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class NukleusRule implements TestRule
{
    private final String[] names;
    private final Nukleus[] nuklei;
    private final Properties properties;

    private Configuration configuration;

    public NukleusRule(String... names)
    {
        this.names = requireNonNull(names);
        this.nuklei = new Nukleus[names.length];
        this.properties = new Properties();
    }

    public NukleusRule directory(String directory)
    {
        properties.setProperty(DIRECTORY_PROPERTY_NAME, directory);
        return this;
    }

    public NukleusRule commandBufferCapacity(int commandBufferCapacity)
    {
        properties.setProperty(COMMAND_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(commandBufferCapacity));
        return this;
    }

    public NukleusRule responseBufferCapacity(int responseBufferCapacity)
    {
        properties.setProperty(RESPONSE_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(responseBufferCapacity));
        return this;
    }

    public NukleusRule counterValuesBufferCapacity(int counterValuesBufferCapacity)
    {
        properties.setProperty(COUNTERS_BUFFER_CAPACITY_PROPERTY_NAME, valueOf(counterValuesBufferCapacity));
        return this;
    }

    public NukleusRule initialize(
        String reader,
        String writer)
    {
        Configuration configuration = configuration();
        int streamsBufferCapacity = configuration.streamsBufferCapacity();
        File directory = configuration.directory();

        File streams = new File(directory, String.format("%s/streams/%s", reader, writer));
        createEmptyFile(streams.getAbsoluteFile(), streamsBufferCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        return this;
    }

    public <T extends Nukleus> T lookup(Class<T> clazz)
    {
        for (Nukleus nukleus : nuklei)
        {
            if (clazz.isInstance(nukleus))
            {
                return clazz.cast(nukleus);
            }
        }

        throw new IllegalStateException("nukleus not found: " + clazz.getName());
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
                NukleusFactory factory = NukleusFactory.instantiate();
                Configuration config = configuration();
                final AtomicBoolean finished = new AtomicBoolean();
                final AtomicInteger errorCount = new AtomicInteger();
                final IdleStrategy idler = new SleepingIdleStrategy(MILLISECONDS.toNanos(50L));
                for (int i=0; i < names.length; i++)
                {
                    nuklei[i] = factory.create(names[i], config);
                }
                Runnable runnable = () ->
                {
                    while (!finished.get())
                    {
                        try
                        {
                            int workCount = 0;

                            for (int i=0; i < nuklei.length; i++)
                            {
                                workCount += nuklei[i].process();
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
                        for (int i=0; i < nuklei.length; i++)
                        {
                            nuklei[i].close();
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