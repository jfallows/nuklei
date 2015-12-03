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
package org.kaazing.nuklei.specification.tcp.streams;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class ConnectIT
{
    private final K3poRule k3po = new K3poRule();

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Before
    public void setupStreamFiles() throws Exception
    {
        int streamCapacity = 1024 * 1024;

        File nukleus = new File("target/nukleus-itests/tcp/streams/handler");
        createEmptyFile(nukleus.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        File handler = new File("target/nukleus-itests/handler/streams/tcp");
        createEmptyFile(handler.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);
    }

    @Test
    @Specification({
        "connect/establish.connection/nukleus",
        "connect/establish.connection/handler"
    })
    public void shouldEstablishConnection() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Test
    @Specification({
        "connect/nukleus.sent.data/nukleus",
        "connect/nukleus.sent.data/handler"
    })
    public void shouldReceiveNukleusSentData() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }


    @Test
    @Specification({
        "connect/handler.sent.data/nukleus",
        "connect/handler.sent.data/handler" })
    public void shouldReceiveHandlerSentData() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Test
    @Specification({
        "connect/echo.data/nukleus",
        "connect/echo.data/handler"
    })
    public void shouldEchoData() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Test
    @Specification({
        "connect/initiate.nukleus.close/nukleus",
        "connect/initiate.nukleus.close/handler" })
    public void shouldInitiateNukleusClose() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Test
    @Specification({
        "connect/initiate.handler.close/nukleus",
        "connect/initiate.handler.close/handler" })
    public void shouldInitiateHandlerClose() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "connect/concurrent.connections/nukleus",
        "connect/concurrent.connections/handler" })
    public void shouldEstablishConcurrentConnections() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }
}
