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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.kaazing.nuklei.MessagingNukleus;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferReader;
import org.kaazing.nuklei.function.Mikro;
import org.kaazing.nuklei.function.Proxy;
import org.kaazing.nuklei.protocol.http.HttpDispatcher;
import org.kaazing.nuklei.protocol.tcp.TcpManagerHeadersDecoder;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * Local endpoint for set of services
 *
 * Can be either a TCP endpoint or a File/shared-memory endpoint
 */
public class LocalEndpoint
{
    private static final int MPSC_DEFAULT_READ_LIMIT = 100;

    public enum Type
    {
        TCP, SHMEM
    }

    private final List<MikroService> serviceList = new ArrayList<>();
    private final MessagingNukleus nukleus;
    private final AtomicBuffer buffer;
    private final TcpManagerHeadersDecoder tcpManagerHeadersDecoder;
    private final HttpDispatcher httpDispatcher;
    private final LocalEndpointConfiguration initialConfiguration;

    private Proxy tcpManagerProxy = null;

    private Mikro tcpMikro;

    public LocalEndpoint(final LocalEndpointConfiguration localEndpointConfiguration)
    {
        initialConfiguration = localEndpointConfiguration;

        if (null != localEndpointConfiguration.file())
        {
            // TODO: use IoUtils from Agrona to create empty file, map, etc. and create AtomicBuffer
            buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(Kompound.MIKRO_RECEIVE_BUFFER_SIZE));
        }
        else
        {
            buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(Kompound.MIKRO_RECEIVE_BUFFER_SIZE));
        }

        tcpManagerHeadersDecoder = new TcpManagerHeadersDecoder(ByteOrder.nativeOrder());

        MpscRingBufferReader.ReadHandler handler;

        if (localEndpointConfiguration.requiresHttpDispatcher())
        {
            httpDispatcher = new HttpDispatcher();
            handler = this::onHttpMessage;
        }
        else
        {
            httpDispatcher = null;
            handler = this::onTcpMessage;
        }

        final MessagingNukleus.Builder builder = new MessagingNukleus.Builder()
            .mpscRingBuffer(buffer, handler, MPSC_DEFAULT_READ_LIMIT);

        nukleus = builder.build();
    }

    public void add(final MikroService mikroService)
    {
        final LocalEndpointConfiguration localEndpointConfiguration = mikroService.localEndpointConfiguration();

        LocalEndpointConfiguration.checkCompatibility(initialConfiguration, localEndpointConfiguration);

        if (localEndpointConfiguration.requiresHttpDispatcher())
        {
            httpDispatcher.addResource(
                localEndpointConfiguration.method().getBytes(),
                localEndpointConfiguration.path().getBytes(),
                mikroService.mikro());
        }
        else
        {
            tcpMikro = mikroService.mikro();
        }

        serviceList.add(mikroService);
    }

    public AtomicBuffer buffer()
    {
        return buffer;
    }

    public Nukleus nukleus()
    {
        return nukleus;
    }

    public LocalEndpointConfiguration initialConfiguration()
    {
        return initialConfiguration;
    }

    public void tcpManagerProxy(final Proxy tcpManagerProxy)
    {
        this.tcpManagerProxy = tcpManagerProxy;
        tcpManagerHeadersDecoder.tcpManagerProxy(tcpManagerProxy);
    }

    private void onTcpMessage(final int typeId, final DirectBuffer buffer, final int offset, final int length)
    {
        tcpManagerHeadersDecoder.wrap(buffer, offset);
        tcpManagerHeadersDecoder.tcpManagerProxy(tcpManagerProxy);
        tcpMikro.onMessage(
            tcpManagerHeadersDecoder,
            typeId,
            buffer,
            offset + tcpManagerHeadersDecoder.length(),
            length - tcpManagerHeadersDecoder.length());
    }

    private void onHttpMessage(final int typeId, final DirectBuffer buffer, final int offset, final int length)
    {
        tcpManagerHeadersDecoder.wrap(buffer, offset);
        httpDispatcher.onMessage(
            tcpManagerHeadersDecoder,
            typeId,
            buffer,
            offset + tcpManagerHeadersDecoder.length(),
            length - tcpManagerHeadersDecoder.length());
    }
}
