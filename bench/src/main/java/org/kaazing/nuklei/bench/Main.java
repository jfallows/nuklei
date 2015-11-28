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
package org.kaazing.nuklei.bench;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static uk.co.real_logic.agrona.concurrent.AgentRunner.startOnThread;

import java.net.InetSocketAddress;
import java.util.Properties;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.NukleusFactory;
import org.kaazing.nuklei.echo.internal.EchoController;
import org.kaazing.nuklei.tcp.internal.TcpController;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.AgentRunner;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.CompositeAgent;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.SigIntBarrier;
import uk.co.real_logic.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class Main
{
    public static void main(final String[] args) throws Exception
    {
        IdleStrategy idleStrategy = new SleepingIdleStrategy(MILLISECONDS.toNanos(100));
        ErrorHandler errorHandler = (throwable) -> { throwable.printStackTrace(System.err); };
        AtomicBuffer labelsBuffer = new UnsafeBuffer(new byte[CountersManager.LABEL_LENGTH]);
        AtomicBuffer countersBuffer = new UnsafeBuffer(new byte[CountersManager.COUNTER_LENGTH]);
        CountersManager counters = new CountersManager(labelsBuffer, countersBuffer);
        AtomicCounter errorCounter = counters.newCounter("errors");

        final Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/nukleus-example");
        Configuration config = new Configuration(properties);
        NukleusFactory factory = NukleusFactory.instantiate();

        Nukleus echo = factory.create("echo", config);
        Nukleus tcp = factory.create("tcp", config);

        Agent agent = new CompositeAgent(new NukleusAgent(tcp), new NukleusAgent(echo));
        startOnThread(new AgentRunner(idleStrategy, errorHandler, errorCounter, agent));

        TcpController tcpctl = (TcpController) factory.create("tcp.controller", config);
        EchoController echoctl = (EchoController) factory.create("echo.controller", config);

        Agent control = new CompositeAgent(new NukleusAgent(tcpctl), new NukleusAgent(echoctl));
        startOnThread(new AgentRunner(idleStrategy, errorHandler, errorCounter, control));

        tcpctl.capture("echo").get();
        echoctl.capture("tcp").get();
        tcpctl.route("echo").get();
        echoctl.route("tcp").get();

        tcpctl.bind("echo", new InetSocketAddress("localhost", 8080))
              .thenAccept((tcpRef) -> { echoctl.bind("tcp", tcpRef); }).get();

        // TODO: resource cleanup via try-with-resources
        System.out.println("echo listening on localhost:8080");
        new SigIntBarrier().await();
    }

    private static class NukleusAgent implements Agent
    {
        private final Nukleus nukleus;

        public NukleusAgent(Nukleus nukleus)
        {
            this.nukleus = nukleus;
        }
        @Override
        public int doWork() throws Exception
        {
            return nukleus.process();
        }

        @Override
        public String roleName()
        {
            return nukleus.name();
        }
    }
}
