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

import static org.junit.Assert.assertSame;

import java.io.IOException;

import javax.annotation.Resource;

import org.junit.Test;

public final class NukleusFactoryTest
{

    @Test
    public void shouldLoadAndInject() throws IOException
    {
        NukleusFactory factory = NukleusFactory.instantiate();
        TestNukleus nukleus = (TestNukleus) factory.create("test", new Configuration());
        assertSame(factory, nukleus.factory());
    }

    public static final class TestNukleusFactory implements NukleusFactorySpi
    {
        private NukleusFactory factory;

        @Resource
        public void setFacktory(NukleusFactory factory)
        {
            this.factory = factory;
        }

        @Override
        public String name()
        {
            return "test";
        }

        @Override
        public Nukleus create(Configuration options)
        {
            return new TestNukleus(factory);
        }

    }

    static final class TestNukleus implements Nukleus
    {
        private final NukleusFactory factory;

        TestNukleus(NukleusFactory factory)
        {
            this.factory = factory;
        }

        NukleusFactory factory()
        {
            return factory;
        }

        @Override
        public int process()
        {
            // no-op
            return 0;
        }

        @Override
        public void close() throws Exception
        {
            // no-op
        }

    }
}
