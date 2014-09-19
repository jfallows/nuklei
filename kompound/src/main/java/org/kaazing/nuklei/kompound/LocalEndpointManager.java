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

import org.kaazing.nuklei.net.TcpManagerProxy;
import org.kaazing.nuklei.protocol.http.HttpDispatcher;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Local endpoint manager for Kompound
 */
public class LocalEndpointManager
{
    private final TcpManagerProxy tcpManagerProxy;
    private final Map<InetSocketAddress, List<MikroService>> mikroByAddressMap = new HashMap<>();
    private final Map<InetSocketAddress, HttpDispatcher> httpDispatcherByAddressMap = new HashMap<>();

    public LocalEndpointManager(final TcpManagerProxy tcpManagerProxy)
    {
        this.tcpManagerProxy = tcpManagerProxy;
    }

    public void addEndpoint(final MikroService mikroService)
    {
        final LocalEndpointConfiguration localEndpointConfiguration = mikroService.localEndpointConfiguration();

        List<MikroService> servicesOnAddress = getOrAddAddress(localEndpointConfiguration.localAddress(), mikroService);

        servicesOnAddress.add(mikroService);
    }

    public List<MikroService> servicesOnAddress(final InetSocketAddress address)
    {
        return mikroByAddressMap.get(address);
    }

    public HttpDispatcher httpDispatcherOnAddress(final InetSocketAddress address)
    {
        return httpDispatcherByAddressMap.get(address);
    }

    public void forEach(final BiConsumer<InetSocketAddress, MikroService> consumer)
    {
        mikroByAddressMap.forEach(
            (address, list) ->
                list.forEach(
                    (mikroService) -> consumer.accept(address, mikroService)));
    }

    private List<MikroService> getOrAddAddress(final InetSocketAddress address, final MikroService mikroService)
    {
        List<MikroService> services = mikroByAddressMap.get(address);

        if (null != services)
        {
            return services;
        }

        InetAddress[] interfaces = new InetAddress[1];
        interfaces[0] = address.getAddress();
        tcpManagerProxy.attach(address.getPort(), interfaces, mikroService.receiveBuffer());

        services = new ArrayList<>();
        mikroByAddressMap.put(address, services);

        if ("http".equals(mikroService.scheme()))
        {
            httpDispatcherByAddressMap.put(address, new HttpDispatcher());
        }

        return services;
    }
}
