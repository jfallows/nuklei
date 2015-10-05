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
package org.kaazing.nuklei.protocol.http;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.kaazing.nuklei.function.Mikro;
import org.kaazing.nuklei.protocol.Coordinates;
import org.kaazing.nuklei.protocol.ProtocolUtil;
import org.kaazing.nuklei.protocol.tcp.TcpManagerHeadersDecoder;
import org.kaazing.nuklei.protocol.tcp.TcpManagerTypeId;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 */
public class HttpDispatcher implements Mikro
{
    private static final int FREE_LIST_SIZE = 16;

    // TODO: replace with unboxed map
    private final Map<Long, HttpHeadersDecoder> decoderByConnectionId = new HashMap<>();
    private final Queue<HttpHeadersDecoder> decoderFreeList = new ArrayDeque<>();
    private final List<DispatchResource> resourceList = new ArrayList<>();
    private final Supplier<HttpHeadersDecoder> decoderSupplier;

    public HttpDispatcher()
    {
        IntStream.range(0, FREE_LIST_SIZE).forEach(
            (i) ->
            {
                decoderFreeList.offer(new HttpHeadersDecoder(Coordinates::new));
            });

        decoderSupplier = this::grabDecoderFromFreeListIfPossible;
    }

    public HttpDispatcher addResource(final byte[] method, final byte[] path, final Mikro handler)
    {
        final DispatchResource resource = new DispatchResource(method, path, handler);

        resourceList.add(resource);
        return this;
    }

    public void onMessage(
        final Object header, final int typeId, final DirectBuffer buffer, final int offset, final int length)
    {
        if (TcpManagerTypeId.RECEIVED_DATA == typeId)
        {
            final TcpManagerHeadersDecoder tcpManagerHeadersDecoder = (TcpManagerHeadersDecoder) header;
            final HttpHeadersDecoder decoder = getOrAddDecoder(tcpManagerHeadersDecoder.connectionId());

            /*
             * TODO: can have decoder smart enough to know of WS or use different decoder for WS per connectionId
             */

            decoder.onMessage(header, typeId, buffer, offset, length);

            if (decoder.isDecoded())
            {
                dispatch(decoder);
            }
        }
    }

    private HttpHeadersDecoder getOrAddDecoder(final long connectionId)
    {
        HttpHeadersDecoder decoder = decoderByConnectionId.get(connectionId);

        if (null == decoder)
        {
            decoder = decoderSupplier.get();
            decoderByConnectionId.put(connectionId, decoder);
        }

        return decoder;
    }

    private HttpHeadersDecoder grabDecoderFromFreeListIfPossible()
    {
        HttpHeadersDecoder decoder = decoderFreeList.poll();

        if (null == decoder)
        {
            decoder = new HttpHeadersDecoder(Coordinates::new);
        }

        return decoder;
    }

    private void freeDecoder(final HttpHeadersDecoder decoder)
    {
        if (FREE_LIST_SIZE > decoderFreeList.size())
        {
            decoderFreeList.offer(decoder);
        }
    }

    private void dispatch(final HttpHeadersDecoder decoder)
    {
        // TODO: change from linear search
        for (int i = resourceList.size() - 1; i >= 0; i--)
        {
            final DispatchResource resource = resourceList.get(i);

            if (match(decoder, HttpHeaderName.PATH, resource.path) &&
                match(decoder, HttpHeaderName.METHOD, resource.method))
            {
                resource.handler.onMessage(
                    decoder,
                    TcpManagerTypeId.RECEIVED_DATA,
                    decoder.buffer(),
                    decoder.cursor(),
                    decoder.limit() - decoder.cursor());
                return;
            }
        }

        // TODO: 404 territory!
    }

    private static boolean match(
        final HttpHeadersDecoder decoder, final HttpHeaderName name, final DirectBuffer buffer)
    {
        final Coordinates coordinates = decoder.header(name);

        return (coordinates.length() == buffer.capacity() &&
            ProtocolUtil.compareMemory(
                decoder.buffer(), decoder.offset() + coordinates.offset(), buffer, 0, coordinates.length()));
    }

    private static class DispatchResource
    {
        private final DirectBuffer method;
        private final DirectBuffer path;
        private final Mikro handler;

        public DispatchResource(final byte[] method, final byte[] path, final Mikro handler)
        {
            this.method = new UnsafeBuffer(method);
            this.path = new UnsafeBuffer(path);
            this.handler = handler;
        }
    }
}
