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
package org.kaazing.nuklei.amqp_1_0.codec.messaging;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

/*
 * See AMQP 1.0 specification, section 3.5.5 "Terminus Durability"
 */
public enum TerminusDurability
{
    NONE, CONFIGURATION, UNSETTLED_STATE;

    public static final LongFunction<TerminusDurability> READ = (long code) ->
    {
        switch ((int) code)
        {
        case 0:
            return TerminusDurability.NONE;
        case 1:
            return TerminusDurability.CONFIGURATION;
        case 2:
            return TerminusDurability.UNSETTLED_STATE;
        default:
            return null;
        }
    };

    public static final ToLongFunction<TerminusDurability> WRITE = (TerminusDurability mode) ->
    {
        switch (mode)
        {
        case NONE:
        case CONFIGURATION:
        case UNSETTLED_STATE:
            return mode.ordinal();
        default:
            throw new IllegalArgumentException();
        }
    };

}
