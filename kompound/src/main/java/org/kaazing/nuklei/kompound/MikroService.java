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

package org.kaazing.nuklei.kompound;

import org.kaazing.nuklei.MessagingNukleus;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.RingBufferReader;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferReader;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferWriter;

import java.util.Map;

/**
 * Wrapper around a Mikro that is used for holding queues and buffers, etc.
 */
public class MikroService implements MpscRingBufferReader.ReadHandler
{
    private final static int MPSC_DEFAULT_READ_LIMIT = 100;

    private final String uri;
    private final Mikro mikro;
    private final AtomicBuffer receiveBuffer;
    private final MpscRingBufferWriter ringBufferWriter;
    private final Map<String, Object> configurationMap;
    private final MessagingNukleus nukleus;
    private final LocalEndpointConfiguration localEndpointConfiguration;

    public MikroService(
        final String uri,
        final Mikro mikro,
        final AtomicBuffer receiveBuffer,
        final Map<String, Object> configurationMap)
    {
        this.uri = uri;
        this.mikro = mikro;
        this.receiveBuffer = receiveBuffer;
        this.configurationMap = configurationMap;

        localEndpointConfiguration = new LocalEndpointConfiguration(uri, configurationMap);

        ringBufferWriter = new MpscRingBufferWriter(receiveBuffer);

        final MessagingNukleus.Builder builder = new MessagingNukleus.Builder()
            .mpscRingBuffer(receiveBuffer, this::onMessage, MPSC_DEFAULT_READ_LIMIT);

        nukleus = builder.build();
    }

    public String uri()
    {
        return uri;
    }

    public String scheme()
    {
        return localEndpointConfiguration.scheme();
    }

    public Mikro mikro()
    {
        return mikro;
    }

    public Map<String, Object> configurationMap()
    {
        return configurationMap;
    }

    public Proxy proxy()
    {
        return ringBufferWriter::write;
    }

    public AtomicBuffer receiveBuffer()
    {
        return receiveBuffer;
    }

    public Nukleus nukleus()
    {
        return nukleus;
    }

    public LocalEndpointConfiguration localEndpointConfiguration()
    {
        return localEndpointConfiguration;
    }

    public void onMessage(final int typeId, final AtomicBuffer buffer, final int offset, final int length)
    {
        mikro.onAvailable(typeId, buffer, offset, length);
    }
}
