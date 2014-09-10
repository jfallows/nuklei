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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

/**
 * Endpoint configuration for a Mikro.
 */
public class LocalEndpointConfiguration
{
    private InetSocketAddress localAddress;
    private String scheme;
    private String userInfo;
    private String path;
    private String query;
    private String fragment;

    public LocalEndpointConfiguration(final String uri, final Map<String, Object> configurationMap)
    {
        parseUri(uri);
    }

    public void parseUri(final String uriAsString)
    {
        try
        {
            final URI uri = new URI(uriAsString);
            scheme = uri.getScheme();

            if ("tcp".equals(scheme))
            {
                handleUriAsTcp(uri);
            }
            else if ("http".equals(scheme))
            {
                handleUriAsHttp(uri);
            }
        }
        catch (final Exception ex)
        {
            localAddress = null;
            throw new IllegalArgumentException(ex);
        }
    }

    public InetSocketAddress localAddress()
    {
        return localAddress;
    }

    public String scheme()
    {
        return scheme;
    }

    public String userInfo()
    {
        return userInfo;
    }

    public String path()
    {
        return path;
    }

    public String query()
    {
        return query;
    }

    public String fragment()
    {
        return fragment;
    }

    private void handleUriAsTcp(final URI uri) throws Exception
    {
        final String host = uri.getHost();
        final int port = uri.getPort();

        localAddress = new InetSocketAddress(InetAddress.getByName(host), port);
    }

    private void handleUriAsHttp(final URI uri) throws Exception
    {
        userInfo = uri.getUserInfo();
        path = uri.getPath();
        query = uri.getQuery();
        fragment = uri.getFragment();

        handleUriAsTcp(uri);
    }
}
