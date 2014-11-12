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
package org.kaazing.nuklei.amqp_1_0.sender;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.net.TcpManagerProxy;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;

public final class TcpSender implements Sender {
    
    private final TcpManagerProxy transport;
    private final long id;
    private final AtomicBuffer sendBuffer;
    private final int sendBufferOffset;
    
    public TcpSender(TcpManagerProxy transport, AtomicBuffer sendBuffer, long id) {
        this.transport = transport;
        this.id = id;

        this.sendBuffer = sendBuffer;
        this.sendBuffer.putLong(0, id);
        this.sendBufferOffset = BitUtil.SIZE_OF_LONG;
    }
    
    public <T extends Flyweight> T wrap(T flyweight) {
        flyweight.wrap(sendBuffer, sendBufferOffset);
        return flyweight;
    }

    public void send(int limit) {
        transport.send(sendBuffer, 0, limit);
    }

    public void close(boolean immediately) {
        transport.closeConnection(id, immediately);
    }

}