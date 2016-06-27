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
package org.kaazing.specification.nuklei.tcp.streams;

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

        File nukleus = new File("target/nukleus-itests/tcp/streams/source");
        createEmptyFile(nukleus.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        File source = new File("target/nukleus-itests/source/streams/tcp");
        createEmptyFile(source.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);
    }

    @Ignore
    @Test
    @Specification({
        "connect/establish.connection/nukleus",
        "connect/establish.connection/source"
    })
    public void shouldEstablishConnection() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "connect/nukleus.sent.data/nukleus",
        "connect/nukleus.sent.data/source"
    })
    public void shouldReceiveNukleusSentData() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "connect/source.sent.data/nukleus",
        "connect/source.sent.data/source" })
    public void shouldReceiveSourceSentData() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "connect/echo.data/nukleus",
        "connect/echo.data/source"
    })
    public void shouldEchoData() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "connect/initiate.nukleus.close/nukleus",
        "connect/initiate.nukleus.close/source" })
    public void shouldInitiateNukleusClose() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "connect/initiate.source.close/nukleus",
        "connect/initiate.source.close/source" })
    public void shouldInitiateSourceClose() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "connect/concurrent.connections/nukleus",
        "connect/concurrent.connections/source" })
    public void shouldEstablishConcurrentConnections() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }
}
