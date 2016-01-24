/*
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

var InetSocketAddress = Java.type('java.net.InetSocketAddress');

nuklei.echo.capture("ws")
  .thenCompose(function(v) { return nuklei.ws.capture("echo"); })
  .thenCompose(function(v) { return nuklei.ws.capture("http"); })
  .thenCompose(function(v) { return nuklei.http.capture("ws"); })
  .thenCompose(function(v) { return nuklei.http.capture("tcp"); })
  .thenCompose(function(v) { return nuklei.tcp.capture("http"); })
  .thenCompose(function(v) { return nuklei.echo.route("ws"); })
  .thenCompose(function(v) { return nuklei.ws.route("echo"); })
  .thenCompose(function(v) { return nuklei.ws.route("http"); })
  .thenCompose(function(v) { return nuklei.http.route("ws"); })
  .thenCompose(function(v) { return nuklei.http.route("tcp"); })
  .thenCompose(function(v) { return nuklei.tcp.route("http"); })
  .thenCompose(function(v) { return nuklei.echo.bind("ws"); })
  .thenCompose(function(echoRef) { return nuklei.ws.bind("echo", echoRef, "http", null); })
  .thenCompose(function(wsRef) { return nuklei.http.bind("ws", wsRef, "tcp", { ":path": "/" }); })
  .thenCompose(function(httpRef) { return nuklei.tcp.bind("http", httpRef, new InetSocketAddress("localhost", 8080)); })
  .thenAccept(function(tcpRef) { return print('echo bound to ws://localhost:8080/'); })
  .join();
