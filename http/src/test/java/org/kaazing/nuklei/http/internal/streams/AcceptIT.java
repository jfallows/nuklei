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
package org.kaazing.nuklei.http.internal.streams;

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

public class AcceptIT
{
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("http")
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

        File handler = new File("target/nukleus-itests/source/streams/http");
        createEmptyFile(handler.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/specification/http/control/bind.address.and.port/controller",
        "specification/tcp/rfc793/establish.connection/client",
        "nuklei/specification/http/streams/accept/establish.connection/source" })
    public void shouldEstablishConnection() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/specification/tcp/control/bind.address.and.port/controller",
        "specification/tcp/rfc793/server.sent.data/client",
        "nuklei/specification/tcp/streams/accept/handler.sent.data/handler" })
    public void shouldReceiveServerSentData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/specification/tcp/control/bind.address.and.port/controller",
        "specification/tcp/rfc793/client.sent.data/client",
        "nuklei/specification/tcp/streams/accept/nukleus.sent.data/handler" })
    public void shouldReceiveClientSentData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/specification/tcp/control/bind.address.and.port/controller",
        "specification/tcp/rfc793/echo.data/client",
        "nuklei/specification/tcp/streams/accept/echo.data/handler" })
    public void shouldEchoData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/specification/tcp/control/bind.address.and.port/controller",
        "specification/tcp/rfc793/server.close/client",
        "nuklei/specification/tcp/streams/accept/initiate.handler.close/handler" })
    public void shouldInitiateServerClose() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/specification/tcp/control/bind.address.and.port/controller",
        "specification/tcp/rfc793/client.close/client",
        "nuklei/specification/tcp/streams/accept/initiate.nukleus.close/handler" })
    public void shouldInitiateClientClose() throws Exception
    {
        k3po.finish();
    }

    @Ignore("non-deterministic ordering of multiple streams")
    @Test
    @Specification({
        "nuklei/specification/tcp/control/bind.address.and.port/controller",
        "specification/tcp/rfc793/concurrent.connections/client",
        "nuklei/specification/tcp/streams/accept/concurrent.connections/handler" })
    public void shouldEstablishConcurrentConnections() throws Exception
    {
        k3po.finish();
    }
}
