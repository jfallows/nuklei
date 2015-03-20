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

package org.kaazing.nuklei.protocol.tcp;

import java.nio.ByteOrder;

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.function.Proxy;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public class TcpManagerHeadersDecoder extends Flyweight
{
    public static final int HEADER_LENGTH = BitUtil.SIZE_OF_LONG;

    private Proxy tcpManagerProxy;

    public TcpManagerHeadersDecoder(final ByteOrder byteOrder)
    {
        super(byteOrder);
    }

    /**
     * Connection ID for the data.
     *
     * @return connection ID
     */
    public long connectionId()
    {
        return buffer().getLong(offset());
    }

    /**
     * Length of the header.
     *
     * @return length of the header in bytes
     */
    public int length()
    {
        return HEADER_LENGTH;
    }

    public void tcpManagerProxy(final Proxy tcpManagerProxy)
    {
        this.tcpManagerProxy = tcpManagerProxy;
    }

    /**
     * Respond to an event with the given buffer contents.
     *
     * @param buffer to respond with
     * @param offset within the buffer to start the response from
     * @param length of the response in bytes
     */
    public void respond(final MutableDirectBuffer buffer, final int offset, final int length)
    {
        write(connectionId(), buffer, offset, length);
    }

    /**
     * write an event with the given buffer contents.
     *
     * @param connectionId target connection
     * @param buffer to respond with
     * @param offset within the buffer to start the response from
     * @param length of the response in bytes
     */
    public void write(final long connectionId, final MutableDirectBuffer buffer, final int offset, final int length)
    {
        if (HEADER_LENGTH > offset)
        {
            throw new IllegalArgumentException(
                "must leave enough room at start of buffer for header: " + HEADER_LENGTH);
        }

        buffer.putLong(offset - HEADER_LENGTH, connectionId);
        tcpManagerProxy.write(TcpManagerTypeId.SEND_DATA, buffer, offset - HEADER_LENGTH, length + HEADER_LENGTH);
    }

    public Flyweight wrap(final DirectBuffer buffer, final int offset)
    {
        return super.wrap(buffer, offset, false);
    }
}
