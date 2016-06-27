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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.rules.RuleChain.outerRule;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.nuklei.test.NukleusRule;

public class PrepareIT
{
    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/nuklei/tcp");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("tcp")
        .directory("target/nukleus-itests")
        .commandBufferCapacity(1024)
        .responseBufferCapacity(1024)
        .counterValuesBufferCapacity(1024)
        .streams("tcp", "source");

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Test
    @Specification({
        "control/prepare/controller",
        "control/route/controller",
        "streams/connection.established/prepare.source" })
    public void shouldEstablishConnection() throws Exception
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED");

            server.setSoTimeout((int) SECONDS.toMillis(5));
            server.accept().close();

            k3po.finish();
        }
    }

    @Test
    @Specification({
        "control/prepare/controller",
        "control/route/controller",
        "streams/echo.data/prepare.source" })
    public void shouldEchoData() throws Exception
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED");

            try (Socket socket = server.accept())
            {
                final InputStream in = socket.getInputStream();
                final OutputStream out = socket.getOutputStream();

                out.write("server data 1".getBytes());

                byte[] buf1 = new byte[256];
                int len1 = in.read(buf1);

                out.write("server data 2".getBytes());

                byte[] buf2 = new byte[256];
                int len2 = in.read(buf2);

                assertEquals("client data 1", new String(buf1, 0, len1, UTF_8));
                assertEquals("client data 2", new String(buf2, 0, len2, UTF_8));
            }
        }

        k3po.finish();
    }
}
