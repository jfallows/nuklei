/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
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

import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastBufferDescriptor;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class Configuration
{
    public static final String DIRECTORY_PROPERTY_NAME = "nuklei.directory";

    public static final String CONDUCTOR_BUFFER_LENGTH_PROPERTY_NAME = "nuklei.conductor.buffer.length";

    public static final String BROADCAST_BUFFER_LENGTH_PROPERTY_NAME = "nuklei.broadcast.buffer.length";

    public static final String COUNTER_VALUES_BUFFER_LENGTH_PROPERTY_NAME = "nuklei.counters.buffer.length";

    public static final int CONDUCTOR_BUFFER_LENGTH_DEFAULT = 1024 * 1024 + RingBufferDescriptor.TRAILER_LENGTH;

    public static final int BROADCAST_BUFFER_LENGTH_DEFAULT = 1024 * 1024 + BroadcastBufferDescriptor.TRAILER_LENGTH;

    public static final int COUNTER_VALUES_BUFFER_LENGTH_DEFAULT = 1024 * 1024;

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

    public int conductorBufferLength()
    {
        return getInteger(CONDUCTOR_BUFFER_LENGTH_PROPERTY_NAME, CONDUCTOR_BUFFER_LENGTH_DEFAULT);
    }

    public int broadcastBufferLength()
    {
        return getInteger(BROADCAST_BUFFER_LENGTH_PROPERTY_NAME, BROADCAST_BUFFER_LENGTH_DEFAULT);
    }

    public int counterValuesBufferLength()
    {
        return getInteger(COUNTER_VALUES_BUFFER_LENGTH_PROPERTY_NAME, COUNTER_VALUES_BUFFER_LENGTH_DEFAULT);
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
