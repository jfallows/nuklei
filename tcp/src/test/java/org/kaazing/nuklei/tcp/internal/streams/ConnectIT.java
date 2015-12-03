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
package org.kaazing.nuklei.tcp.internal.streams;

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
import org.kaazing.nuklei.test.NukleusRule;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class ConnectIT
{
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("tcp")
            .setDirectory("target/nukleus-itests")
            .setCommandBufferCapacity(1024)
            .setResponseBufferCapacity(1024)
            .setCounterValuesBufferCapacity(1024);

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Before
    public void setupStreamFiles() throws Exception
    {
        int streamCapacity = 1024 * 1024;

        File handler = new File("target/nukleus-itests/handler/streams/tcp");
        createEmptyFile(handler.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);
    }

    @Test
    @Specification({
        "nuklei/specification/tcp/control/prepare.address.and.port/controller",
        "specification/tcp/rfc793/establish.connection/server",
        "nuklei/specification/tcp/streams/connect/establish.connection/handler" })
    public void shouldEstablishConnection() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/specification/tcp/control/prepare.address.and.port/controller",
        "specification/tcp/rfc793/server.sent.data/server",
        "nuklei/specification/tcp/streams/connect/nukleus.sent.data/handler" })
    public void shouldReceiveServerSentData() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/specification/tcp/control/prepare.address.and.port/controller",
        "specification/tcp/rfc793/client.sent.data/server",
        "nuklei/specification/tcp/streams/connect/handler.sent.data/handler" })
    public void shouldReceiveClientSentData() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/specification/tcp/control/prepare.address.and.port/controller",
        "specification/tcp/rfc793/echo.data/server",
        "nuklei/specification/tcp/streams/connect/echo.data/handler" })
    public void shouldEchoData() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/specification/tcp/control/prepare.address.and.port/controller",
        "specification/tcp/rfc793/server.close/server",
        "nuklei/specification/tcp/streams/connect/initiate.nukleus.close/handler" })
    public void shouldInitiateServerClose() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/specification/tcp/control/prepare.address.and.port/controller",
        "specification/tcp/rfc793/client.close/server",
        "nuklei/specification/tcp/streams/connect/initiate.handler.close/handler" })
    public void shouldInitiateClientClose() throws Exception
    {
        k3po.finish();
    }

    @Ignore("non-deterministic ordering of multiple streams")
    @Test
    @Specification({
        "nuklei/specification/tcp/control/prepare.address.and.port/controller",
        "specification/tcp/rfc793/concurrent.connections/server",
        "nuklei/specification/tcp/streams/connect/concurrent.connections/handler" })
    public void shouldEstablishConcurrentConnections() throws Exception
    {
        k3po.finish();
    }
}
