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
import org.kaazing.nuklei.net.TcpManagerProxy;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Local endpoint manager for Kompound
 */
public class LocalEndpointManager
{
    private final Map<InetSocketAddress, LocalEndpoint> endpointByAddressMap = new HashMap<>();
    private final Map<File, LocalEndpoint> endpointByFileMap = new HashMap<>();

    public void addEndpoint(final MikroService mikroService)
    {
        final LocalEndpointConfiguration localEndpointConfiguration = mikroService.localEndpointConfiguration();
        final LocalEndpoint.Type endpointType = localEndpointConfiguration.endpointType();
        final InetSocketAddress address = localEndpointConfiguration.localAddress();
        final File file = localEndpointConfiguration.file();

        LocalEndpoint localEndpoint = null;
        if (LocalEndpoint.Type.TCP == endpointType)
        {
            localEndpoint = getOrAddLocalEndpoint(endpointByAddressMap, address, localEndpointConfiguration);

            // may also add to file map if configuration specifies a File also
            if (null != file)
            {
                final LocalEndpoint existingEndpoint = endpointByFileMap.get(file);
                if (null != existingEndpoint && localEndpoint != existingEndpoint)
                {
                    throw new IllegalArgumentException("mikro specifies an existing file, but not same endpoint");
                }
                else if (null == existingEndpoint)
                {
                    endpointByFileMap.put(file, localEndpoint);
                }
            }
        }
        else if (LocalEndpoint.Type.SHMEM == endpointType)
        {
            localEndpoint = getOrAddLocalEndpoint(endpointByFileMap, file, localEndpointConfiguration);
        }

        Objects.requireNonNull(localEndpoint);
        localEndpoint.add(mikroService);
    }

    public void doLocalAttaches(final TcpManagerProxy tcpManagerProxy)
    {
        endpointByAddressMap.forEach(
            (address, localEndpoint) ->
            {
                InetAddress[] interfaces = new InetAddress[1];
                interfaces[0] = address.getAddress();
                tcpManagerProxy.attach(address.getPort(), interfaces, localEndpoint.buffer());
            });
    }

    public void doSpinUp(final DedicatedNuklei nuklei)
    {
        endpointByAddressMap.forEach((address, localEndpoint) -> nuklei.spinUp(localEndpoint.nukleus()));

        // if strictly file, then spin it up. If not, then already spun up
        endpointByFileMap.forEach(
            (file, localEndpoint) ->
            {
                if (localEndpoint.initialConfiguration().endpointType() == LocalEndpoint.Type.SHMEM)
                {
                    nuklei.spinUp(localEndpoint.nukleus());
                }
            }
        );
    }

    private static <T> LocalEndpoint getOrAddLocalEndpoint(
        final Map<T, LocalEndpoint> map, final T key, final LocalEndpointConfiguration localEndpointConfiguration)
    {
        LocalEndpoint localEndpoint = map.get(key);

        if (null == localEndpoint)
        {
            localEndpoint = new LocalEndpoint(localEndpointConfiguration);
            map.put(key, localEndpoint);
        }

        return localEndpoint;
    }
}
