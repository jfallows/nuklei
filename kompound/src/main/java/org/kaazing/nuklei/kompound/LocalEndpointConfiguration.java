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

import org.kaazing.nuklei.protocol.http.HttpDispatcher;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Endpoint configuration for a Mikro.
 *
 * This holds all the configuration options, etc. for composition
 */
public class LocalEndpointConfiguration
{
    private final Map<String, Object> configurationMap;
    private InetSocketAddress localAddress;
    private String scheme;
    private String schemeSpecificPart;
    private String userInfo;
    private String path;
    private String query;
    private String fragment;
    private LocalEndpoint.Type endpointType;
    private File file;
    private boolean requiresHttpDispatcher = false;

    public LocalEndpointConfiguration(final String uri, final Map<String, Object> configurationMap)
    {
        this.configurationMap = configurationMap;
        // TODO: TCP could set a name in the configurationMap to expose
        parseUri(uri, configurationMap);
    }

    public void parseUri(final String uriAsString, final Map<String, Object> configurationMap)
    {
        try
        {
            final URI uri = new URI(uriAsString);
            scheme = uri.getScheme();

            switch (scheme)
            {
                case "tcp":
                    handleUriAsTcp(uri, configurationMap);
                    break;
                case "http":
                    handleUriAsHttp(uri, configurationMap);
                    break;
                case "shmem":
                    handleUriAsShMem(uri, configurationMap);
                    break;
                default:
                    throw new IllegalArgumentException("unknown scheme: " + scheme);
            }
        }
        catch (final Exception ex)
        {
            localAddress = null;
            throw new IllegalArgumentException(ex);
        }
    }

    public Map<String, Object> configurationMap()
    {
        return configurationMap;
    }

    public InetSocketAddress localAddress()
    {
        return localAddress;
    }

    public File file()
    {
        return file;
    }

    public String scheme()
    {
        return scheme;
    }

    public String schemeSpecificPart()
    {
        return schemeSpecificPart;
    }

    public String userInfo()
    {
        return userInfo;
    }

    public String path()
    {
        return path;
    }

    public String method()
    {
        // TODO: config may change this
        return "GET";
    }

    public String query()
    {
        return query;
    }

    public String fragment()
    {
        return fragment;
    }

    public LocalEndpoint.Type endpointType()
    {
        return endpointType;
    }

    public boolean requiresHttpDispatcher()
    {
        return requiresHttpDispatcher;
    }

    public static boolean checkCompatibility(
        final LocalEndpointConfiguration lhs, final LocalEndpointConfiguration rhs)
    {
        // TODO: finish. If not compatible, throw IllegalArgumentException
        return true;
    }

    private void handleUriAsTcp(final URI uri, final Map<String, Object> configurationMap)
    {
        try
        {
            final String host = uri.getHost();
            final int port = uri.getPort();

            localAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            endpointType = LocalEndpoint.Type.TCP;
        }
        catch (final UnknownHostException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }

    private void handleUriAsHttp(final URI uri, final Map<String, Object> configurationMap)
    {
        requiresHttpDispatcher = true;
        userInfo = uri.getUserInfo();
        path = uri.getPath();
        query = uri.getQuery();
        fragment = uri.getFragment();

        // TODO: if configurationMap specifies this runs over something else, then call that. If not, assume TCP.
        handleUriAsTcp(uri, configurationMap);
    }

    private void handleUriAsShMem(final URI uri, final Map<String, Object> configurationMap)
    {
        schemeSpecificPart = uri.getSchemeSpecificPart();
        file = new File(schemeSpecificPart);
        endpointType = LocalEndpoint.Type.SHMEM;
    }
}
