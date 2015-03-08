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
package org.kaazing.nuklei.amqp_1_0.codec.definitions;

import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/*
 * See AMQP 1.0 specification, section 2.8.2 "Sender Settle Mode"
 */
public enum SenderSettleMode
{
    UNSETTLED, SETTLED, MIXED;

    public static final IntFunction<SenderSettleMode> READ = (int code) ->
    {
        switch (code)
        {
        case 0:
            return SenderSettleMode.UNSETTLED;
        case 1:
            return SenderSettleMode.SETTLED;
        case 2:
            return SenderSettleMode.MIXED;
        default:
            throw new IllegalArgumentException();
        }
    };

    public static final ToIntFunction<SenderSettleMode> WRITE = (SenderSettleMode mode) ->
    {
        switch (mode)
        {
        case UNSETTLED:
        case SETTLED:
        case MIXED:
            return mode.ordinal();
        default:
            throw new IllegalArgumentException();
        }
    };

}
