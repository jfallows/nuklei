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
package org.kaazing.nuklei.amqp_1_0.codec.types;

import org.kaazing.nuklei.FlyweightBE;

public abstract class Type extends FlyweightBE
{

    public static enum Kind
    {
        DYNAMIC, NULL, BOOLEAN, UBYTE, USHORT, UINT, ULONG, BYTE, SHORT, INT,
        LONG, FLOAT, DOUBLE, DECIMAL32, DECIMAL64, DECIMAL128, CHAR, TIMESTAMP,
        UUID, BINARY, STRING, SYMBOL, LIST, MAP, ARRAY, DESCRIBED, DATA,
        AMQPSEQUENCE, AMQPVALUE, MESSAGE
    };

    public abstract Kind kind();

}
