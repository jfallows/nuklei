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
package org.kaazing.nuklei.shell;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Scanner;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.http.internal.HttpController;
import org.kaazing.nuklei.reaktor.internal.Reaktor;
import org.kaazing.nuklei.tcp.internal.TcpController;
import org.kaazing.nuklei.ws.internal.WsController;

public final class Main
{
    public static void main(final String[] args) throws Exception
    {
        final Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/controller-example");
        Configuration config = new Configuration(properties);

        try (Reaktor reaktor = Reaktor.launch(config, n -> true, c -> true))
        {
            TcpController tcpctl = reaktor.controller(TcpController.class);
            WsController wsctl = reaktor.controller(WsController.class);
            HttpController httpctl = reaktor.controller(HttpController.class);

            long tcpInitRef = tcpctl.bind(0x21).get();
            long httpInitRef = httpctl.bind(0x21).get();
            long wsInitRef = wsctl.bind(0x21).get();
            long wsReplyRef = wsctl.bind(0x22).get();
            long httpReplyRef = httpctl.bind(0x22).get();
            long tcpReplyRef = tcpctl.bind(0x22).get();

            tcpctl.route("any", tcpInitRef, "http", httpInitRef, new InetSocketAddress("localhost", 8080)).get();
            httpctl.route("tcp", httpInitRef, "ws", wsInitRef, singletonMap(":path", "/")).get();
            wsctl.route("http", wsInitRef, "ws", wsReplyRef, null).get();
            wsctl.route("ws", wsReplyRef, "http", httpReplyRef, null).get();
            httpctl.route("ws", httpReplyRef, "tcp", tcpReplyRef, emptyMap()).get();
            tcpctl.route("http", tcpReplyRef, "any", 0, null);

            System.out.println("echo bound to ws://localhost:8080/");

            try (Scanner scanner = new Scanner(System.in))
            {
                scanner.nextLine();
            }
        }
    }

    private Main()
    {
    }
}
