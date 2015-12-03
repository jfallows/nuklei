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
package org.kaazing.nuklei.http.internal;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.Test;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.NukleusFactory;
import org.kaazing.nuklei.Configuration;

public class HttpNukleusFactorySpiTest
{
    @Test
    public void shouldCreateHttpNukleus()
    {
        NukleusFactory factory = NukleusFactory.instantiate();
        Properties properties = new Properties();
        properties.setProperty(Configuration.DIRECTORY_PROPERTY_NAME, "target/nuklei-tests");
        Configuration config = new Configuration(properties);
        Nukleus nukleus = factory.create("http", config);
        assertThat(nukleus, instanceOf(HttpNukleus.class));
    }

}
