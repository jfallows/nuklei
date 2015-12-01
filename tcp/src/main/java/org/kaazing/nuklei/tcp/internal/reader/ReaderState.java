/**
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
package org.kaazing.nuklei.tcp.internal.reader;

import static org.kaazing.nuklei.tcp.internal.types.stream.Types.TYPE_ID_BEGIN;
import static org.kaazing.nuklei.tcp.internal.types.stream.Types.TYPE_ID_DATA;
import static org.kaazing.nuklei.tcp.internal.types.stream.Types.TYPE_ID_END;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.kaazing.nuklei.tcp.internal.connector.ConnectorProxy;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataFW;
import org.kaazing.nuklei.tcp.internal.types.stream.EndFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public class ReaderState implements AutoCloseable
{
    private final BeginFW beginRO = new BeginFW();
    private final EndFW endRO = new EndFW();
    private final DataFW dataRO = new DataFW();

    private final ConnectorProxy.FromReader connectorProxy;
    private final Long2ObjectHashMap<ConnectingState> connectingStateByStreamId;
    private final Long2ObjectHashMap<StreamState> stateByStreamId;

    private final String handler;
    private final RingBuffer buffer;

    public ReaderState(
        ConnectorProxy.FromReader connectorProxy,
        String handler,
        RingBuffer buffer)
    {
        this.connectorProxy = connectorProxy;
        this.connectingStateByStreamId = new Long2ObjectHashMap<>();
        this.stateByStreamId = new Long2ObjectHashMap<>();

        this.handler = handler;
        this.buffer = buffer;
    }

    public String handler()
    {
        return this.handler;
    }

    public RingBuffer buffer()
    {
        return buffer;
    }

    public int process() throws Exception
    {
        int weight = 0;

        weight += buffer.read(this::handleRead);

        return weight;
    }

    @Override
    public void close()
    {
        stateByStreamId.values().forEach((state) -> {
            try
            {
                state.channel().shutdownOutput();
            }
            catch (final Exception ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
        });
    }

    public void doRegister(
        String handler,
        long handlerRef,
        long clientStreamId,
        long serverStreamId,
        SocketChannel channel)
    {
        if (serverStreamId == 0L)
        {
            ConnectingState connectingState = new ConnectingState(clientStreamId, buffer(), channel);
            connectingStateByStreamId.put(connectingState.streamId(), connectingState);
        }
        else
        {
            StreamState newState = new StreamState(clientStreamId, buffer(), channel);
            stateByStreamId.put(newState.streamId(), newState);
        }
    }

    @Override
    public String toString()
    {
        return String.format("[handler=%d]", handler());
    }

    private void handleRead(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case TYPE_ID_BEGIN:
            beginRO.wrap(buffer, index, index + length);

            final long streamId = beginRO.streamId();
            final long referenceId = beginRO.referenceId();

            if ((streamId & 0x0000000000000001L) != 0L)
            {
                final String handler = this.handler;
                final long handlerRef = referenceId;

                connectorProxy.doConnect(handler, handlerRef, streamId);
            }
            else
            {
                final long clientStreamId = referenceId;

                ConnectingState connectingState = connectingStateByStreamId.remove(clientStreamId);
                if (connectingState == null)
                {
                    throw new IllegalStateException("stream not found: " + streamId);
                }

                StreamState newState = new StreamState(streamId, connectingState.buffer(), connectingState.channel());
                stateByStreamId.put(newState.streamId(), newState);
            }
            break;

        case TYPE_ID_DATA:
            dataRO.wrap(buffer, index, index + length);

            StreamState state = stateByStreamId.get(dataRO.streamId());
            if (state == null)
            {
                throw new IllegalStateException("stream not found: " + dataRO.streamId());
            }

            try
            {
                SocketChannel channel = state.channel();
                ByteBuffer writeBuffer = state.writeBuffer();
                writeBuffer.limit(dataRO.limit());
                writeBuffer.position(dataRO.payloadOffset());

                // send buffer underlying buffer for read buffer
                final int totalBytes = writeBuffer.remaining();
                final int bytesWritten = channel.write(writeBuffer);

                if (bytesWritten < totalBytes)
                {
                    // TODO: support partial writes
                    throw new IllegalStateException("partial write: " + bytesWritten + "/" + totalBytes);
                }
            }
            catch (IOException ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
            break;

        case TYPE_ID_END:
            endRO.wrap(buffer, index, index + length);

            StreamState oldState = stateByStreamId.remove(endRO.streamId());
            if (oldState == null)
            {
                throw new IllegalStateException("stream not found: " + endRO.streamId());
            }

            try
            {
                SocketChannel channel = oldState.channel();
                channel.close();
            }
            catch (IOException ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
            break;
        }
    }

}
