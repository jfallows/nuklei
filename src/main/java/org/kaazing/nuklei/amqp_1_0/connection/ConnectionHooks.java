/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
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
package org.kaazing.nuklei.amqp_1_0.connection;

import java.util.function.Consumer;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Close;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Open;
import org.kaazing.nuklei.amqp_1_0.function.FrameConsumer;
import org.kaazing.nuklei.amqp_1_0.function.HeaderConsumer;

public class ConnectionHooks {

    public Consumer<Connection> whenInitialized = (c) -> {};
    public Consumer<Connection> whenError = (c) -> {};

    public HeaderConsumer<Connection> whenHeaderReceived = (c, h) -> {};
    public HeaderConsumer<Connection> whenHeaderSent = (c, h) -> {};
    public HeaderConsumer<Connection> whenHeaderReceivedNotEqualSent = (c, h) -> {};
    public HeaderConsumer<Connection> whenHeaderSentNotEqualReceived = (c, h) -> {};

    public FrameConsumer<Connection, Open> whenOpenReceived = (p, f, o) -> {};
    public FrameConsumer<Connection, Open> whenOpenSent = (p, f, o) -> {};
    public FrameConsumer<Connection, Close> whenCloseReceived = (p, f, c) -> {};
    public FrameConsumer<Connection, Close> whenCloseSent = (p, f, c) -> {};
    
}