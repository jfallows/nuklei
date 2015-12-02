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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Properties;

public class Configuration
{
    public static final String DIRECTORY_PROPERTY_NAME = "nuklei.directory";

    public static final String STREAMS_CAPACITY_PROPERTY_NAME = "nuklei.streams.capacity";

    public static final String COMMAND_BUFFER_CAPACITY_PROPERTY_NAME = "nuklei.command.buffer.capacity";

    public static final String RESPONSE_BUFFER_CAPACITY_PROPERTY_NAME = "nuklei.response.buffer.capacity";

    public static final String COUNTER_VALUES_BUFFER_CAPACITY_PROPERTY_NAME = "nuklei.counters.buffer.capacity";

    public static final int STREAMS_CAPACITY_DEFAULT = 1024 * 1024;

    public static final int COMMAND_BUFFER_CAPACITY_DEFAULT = 1024 * 1024;

    public static final int RESPONSE_BUFFER_CAPACITY_DEFAULT = 1024 * 1024;

    public static final int COUNTER_VALUES_BUFFER_CAPACITY_DEFAULT = 1024 * 1024;

    private final Properties properties;

    public Configuration()
    {
        // use System.getProperty(...)
        this.properties = null;
    }

    public Configuration(Properties properties)
    {
        requireNonNull(properties, "properties");
        this.properties = properties;
    }

    public File directory()
    {
        return new File(getProperty(DIRECTORY_PROPERTY_NAME, "./"));
    }

    public int streamsCapacity()
    {
        return getInteger(STREAMS_CAPACITY_PROPERTY_NAME, STREAMS_CAPACITY_DEFAULT);
    }

    public int commandBufferCapacity()
    {
        return getInteger(COMMAND_BUFFER_CAPACITY_PROPERTY_NAME, COMMAND_BUFFER_CAPACITY_DEFAULT);
    }

    public int responseBufferCapacity()
    {
        return getInteger(RESPONSE_BUFFER_CAPACITY_PROPERTY_NAME, RESPONSE_BUFFER_CAPACITY_DEFAULT);
    }

    public int counterValuesBufferLength()
    {
        return getInteger(COUNTER_VALUES_BUFFER_CAPACITY_PROPERTY_NAME, COUNTER_VALUES_BUFFER_CAPACITY_DEFAULT);
    }

    public int counterLabelsBufferLength()
    {
        return counterValuesBufferLength();
    }

    private String getProperty(String key, String defaultValue)
    {
        if (properties == null)
        {
            return System.getProperty(key, defaultValue);
        }
        else
        {
            return properties.getProperty(key, defaultValue);
        }
    }

    private Integer getInteger(String key, int defaultValue)
    {
        if (properties == null)
        {
            return Integer.getInteger(key, defaultValue);
        }
        else
        {
            String value = properties.getProperty(key);
            if (value == null)
            {
                return defaultValue;
            }
            return Integer.valueOf(value);
        }
    }

}
