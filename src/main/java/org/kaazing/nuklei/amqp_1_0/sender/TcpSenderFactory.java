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
import org.kaazing.nuklei.net.TcpManagerHeadersDecoder;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public final class TcpSenderFactory implements SenderFactory {

    private final MutableDirectBuffer sendBuffer;

    public TcpSenderFactory(MutableDirectBuffer sendBuffer) {
        this.sendBuffer = sendBuffer;
    }

    public Sender newSender(Object headers) {
        TcpManagerHeadersDecoder tcpHeaders = (TcpManagerHeadersDecoder) headers;
        return new TcpSender(tcpHeaders, sendBuffer);
    }

    private static final class TcpSender implements Sender {
        
        private final TcpManagerHeadersDecoder tcpHeaders;
        private final MutableDirectBuffer sendBuffer;
        private final int sendBufferOffset;

        public TcpSender(TcpManagerHeadersDecoder tcpHeaders, MutableDirectBuffer sendBuffer) {
            this.tcpHeaders = tcpHeaders;

            this.sendBuffer = sendBuffer;
            this.sendBufferOffset = tcpHeaders.length();
        }

        public <T extends Flyweight> T wrap(T flyweight) {
            flyweight.wrap(sendBuffer, sendBufferOffset);
            return flyweight;
        }

        public void send(int limit) {
            tcpHeaders.respond(sendBuffer, sendBufferOffset, limit - sendBufferOffset);
        }

        public void close(boolean immediately) {
            throw new UnsupportedOperationException();
            // TODO: tcpHeaders.closeConnection(id, immediately);
        }
    }
}