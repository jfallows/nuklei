/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.nuklei.tcp.internal.cnc;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.rules.RuleChain.outerRule;
import static org.kaazing.nuklei.Configuration.BROADCAST_BUFFER_LENGTH_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.CONDUCTOR_BUFFER_LENGTH_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.COUNTER_VALUES_BUFFER_LENGTH_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.NukleusFactory;

import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastBufferDescriptor;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class CncIT
{
    private static final int CONDUCTOR_BUFFER_LENGTH = 1024 + RingBufferDescriptor.TRAILER_LENGTH;
    private static final int BROADCAST_BUFFER_LENGTH = 1024 + BroadcastBufferDescriptor.TRAILER_LENGTH;
    private static final int COUNTER_VALUES_BUFFER_LENGTH = 1024;

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/nuklei/specification/tcp/cnc");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Test
    @Specification({
        "bind.ipv4.address.and.port/controller"
    })
    public void shouldBindIPv4AddressAndPort() throws Exception
    {
        System.out.println("CncIT: cwd = " + new File(".").getAbsolutePath());

        // TODO: JUnit Rule setup
        Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/nukleus-itests/");
        properties.setProperty(CONDUCTOR_BUFFER_LENGTH_PROPERTY_NAME, Integer.toString(CONDUCTOR_BUFFER_LENGTH));
        properties.setProperty(BROADCAST_BUFFER_LENGTH_PROPERTY_NAME, Integer.toString(BROADCAST_BUFFER_LENGTH));
        properties.setProperty(COUNTER_VALUES_BUFFER_LENGTH_PROPERTY_NAME, Integer.toString(COUNTER_VALUES_BUFFER_LENGTH));
        NukleusFactory factory = NukleusFactory.instantiate();
        Configuration config = new Configuration(properties);
        Nukleus nukleus = factory.create("tcp", config);
        final AtomicBoolean finished = new AtomicBoolean();
        final AtomicInteger errorCount = new AtomicInteger();
        final IdleStrategy idler = new SleepingIdleStrategy(MILLISECONDS.toNanos(50L));
        Runnable runnable = () ->
        {
            while (!finished.get())
            {
                try
                {
                    int workCount = nukleus.doWork();
                    idler.idle(workCount);
                }
                catch (Exception e)
                {
                    errorCount.incrementAndGet();
                }
            }
        };
        Thread caller = new Thread(runnable);
        caller.start();

        k3po.finish();

        // TODO: JUnit Rule tear down
        finished.set(true);
        caller.join();

        assertEquals(0, errorCount.get());
    }

    @Test
    @Specification({
        "unbind.ipv4.address.and.port/controller",
        "unbind.ipv4.address.and.port/nukleus"
    })
    public void shouldUnbindIPv4AddressAndPort() throws Exception
    {
        k3po.finish();
    }

}