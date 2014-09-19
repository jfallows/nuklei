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

import org.kaazing.nuklei.amqp_1_0.sender.TcpSenderFactory;
import org.kaazing.nuklei.concurrent.AtomicBuffer;

public final class ConnectionFactory {
    
    private final TcpSenderFactory senderFactory;
    private final ConnectionStateMachine stateMachine;

    public ConnectionFactory(ConnectionHooks connectionHooks, TcpSenderFactory senderFactory) {
        this.senderFactory = senderFactory;
        this.stateMachine = new ConnectionStateMachine(connectionHooks);
    }

    public Connection newConnection(long id, AtomicBuffer reassemblyBuffer) {
        return new Connection(stateMachine, senderFactory.newSender(id), reassemblyBuffer);
    }
}