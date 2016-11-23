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

public class ClientIT
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
        "control/bind/client/initial/controller",
        "control/bind/client/reply/controller",
        "control/route/client/initial/controller",
        "control/route/client/reply/controller",
        "streams/connection.established/client/source" })
    public void shouldEstablishConnection() throws Exception
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));
            server.setSoTimeout((int) SECONDS.toMillis(5));

            k3po.start();
            k3po.awaitBarrier("ROUTED_REPLY");

            try (Socket socket = server.accept())
            {
                k3po.finish();
            }
        }
    }

    @Test
    @Specification({
        "control/bind/client/initial/controller",
        "control/bind/client/reply/controller",
        "control/route/client/initial/controller",
        "control/route/client/reply/controller",
        "streams/server.sent.data/client/source" })
    public void shouldReceiveServerSentData() throws Exception
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED_REPLY");

            try (Socket socket = server.accept())
            {
                final OutputStream out = socket.getOutputStream();

                out.write("server data".getBytes());
            }
        }

        k3po.finish();
    }

    @Test
    @Specification({
        "control/bind/client/initial/controller",
        "control/bind/client/reply/controller",
        "control/route/client/initial/controller",
        "control/route/client/reply/controller",
        "streams/client.sent.data/client/source" })
    public void shouldReceiveClientSentData() throws Exception
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED_REPLY");

            try (Socket socket = server.accept())
            {
                final InputStream in = socket.getInputStream();

                byte[] buf = new byte[256];
                int len1 = in.read(buf);

                assertEquals("client data", new String(buf, 0, len1, UTF_8));
            }
        }

        k3po.finish();
    }

    @Test
    @Specification({
        "control/bind/client/initial/controller",
        "control/bind/client/reply/controller",
        "control/route/client/initial/controller",
        "control/route/client/reply/controller",
        "streams/echo.data/client/source" })
    public void shouldEchoData() throws Exception
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED_REPLY");

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

    @Test
    @Specification({
        "control/bind/client/initial/controller",
        "control/bind/client/reply/controller",
        "control/route/client/initial/controller",
        "control/route/client/reply/controller",
        "streams/server.close/client/source" })
    public void shouldInitiateServerClose() throws Exception
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED_REPLY");

            try (Socket socket = server.accept())
            {
                socket.shutdownOutput();
                k3po.finish();
            }
        }

    }

    @Test
    @Specification({
        "control/bind/client/initial/controller",
        "control/bind/client/reply/controller",
        "control/route/client/initial/controller",
        "control/route/client/reply/controller",
        "streams/client.close/client/source" })
    public void shouldInitiateClientClose() throws Exception
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED_REPLY");

            try (Socket socket = server.accept())
            {
                final InputStream in = socket.getInputStream();

                byte[] buf = new byte[256];
                int len = in.read(buf);

                assertEquals(-1, len);
            }
        }

        k3po.finish();
    }
}
