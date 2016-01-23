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

nuklei.echo.capture("ws").get();
nuklei.ws.capture("echo").get();
nuklei.ws.capture("http").get();
nuklei.http.capture("ws").get();
nuklei.http.capture("tcp").get();
nuklei.tcp.capture("http").get();

nuklei.echo.route("ws").get();
nuklei.ws.route("echo").get();
nuklei.ws.route("http").get();
nuklei.http.route("ws").get();
nuklei.http.route("tcp").get();
nuklei.tcp.route("http").get();

var InetSocketAddress = Java.type('java.net.InetSocketAddress');

var echoRef = nuklei.echo.bind("ws").get();
var wsRef = nuklei.ws.bind("echo", echoRef, "http", null).get();
var httpRef = nuklei.http.bind("ws", wsRef, "tcp", { ":path": "/" }).get();
nuklei.tcp.bind("http", httpRef, new InetSocketAddress("localhost", 8080)).get();

print('echo bound to ws://localhost:8080/')
