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
package org.kaazing.nuklei.tcp.internal.streams;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.nuklei.test.NukleusRule;

public class AcceptIT
{
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("tcp")
            .directory("target/nukleus-itests")
            .commandBufferCapacity(1024)
            .responseBufferCapacity(1024)
            .counterValuesBufferCapacity(1024)
            .streams("source", "tcp")
            .streams("destination", "tcp");

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Ignore
    @Test
    @Specification({
        "nuklei/tcp/control/capture.destination/controller",
        "nuklei/tcp/control/route.destination/controller",
        "nuklei/tcp/control/bind.socket.destination/controller",
        "tcp/rfc793/establish.connection/client",
        "nuklei/tcp/streams/accept/establish.connection/destination" })
    public void shouldEstablishConnection() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/tcp/control/capture.destination/controller",
        "nuklei/tcp/control/route.destination/controller",
        "nuklei/tcp/control/bind.socket.destination/controller",
        "tcp/rfc793/server.sent.data/client",
        "nuklei/tcp/streams/accept/destination.sent.data/destination" })
    public void shouldReceiveServerSentData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/tcp/control/capture.destination/controller",
        "nuklei/tcp/control/route.destination/controller",
        "nuklei/tcp/control/bind.socket.destination/controller",
        "tcp/rfc793/client.sent.data/client",
        "nuklei/tcp/streams/accept/nukleus.sent.data/destination" })
    public void shouldReceiveClientSentData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/tcp/control/capture.destination/controller",
        "nuklei/tcp/control/route.destination/controller",
        "nuklei/tcp/control/bind.socket.destination/controller",
        "tcp/rfc793/echo.data/client",
        "nuklei/tcp/streams/accept/echo.data/destination" })
    public void shouldEchoData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/tcp/control/capture.destination/controller",
        "nuklei/tcp/control/route.destination/controller",
        "nuklei/tcp/control/bind.socket.destination/controller",
        "tcp/rfc793/server.close/client",
        "nuklei/tcp/streams/accept/initiate.destination.close/destination" })
    public void shouldInitiateServerClose() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "nuklei/tcp/control/capture.destination/controller",
        "nuklei/tcp/control/route.destination/controller",
        "nuklei/tcp/control/bind.socket.destination/controller",
        "tcp/rfc793/client.close/client",
        "nuklei/tcp/streams/accept/initiate.nukleus.close/destination" })
    public void shouldInitiateClientClose() throws Exception
    {
        k3po.finish();
    }

    @Ignore("non-deterministic ordering of multiple streams")
    @Test
    @Specification({
        "nuklei/tcp/control/capture.destination/controller",
        "nuklei/tcp/control/route.destination/controller",
        "nuklei/tcp/control/bind.socket.destination/controller",
        "tcp/rfc793/concurrent.connections/client",
        "nuklei/tcp/streams/accept/concurrent.connections/destination" })
    public void shouldEstablishConcurrentConnections() throws Exception
    {
        k3po.finish();
    }
}
