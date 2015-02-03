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

import org.kaazing.nuklei.function.Mikro;

import java.util.Map;
import java.util.Objects;

/**
 * Wrapper around a Mikro that is used for holding configuration, etc.
 */
public class MikroService
{
    private final String uri;
    private final Mikro mikro;
    private final LocalEndpointConfiguration localEndpointConfiguration;

    public MikroService(
        final String uri,
        final Mikro mikro,
        final Map<String, Object> configurationMap)
    {
        Objects.requireNonNull(configurationMap);

        this.uri = uri;
        this.mikro = mikro;

        localEndpointConfiguration = new LocalEndpointConfiguration(uri, configurationMap);
    }

    public String uri()
    {
        return uri;
    }

    public Mikro mikro()
    {
        return mikro;
    }

    public Map<String, Object> configurationMap()
    {
        return localEndpointConfiguration.configurationMap();
    }

    public LocalEndpointConfiguration localEndpointConfiguration()
    {
        return localEndpointConfiguration;
    }
}
