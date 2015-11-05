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

package org.kaazing.nuklei.tcp.internal;

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

import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastBufferDescriptor;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class TcpIT
{
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("tcp")
            .setDirectory("target/nukleus-itests")
            .setConductorBufferLength(1024 + RingBufferDescriptor.TRAILER_LENGTH)
            .setBroadcastBufferLength(1024 + BroadcastBufferDescriptor.TRAILER_LENGTH)
            .setCounterValuesBufferLength(1024);

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout).around(nukleus);

    @Test
    @Specification({
        "nuklei/specification/tcp/cnc/bind.ipv4.address.and.port/controller",
        "specification/tcp/rfc793/establish.connection/tcp.client",
        "nuklei/specification/tcp/stream/accept.begin.ipv4.address.and.port/destination" })
    public void establishConnection() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "server.sent.data/tcp.client",
        "server.sent.data/tcp.server" })
    public void serverSentData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "client.sent.data/tcp.client",
        "client.sent.data/tcp.server" })
    public void clientSentData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "bidirectional.data/tcp.client",
        "bidirectional.data/tcp.server" })
    public void bidirectionalData() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "server.close/tcp.client",
        "server.close/tcp.server" })
    public void serverClose() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "client.close/tcp.client",
        "client.close/tcp.server" })
    public void clientClose() throws Exception
    {
        k3po.finish();
    }

    @Ignore
    @Test
    @Specification({
        "concurrent.connections/tcp.client",
        "concurrent.connections/tcp.server" })
    public void concurrentConnections() throws Exception
    {
        k3po.finish();
    }
}
