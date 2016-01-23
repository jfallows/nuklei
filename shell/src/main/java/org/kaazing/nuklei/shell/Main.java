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
package org.kaazing.nuklei.shell;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static uk.co.real_logic.agrona.concurrent.AgentRunner.startOnThread;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.Controller;
import org.kaazing.nuklei.ControllerFactory;
import org.kaazing.nuklei.echo.internal.EchoController;
import org.kaazing.nuklei.http.internal.HttpController;
import org.kaazing.nuklei.tcp.internal.TcpController;
import org.kaazing.nuklei.ws.internal.WsController;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.AgentRunner;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.CompositeAgent;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
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
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/controller-example");
        Configuration config = new Configuration(properties);

        ControllerFactory controllers = ControllerFactory.instantiate();
        TcpController tcpctl = controllers.create(TcpController.class, config);
        WsController wsctl = controllers.create(WsController.class, config);
        HttpController httpctl = controllers.create(HttpController.class, config);
        EchoController echoctl = controllers.create(EchoController.class, config);

        Agent control = new CompositeAgent(new CompositeAgent(new ControllerAgent(tcpctl), new ControllerAgent(httpctl)),
                                           new CompositeAgent(new ControllerAgent(wsctl), new ControllerAgent(echoctl)));

        AgentRunner runner = new AgentRunner(idleStrategy, errorHandler, errorCounter, control);

        startOnThread(runner);

        allOf(echoctl.capture("ws"),
              wsctl.capture("echo"),
              wsctl.capture("http"),
              httpctl.capture("ws"),
              httpctl.capture("tcp"),
              tcpctl.capture("http"))
        .thenCompose(v -> { return allOf(echoctl.route("ws"),
                                         wsctl.route("echo"),
                                         wsctl.route("http"),
                                         httpctl.route("ws"),
                                         httpctl.route("tcp"),
                                         tcpctl.route("http"));
                          })
        .thenCompose(v -> echoctl.bind("ws"))
        .whenComplete((value, ex) -> { /* exception handler */ })
        .thenCompose(echoRef -> { return wsctl.bind("echo", echoRef, "http", null); })
        .thenCompose(wsRef -> { return httpctl.bind("ws", wsRef, "tcp", singletonMap(":path", "/")); })
        .thenCompose(httpRef -> { return tcpctl.bind("http", httpRef, new InetSocketAddress("localhost", 8080)); })
        .join();

        // TODO: resource cleanup via try-with-resources
        System.out.println("echo bound to ws://localhost:8080/");

        runner.close();
    }

    private static class ControllerAgent implements Agent
    {
        private final Controller controller;

        public ControllerAgent(Controller controller)
        {
            this.controller = controller;
        }
        @Override
        public int doWork() throws Exception
        {
            return controller.process();
        }

        @Override
        public String roleName()
        {
            return controller.kind().getSimpleName();
        }
    }
}
