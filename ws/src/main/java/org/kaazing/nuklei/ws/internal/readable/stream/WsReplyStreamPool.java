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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_BEGIN;
import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_DATA;
import static org.kaazing.nuklei.ws.internal.types.stream.Types.TYPE_ID_END;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.function.Consumer;

import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsFrameFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class WsReplyStreamPool
{
    private static final byte[] HANDSHAKE_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(UTF_8);

    private final MessageDigest sha1 = initSHA1();

    private final WsBeginFW wsBeginRO = new WsBeginFW();
    private final WsDataFW wsDataRO = new WsDataFW();
    private final WsEndFW wsEndRO = new WsEndFW();

    private final HttpBeginFW.Builder httpBeginRW = new HttpBeginFW.Builder();
    private final HttpDataFW.Builder httpDataRW = new HttpDataFW.Builder();

    private final WsFrameFW.Builder wsFrameRW = new WsFrameFW.Builder();

    private final AtomicBuffer atomicBuffer;

    public WsReplyStreamPool(
        int capacity,
        AtomicBuffer atomicBuffer)
    {
        this.atomicBuffer = atomicBuffer;
    }

    public MessageHandler acquire(
        long sourceInitialStreamId,
        long sourceReplyStreamId,
        RingBuffer sourceRoute,
        byte[] handshakeKey, Consumer<MessageHandler> released)
    {
        return new WsReplyStream(released, sourceInitialStreamId, sourceReplyStreamId, sourceRoute, handshakeKey);
    }

    private final class WsReplyStream implements MessageHandler
    {
        private final Consumer<MessageHandler> cleanup;
        private final long sourceInitialStreamId;
        private final long sourceReplyStreamId;
        private final RingBuffer sourceRoute;
        private final byte[] handshakeKey;

        public WsReplyStream(
            Consumer<MessageHandler> cleanup,
            long sourceInitialStreamId,
            long sourceReplyStreamId,
            RingBuffer sourceRoute,
            byte[] handshakeKey)
        {
            this.cleanup = cleanup;
            this.sourceInitialStreamId = sourceInitialStreamId;
            this.sourceReplyStreamId = sourceReplyStreamId;
            this.sourceRoute = sourceRoute;
            this.handshakeKey = handshakeKey;
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
                onBegin(buffer, index, length);
                break;
            case TYPE_ID_DATA:
                onData(buffer, index, length);
                break;
            case TYPE_ID_END:
                onEnd(buffer, index, length);
                break;
            }
        }

        private void onBegin(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            wsBeginRO.wrap(buffer, index, index + length);

            sha1.reset();
            sha1.update(handshakeKey);
            byte[] digest = sha1.digest(HANDSHAKE_GUID);
            byte[] handshakeHash = Base64.getEncoder().encode(digest);

            HttpBeginFW httpBegin = httpBeginRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                               .streamId(sourceReplyStreamId)
                                               .referenceId(sourceInitialStreamId)
                                               .header(":status", "101")
                                               .header("upgrade", "websocket")
                                               .header("connection", "upgrade")
                                               .header("sec-websocket-accept", new String(handshakeHash, US_ASCII))
                                               .header("sec-websocket-protocol", wsBeginRO.protocol().asString())
                                               .build();

            if (!sourceRoute.write(httpBegin.typeId(), httpBegin.buffer(), httpBegin.offset(), httpBegin.length()))
            {
                 throw new IllegalStateException("could not write to ring buffer");
            }
        }

        private void onData(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            wsDataRO.wrap(buffer, index, index + length);

            // TODO: combine httpDataRW with wsFrameRW
            httpDataRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                      .streamId(sourceReplyStreamId);

            WsFrameFW wsFrame = wsFrameRW.wrap(atomicBuffer, httpDataRW.payloadOffset(), atomicBuffer.capacity())
                                         .flagsAndOpcode(0x82) // TODO: via WsData metadata
                                         .payload(wsDataRO.buffer(), wsDataRO.payloadOffset(), wsDataRO.payloadLength())
                                         .build();

            HttpDataFW httpData = httpDataRW.payload(wsFrame.buffer(), wsFrame.offset(), wsFrame.length())
                                            .build();

            if (!sourceRoute.write(httpData.typeId(), httpData.buffer(), httpData.offset(), httpData.length()))
            {
                 throw new IllegalStateException("could not write to ring buffer");
            }
        }

        private void onEnd(
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            wsEndRO.wrap(buffer, index, index + length);

            // TODO

            cleanup.accept(this);
        }
    }

    private static MessageDigest initSHA1()
    {
        try
        {
            return MessageDigest.getInstance("SHA-1");
        }
        catch (Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
            return null;
        }
    }
}
