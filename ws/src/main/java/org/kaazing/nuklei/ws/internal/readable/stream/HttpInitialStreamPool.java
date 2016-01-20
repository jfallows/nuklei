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
package org.kaazing.nuklei.ws.internal.readable.stream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_BEGIN;
import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_DATA;
import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_END;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.kaazing.nuklei.ws.internal.readable.ReadableProxy;
import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsFrameFW;
import org.kaazing.nuklei.ws.internal.util.BufferUtil;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class HttpInitialStreamPool
{
    private final HttpBeginFW httpBeginRO = new HttpBeginFW();
    private final HttpDataFW httpDataRO = new HttpDataFW();
    private final HttpEndFW httpEndRO = new HttpEndFW();

    private final HttpBeginFW.Builder httpBeginRW = new HttpBeginFW.Builder();
    private final HttpEndFW.Builder httpEndRW = new HttpEndFW.Builder();

    private final WsBeginFW.Builder wsBeginRW = new WsBeginFW.Builder();
    private final WsDataFW.Builder wsDataRW = new WsDataFW.Builder();
    private final WsEndFW.Builder wsEndRW = new WsEndFW.Builder();

    private final WsFrameFW wsFrameRO = new WsFrameFW();

    private final AtomicBuffer atomicBuffer;
    private final AtomicCounter streamsAccepted;

    public HttpInitialStreamPool(
        int capacity,
        AtomicBuffer atomicBuffer,
        AtomicCounter streamsAccepted)
    {
        this.atomicBuffer = atomicBuffer;
        this.streamsAccepted = streamsAccepted;
    }

    public MessageHandler acquire(
        long destinationRef,
        long sourceReplyStreamId,
        ReadableProxy destination,
        RingBuffer sourceRoute,
        RingBuffer destinationRoute,
        Consumer<MessageHandler> released)
    {
        return new HttpInitialStream(released, destinationRef, sourceReplyStreamId,
                                     destination, sourceRoute, destinationRoute);
    }

    private final class HttpInitialStream implements MessageHandler
    {
        private final Consumer<MessageHandler> cleanup;
        private final long destinationRef;
        private final long sourceReplyStreamId;
        private final ReadableProxy destination;
        private final RingBuffer sourceRoute;
        private final RingBuffer destinationRoute;

        private long destinationInitialStreamId;

        public HttpInitialStream(
            Consumer<MessageHandler> cleanup,
            long destinationRef,
            long sourceReplyStreamId,
            ReadableProxy destination,
            RingBuffer sourceRoute,
            RingBuffer destinationRoute)
        {
            this.cleanup = cleanup;
            this.destinationRef = destinationRef;
            this.sourceReplyStreamId = sourceReplyStreamId;
            this.destination = destination;
            this.sourceRoute = sourceRoute;
            this.destinationRoute = destinationRoute;
        }

        @Override
        public void onMessage(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case TYPE_ID_BEGIN:
                onHttpBegin(buffer, index, length);
                break;
            case TYPE_ID_DATA:
                onHttpData(buffer, index, length);
                break;
            case TYPE_ID_END:
                onHttpEnd(buffer, index, length);
                break;
            }
        }

        private void onHttpBegin(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpBeginRO.wrap(buffer, index, index + length);

            final long sourceInitialStreamId = httpBeginRO.streamId();

            // TODO: need lightweight approach (start)
            final Map<String, String> headers = new LinkedHashMap<>();
            httpBeginRO.headers().forEach((header) ->
            {
                headers.put(header.name().asString(), header.value().asString());
            });

            String version = headers.get("sec-websocket-version");
            String key = headers.get("sec-websocket-key");
            String protocol = headers.get("sec-websocket-protocol");
            // TODO: need lightweight approach (end)

            if (!"13".equals(version) || key == null)
            {
                HttpBeginFW httpBegin = httpBeginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                                   .streamId(sourceReplyStreamId)
                                                   .referenceId(sourceInitialStreamId)
                                                   .header(":status", "500")
                                                   .build();

                if (!sourceRoute.write(httpBegin.typeId(), httpBegin.buffer(), httpBegin.offset(), httpBegin.length()))
                {
                     throw new IllegalStateException("could not write to ring buffer");
                }

                HttpEndFW httpEnd = httpEndRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                             .streamId(sourceReplyStreamId)
                                             .build();

                if (!sourceRoute.write(httpEnd.typeId(), httpEnd.buffer(), httpEnd.offset(), httpEnd.length()))
                {
                     throw new IllegalStateException("could not write to ring buffer");
                }
            }
            else
            {
                byte[] handshakeKey = key.getBytes(US_ASCII);

                // positive, odd destination stream id
                this.destinationInitialStreamId = (streamsAccepted.increment() << 1L) | 0x0000000000000001L;

                // TODO: include key to compute Sec-WebSocket-Accept handshake response header
                destination.doRegisterEncoder(destinationInitialStreamId, sourceInitialStreamId,
                                              sourceReplyStreamId, sourceRoute, handshakeKey);

                final WsBeginFW wsBegin = wsBeginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                                   .streamId(destinationInitialStreamId)
                                                   .referenceId(destinationRef)
                                                   .protocol(protocol)
                                                   .build();

                if (!destinationRoute.write(wsBegin.typeId(), wsBegin.buffer(), wsBegin.offset(), wsBegin.length()))
                {
                    throw new IllegalStateException("could not write to ring buffer");
                }
            }
        }

        private void onHttpData(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpDataRO.wrap(buffer, index, index + length);

            DirectBuffer payload = httpDataRO.payload();

            int nextPayloadOffset = 0;
            int maxPayloadLimit = payload.capacity();
            while (nextPayloadOffset < maxPayloadLimit)
            {
                wsFrameRO.wrap(payload, nextPayloadOffset, maxPayloadLimit);
                nextPayloadOffset = wsFrameRO.limit();

                if (!wsFrameRO.mask())
                {
                    byte[] bytes = new byte[payload.capacity()];
                    payload.getBytes(0, bytes);
                    throw new IllegalStateException("client frame not masked: " + BitUtil.toHex(bytes));
                }

                switch (wsFrameRO.opcode())
                {
                case 1: // TEXT
                case 2: // BINARY
                    // TODO: binary versus text in WsDataFW metadata
                    final WsDataFW wsData = wsDataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                                    .streamId(destinationInitialStreamId)
                                                    .payload(wsFrameRO.payload())
                                                    .build();

                    BufferUtil.xor(atomicBuffer, wsData.payloadOffset(), wsData.payloadLength(), wsFrameRO.maskingKey());

                    if (!destinationRoute.write(wsData.typeId(), wsData.buffer(), wsData.offset(), wsData.length()))
                    {
                        throw new IllegalStateException("could not write to ring buffer");
                    }
                    break;

                case 8: // CLOSE
                    final WsEndFW wsEnd = wsEndRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                                 .streamId(destinationInitialStreamId)
                                                 .build();

                    if (!destinationRoute.write(wsEnd.typeId(), wsEnd.buffer(), wsEnd.offset(), wsEnd.length()))
                    {
                        throw new IllegalStateException("could not write to ring buffer");
                    }
                    break;

                default:
                    throw new IllegalStateException("not yet implemented");
                }
            }
        }

        private void onHttpEnd(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            httpEndRO.wrap(buffer, index, index + length);

            // TODO: wsReset if necessary?

            cleanup.accept(this);
        }
    }
}
