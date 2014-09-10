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
import org.kaazing.nuklei.kompound.cmd.StartCmd;
import org.kaazing.nuklei.kompound.cmd.StopCmd;
import org.kaazing.nuklei.net.TcpManager;
import org.kaazing.nuklei.net.TcpManagerProxy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for one or more Mikro services.
 */
public class Kompound implements AutoCloseable
{
    private static final int TCP_MANAGER_COMMAND_QUEUE_SIZE = 1024;
    private static final int TCP_MANAGER_SEND_BUFFER_SIZE = 64 * 1024 + MpscRingBuffer.STATE_TRAILER_SIZE;
    private static final int MIKRO_RECEIVE_BUFFER_SIZE = 64 * 1024 + MpscRingBuffer.STATE_TRAILER_SIZE;

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
     */
    public static void main(final String[] args) throws Exception
    {
        try (final Kompound theKompound = Kompound.startUp())
        {
            while (true)
            {
                Thread.sleep(1000); // actually see about grabbing SIGINT and graceful shutdown
            }
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
                mikroService.mikro().onCommand(stopCmd);
            });
    }

    private Kompound(final Builder builder)
    {
        serviceList = builder.serviceList;
        tcpManager = new TcpManager(managerCommandQueue, managerSendBuffer);
        tcpManagerProxy = new TcpManagerProxy(managerCommandQueue, managerSendBuffer);
        mikroLocator = new MikroLocator(serviceList);
        tcpManagerNuklei = new DedicatedNuklei("tcp-manager");
        mikroNuklei = new DedicatedNuklei("mikros");
        localEndpointManager = new LocalEndpointManager(tcpManagerProxy);

        tcpManager.launch(tcpManagerNuklei);

        final StartCmd startCmd = new StartCmd();

        serviceList.forEach(
            (mikroService) ->
            {
                startCmd.reset(
                    mikroLocator,
                    (typeId, buffer, offset, length) ->
                    {
                        // TODO: change TcpManagerProxy.send to take a typeId so that it is symmetric with recv/read
                        tcpManagerProxy.send(buffer, offset, length);
                        return true;
                    },
                    mikroService.configurationMap());
                // call onCommand() directly instead of going through a queue so it occurs ordered correctly
                mikroService.mikro().onCommand(startCmd);

                localEndpointManager.addEndpoint(mikroService);

                // spin up this mikro service now
                mikroNuklei.spinUp(mikroService.nukleus());
            });
    }

    public static class Builder
    {
        public ArrayList<MikroService> serviceList = new ArrayList<>();

        public Builder service(final String uri, final Mikro mikro)
        {
            return service(uri, mikro, new HashMap<>());
        }

        public Builder service(
            final String uri, final Mikro mikro, final Map<String, Object> configurationMap)
        {
            final AtomicBuffer receiveBuffer = new AtomicBuffer(ByteBuffer.allocateDirect(MIKRO_RECEIVE_BUFFER_SIZE));
            serviceList.add(new MikroService(uri, mikro, receiveBuffer, configurationMap));
            return this;
        }

        public Kompound build()
        {
            return new Kompound(this);
        }
    }
}
