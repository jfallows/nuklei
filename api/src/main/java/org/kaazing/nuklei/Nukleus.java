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

import uk.co.real_logic.agrona.collections.ArrayUtil;

@FunctionalInterface
public interface Nukleus extends AutoCloseable
{
    int process();

    @Override
    default void close() throws Exception
    {
    }

    default String name()
    {
        return null;
    }

    class Composite implements Nukleus
    {
        private Nukleus[] nuklei;

        protected Composite(
            Nukleus... nuklei)
        {
            this.nuklei = nuklei;
        }

        @Override
        public int process()
        {
            int weight = 0;

            for (int i=0; i < nuklei.length; i++)
            {
                weight += nuklei[i].process();
            }

            return weight;
        }

        @Override
        public void close() throws Exception
        {
            Exception deferred = null;

            for (int i=0; i < nuklei.length; i++)
            {
                try
                {
                    nuklei[i].close();
                }
                catch (Exception ex)
                {
                    if (deferred == null)
                    {
                        deferred = ex;
                    }
                    else
                    {
                        deferred.addSuppressed(ex);
                    }
                }
            }

            if (deferred != null)
            {
                throw deferred;
            }
        }

        @Override
        public String toString()
        {
            return name();
        }

        protected final <T extends Nukleus> T include(
            T nukleus)
        {
            nuklei = ArrayUtil.add(nuklei, nukleus);
            return nukleus;
        }

        protected final <T extends Nukleus> T exclude(
            T nukleus)
        {
            nuklei = ArrayUtil.remove(nuklei, nukleus);
            return nukleus;
        }
    }
}
