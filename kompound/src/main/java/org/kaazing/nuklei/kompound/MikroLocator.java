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

import org.kaazing.nuklei.function.Proxy;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Locator for services by name and URI
 *
 * Mikro's are added as a batch and treated as immutable. Access to them is NOT protected,
 * but will be from multiple threads.
 */
public class MikroLocator
{
    private final HashMap<String, MikroService> mikroByUriMap = new HashMap<>();

    public MikroLocator(final ArrayList<MikroService> localServiceList)
    {
        localServiceList.forEach((mikroService) -> mikroByUriMap.put(mikroService.uri(), mikroService));
    }

    public Proxy resolve(final String uri)
    {
        final MikroService service = mikroByUriMap.get(uri);

        if (null != service)
        {
            // TODO: replace with better
//            return service.proxy();
            return null;
        }
        else
        {
            // TODO: do a connect to URI, etc. Need to cache it and handle its lifetime
            return null;
        }
    }
}
