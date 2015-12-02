/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.ServiceLoader.load;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.Resource;

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

    public Nukleus create(String name, Configuration config)
    {
        requireNonNull(name);
        requireNonNull(config);

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
        factories.forEach((factorySpi) -> { factorySpisByName.put(factorySpi.name(), factorySpi); });

        NukleusFactory factory = new NukleusFactory(unmodifiableMap(factorySpisByName));
        factorySpisByName.values().forEach((factorySpi) -> { inject(factorySpi, NukleusFactory.class, factory); });

        return factory;
    }

    private static <T> void inject(Object target, Class<T> type, T instance)
    {
        try
        {
            Class<? extends Object> targetClass = target.getClass();
            Method[] targetMethods = targetClass.getMethods();
            for (Method targetMethod : targetMethods)
            {
                if (targetMethod.getAnnotation(Resource.class) == null)
                {
                    continue;
                }

                if (!isPublic(targetMethod.getModifiers()) || isStatic(targetMethod.getModifiers()))
                {
                    continue;
                }

                Class<?>[] targetMethodParameterTypes = targetMethod.getParameterTypes();
                if (targetMethodParameterTypes.length != 1 || targetMethodParameterTypes[0] != type)
                {
                    continue;
                }

                try
                {
                    targetMethod.invoke(target, instance);
                }
                catch (IllegalArgumentException e)
                {
                    // failed to inject
                }
                catch (IllegalAccessException e)
                {
                    // failed to inject
                }
                catch (InvocationTargetException e)
                {
                    // failed to inject
                }
            }
        }
        catch (SecurityException e)
        {
            // failed to reflect
        }
    }

    private final Map<String, NukleusFactorySpi> factorySpisByName;

    private NukleusFactory(Map<String, NukleusFactorySpi> factorySpisByName)
    {
        this.factorySpisByName = factorySpisByName;
    }
}
