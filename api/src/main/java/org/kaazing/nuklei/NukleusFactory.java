/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.ServiceLoader.load;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class NukleusFactory
{
    public static NukleusFactory instantiate()
    {
        return instantiate(load(NukleusFactorySpi.class));
    }

    public static NukleusFactory instantiate(ClassLoader classLoader)
    {
        return instantiate(load(NukleusFactorySpi.class, classLoader));
    }

    public Iterable<String> names()
    {
        return factorySpisByName.keySet();
    }

    public Nukleus create(String name, Configuration config)
    {
        requireNonNull(name, "name");
        requireNonNull(config, "config");

        NukleusFactorySpi factorySpi = factorySpisByName.get(name);
        if (factorySpi == null)
        {
            throw new IllegalArgumentException("Unregonized nukleus name: " + name);
        }

        return factorySpi.create(config);
    }

    private static NukleusFactory instantiate(ServiceLoader<NukleusFactorySpi> factories)
    {
        Map<String, NukleusFactorySpi> factorySpisByName = new HashMap<>();
        factories.forEach((factorySpi) -> factorySpisByName.put(factorySpi.name(), factorySpi));

        return new NukleusFactory(unmodifiableMap(factorySpisByName));
    }

    private final Map<String, NukleusFactorySpi> factorySpisByName;

    private NukleusFactory(Map<String, NukleusFactorySpi> factorySpisByName)
    {
        this.factorySpisByName = factorySpisByName;
    }
}
