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

import java.io.Closeable;

@FunctionalInterface
public interface Nukleus extends AutoCloseable
{
    int process();

    default void close() throws Exception
    {
    }

    default String name()
    {
        return null;
    }

    public static final class Noop implements Nukleus
    {
        private final Closeable closer;

        @Override
        public int process()
        {
            return 0;
        }

        @Override
        public String name()
        {
            return "noop";
        }

        @Override
        public void close() throws Exception
        {
            closer.close();
        }

        public static Nukleus of(Closeable closer)
        {
            return new Noop(closer);
        }

        private Noop(Closeable close)
        {
            this.closer = close;
        }
    }

    public static class Composite implements Nukleus
    {
        private final Nukleus[] nuklei;

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
    }
}
