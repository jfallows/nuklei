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

import org.kaazing.nuklei.DedicatedNuklei;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBuffer;
import org.kaazing.nuklei.function.Mikro;
import org.kaazing.nuklei.kompound.cmd.StartCmd;
import org.kaazing.nuklei.kompound.cmd.StopCmd;
import org.kaazing.nuklei.net.TcpManager;
import org.kaazing.nuklei.net.TcpManagerProxy;
import org.kaazing.nuklei.net.TcpManagerTypeId;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for one or more Mikro services.
 */
public class Kompound implements AutoCloseable
{
    public static final int TCP_MANAGER_COMMAND_QUEUE_SIZE = 1024;
    public static final int TCP_MANAGER_SEND_BUFFER_SIZE = 64 * 1024 + MpscRingBuffer.STATE_TRAILER_SIZE;
    public static final int MIKRO_RECEIVE_BUFFER_SIZE = 64 * 1024 + MpscRingBuffer.STATE_TRAILER_SIZE;

    private static final AtomicBuffer NULL_BUFFER = new AtomicBuffer(new byte[0]);

    private final MpscArrayBuffer<Object> managerCommandQueue = new MpscArrayBuffer<>(TCP_MANAGER_COMMAND_QUEUE_SIZE);
    private final AtomicBuffer managerSendBuffer = new AtomicBuffer(ByteBuffer.allocate(TCP_MANAGER_SEND_BUFFER_SIZE));
    private final TcpManager tcpManager;
    private final TcpManagerProxy tcpManagerProxy;
    private final MikroLocator mikroLocator;
    private final DedicatedNuklei tcpManagerNuklei;
    private final DedicatedNuklei mikroNuklei;
    private final ArrayList<MikroService> serviceList;
    private final LocalEndpointManager localEndpointManager;

    /**
     * Start a Kompound as a standalone process.
     *
     * @param args command line arguments
     * @throws Exception if error on setup
     *
     */
    public static void main(final String[] args) throws Exception
    {
        try (final Kompound theKompound = Kompound.startUp(args))
        {
            while (true)
            {
                Thread.sleep(1000); // TODO: actually see about grabbing SIGINT and graceful shutdown
            }
        }
    }

    public static Kompound startUp(final String[] args)
    {
        try
        {
            final Builder builder = new Builder();

            for (int i = 0; i < args.length; i += 2)
            {
                final Object service = Class.forName(args[i + 1]).newInstance();

                if (service instanceof Mikro)
                {
                    builder.service(args[i], (Mikro) service);
                }
            }

            return builder.build();
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public static Kompound startUp()
    {
        // TODO: create new builder, read from YAML file which fills in builder, then create Kompound and return it
        return null;
    }

    public static Kompound startUp(final Builder builder)
    {
        return builder.build();
    }

    public void close() throws Exception
    {
        if (null != tcpManagerNuklei)
        {
            tcpManagerNuklei.stop();
        }

        if (null != mikroNuklei)
        {
            mikroNuklei.stop();
        }

        if (null != tcpManager)
        {
            tcpManager.close();
        }

        final StopCmd stopCmd = new StopCmd();
        // inform mikros of shutdown now that everything is all shutdown and stopped
        serviceList.forEach(
            (mikroService) ->
            {
                mikroService.mikro().onMessage(stopCmd, TcpManagerTypeId.NONE, NULL_BUFFER, 0, NULL_BUFFER.capacity());
            });
    }

    private Kompound(final Builder builder)
    {
        serviceList = builder.serviceList;
        localEndpointManager = new LocalEndpointManager();
        mikroNuklei = new DedicatedNuklei("mikros");
        mikroLocator = new MikroLocator(serviceList);

        if (builder.numTcpServices > 0)
        {
            tcpManager = new TcpManager(managerCommandQueue, managerSendBuffer);
            tcpManagerProxy = new TcpManagerProxy(managerCommandQueue, managerSendBuffer);
            tcpManagerNuklei = new DedicatedNuklei("tcp-manager");

            tcpManager.launch(tcpManagerNuklei);
        }
        else
        {
            tcpManager = null;
            tcpManagerProxy = null;
            tcpManagerNuklei = null;
        }

        final StartCmd startCmd = new StartCmd();

        // add endpoints, which might do checks.
        serviceList.forEach(localEndpointManager::addEndpoint);

        // reach here then configuration and URIs, etc. should all be good. So, inform mikros of start.
        serviceList.forEach(
            (mikroService) ->
            {
                startCmd.reset(mikroLocator, mikroService.configurationMap());

                // call directly instead of going through a queue so it occurs ordered correctly
                mikroService.mikro().onMessage(startCmd, TcpManagerTypeId.NONE, NULL_BUFFER, 0, NULL_BUFFER.capacity());
            });

        // do attaches
        localEndpointManager.doLocalAttaches(tcpManagerProxy);

        localEndpointManager.doSpinUp(mikroNuklei);
    }

    public static class Builder
    {
        public ArrayList<MikroService> serviceList = new ArrayList<>();
        public int numTcpServices = 0;

        public Builder service(final String uri, final Mikro mikro)
        {
            return service(uri, mikro, new HashMap<>());
        }

        public Builder service(
            final String uri, final Mikro mikro, final Map<String, Object> configurationMap)
        {
            final MikroService mikroService = new MikroService(uri, mikro, configurationMap);
            serviceList.add(mikroService);

            if (mikroService.localEndpointConfiguration().endpointType() == LocalEndpoint.Type.TCP)
            {
                numTcpServices++;
            }

            return this;
        }

        public Kompound build()
        {
            return new Kompound(this);
        }
    }
}
