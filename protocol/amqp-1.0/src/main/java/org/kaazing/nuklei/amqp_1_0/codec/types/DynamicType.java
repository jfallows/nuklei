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

import java.util.function.Consumer;

import org.kaazing.nuklei.Flyweight;

import uk.co.real_logic.agrona.DirectBuffer;

public class DynamicType extends Type
{

    private static final int WIDTH_KIND_0_NULL = 0x40;
    private static final int WIDTH_KIND_0_TRUE = 0x41;
    private static final int WIDTH_KIND_0_FALSE = 0x42;
    private static final int WIDTH_KIND_0_UINT = 0x43;
    private static final int WIDTH_KIND_0_ULONG = 0x44;
    private static final int WIDTH_KIND_0_LIST = 0x45;
    private static final int WIDTH_KIND_1_UBYTE = 0x50;
    private static final int WIDTH_KIND_1_BYTE = 0x51;
    private static final int WIDTH_KIND_1_UINT = 0x52;
    private static final int WIDTH_KIND_1_ULONG = 0x53;
    private static final int WIDTH_KIND_1_INT = 0x54;
    private static final int WIDTH_KIND_1_LONG = 0x55;
    private static final int WIDTH_KIND_1_BOOLEAN = 0x56;
    private static final int WIDTH_KIND_2_USHORT = 0x60;
    private static final int WIDTH_KIND_2_SHORT = 0x61;
    private static final int WIDTH_KIND_4_UINT = 0x70;
    private static final int WIDTH_KIND_4_INT = 0x71;
    private static final int WIDTH_KIND_4_FLOAT = 0x72;
    private static final int WIDTH_KIND_4_CHAR = 0x73;
    private static final int WIDTH_KIND_4_DECIMAL32 = 0x74;
    private static final int WIDTH_KIND_8_ULONG = 0x80;
    private static final int WIDTH_KIND_8_LONG = 0x81;
    private static final int WIDTH_KIND_8_DOUBLE = 0x82;
    private static final int WIDTH_KIND_8_TIMESTAMP = 0x83;
    private static final int WIDTH_KIND_8_DECIMAL64 = 0x84;
    private static final int WIDTH_KIND_16_DECIMAL128 = 0x94;
    private static final int WIDTH_KIND_16_UUID = 0x98;
    private static final int WIDTH_KIND_1_BINARY = 0xa0;
    private static final int WIDTH_KIND_1_STRING = 0xa1;
    private static final int WIDTH_KIND_1_SYMBOL = 0xa3;
    private static final int WIDTH_KIND_4_BINARY = 0xb0;
    private static final int WIDTH_KIND_4_STRING = 0xb1;
    private static final int WIDTH_KIND_4_SYMBOL = 0xb3;
    private static final int WIDTH_KIND_1_LIST = 0xc0;
    private static final int WIDTH_KIND_1_MAP = 0xc1;
    private static final int WIDTH_KIND_4_LIST = 0xd0;
    private static final int WIDTH_KIND_4_MAP = 0xd1;
    private static final int WIDTH_KIND_1_ARRAY = 0xe0;
    private static final int WIDTH_KIND_4_ARRAY = 0xf0;

    @Override
    public Kind kind()
    {
        switch (uint8Get(buffer(), offset()))
        {
        case WIDTH_KIND_1_ARRAY:
        case WIDTH_KIND_4_ARRAY:
            return Kind.ARRAY;
        case WIDTH_KIND_1_BINARY:
        case WIDTH_KIND_4_BINARY:
            return Kind.BINARY;
        case WIDTH_KIND_0_TRUE:
        case WIDTH_KIND_0_FALSE:
        case WIDTH_KIND_1_BOOLEAN:
            return Kind.BOOLEAN;
        case WIDTH_KIND_1_BYTE:
            return Kind.BYTE;
        case WIDTH_KIND_4_CHAR:
            return Kind.CHAR;
        case WIDTH_KIND_16_DECIMAL128:
            return Kind.DECIMAL128;
        case WIDTH_KIND_4_DECIMAL32:
            return Kind.DECIMAL32;
        case WIDTH_KIND_8_DECIMAL64:
            return Kind.DECIMAL64;
        case WIDTH_KIND_8_DOUBLE:
            return Kind.DOUBLE;
        case WIDTH_KIND_4_FLOAT:
            return Kind.FLOAT;
        case WIDTH_KIND_1_INT:
        case WIDTH_KIND_4_INT:
            return Kind.INT;
        case WIDTH_KIND_0_LIST:
        case WIDTH_KIND_1_LIST:
        case WIDTH_KIND_4_LIST:
            return Kind.LIST;
        case WIDTH_KIND_1_LONG:
        case WIDTH_KIND_8_LONG:
            return Kind.LONG;
        case WIDTH_KIND_1_MAP:
        case WIDTH_KIND_4_MAP:
            return Kind.MAP;
        case WIDTH_KIND_0_NULL:
            return Kind.NULL;
        case WIDTH_KIND_2_SHORT:
            return Kind.SHORT;
        case WIDTH_KIND_1_STRING:
        case WIDTH_KIND_4_STRING:
            return Kind.STRING;
        case WIDTH_KIND_1_SYMBOL:
        case WIDTH_KIND_4_SYMBOL:
            return Kind.SYMBOL;
        case WIDTH_KIND_8_TIMESTAMP:
            return Kind.TIMESTAMP;
        case WIDTH_KIND_1_UBYTE:
            return Kind.UBYTE;
        case WIDTH_KIND_0_UINT:
        case WIDTH_KIND_1_UINT:
        case WIDTH_KIND_4_UINT:
            return Kind.UINT;
        case WIDTH_KIND_0_ULONG:
        case WIDTH_KIND_1_ULONG:
            return Kind.ULONG;
        case WIDTH_KIND_2_USHORT:
            return Kind.USHORT;
        case WIDTH_KIND_8_ULONG:
            return Kind.ULONG;
        case WIDTH_KIND_16_UUID:
            return Kind.UUID;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public DynamicType watch(Consumer<Flyweight> observer)
    {
        super.watch(observer);
        return this;
    }

    @Override
    public DynamicType wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        super.wrap(buffer, offset, mutable);
        return this;
    }

    public int limit()
    {
        switch (uint8Get(buffer(), offset()))
        {
        case WIDTH_KIND_0_NULL:
        case WIDTH_KIND_0_TRUE:
        case WIDTH_KIND_0_FALSE:
        case WIDTH_KIND_0_UINT:
        case WIDTH_KIND_0_ULONG:
        case WIDTH_KIND_0_LIST:
            return offset() + 1;
        case WIDTH_KIND_1_UBYTE:
        case WIDTH_KIND_1_BYTE:
        case WIDTH_KIND_1_UINT:
        case WIDTH_KIND_1_ULONG:
        case WIDTH_KIND_1_INT:
        case WIDTH_KIND_1_LONG:
        case WIDTH_KIND_1_BOOLEAN:
            return offset() + 2;
        case WIDTH_KIND_2_USHORT:
        case WIDTH_KIND_2_SHORT:
            return offset() + 3;
        case WIDTH_KIND_4_UINT:
        case WIDTH_KIND_4_INT:
        case WIDTH_KIND_4_FLOAT:
        case WIDTH_KIND_4_CHAR:
        case WIDTH_KIND_4_DECIMAL32:
            return offset() + 5;
        case WIDTH_KIND_8_ULONG:
        case WIDTH_KIND_8_LONG:
        case WIDTH_KIND_8_DOUBLE:
        case WIDTH_KIND_8_TIMESTAMP:
        case WIDTH_KIND_8_DECIMAL64:
            return offset() + 9;
        case WIDTH_KIND_16_DECIMAL128:
        case WIDTH_KIND_16_UUID:
            return offset() + 17;
        case WIDTH_KIND_1_ARRAY:
            return offset() + 3;
        case WIDTH_KIND_4_ARRAY:
            return offset() + 9;
        case WIDTH_KIND_1_BINARY:
        case WIDTH_KIND_1_STRING:
        case WIDTH_KIND_1_SYMBOL:
            return offset() + 2 + uint8Get(buffer(), offset() + 1);
        case WIDTH_KIND_1_LIST:
        case WIDTH_KIND_1_MAP:
            return offset() + 2 + uint8Get(buffer(), offset() + 1);
        case WIDTH_KIND_4_BINARY:
        case WIDTH_KIND_4_STRING:
        case WIDTH_KIND_4_SYMBOL:
            return offset() + 5 + int32Get(buffer(), offset() + 1);
        case WIDTH_KIND_4_LIST:
        case WIDTH_KIND_4_MAP:
            return offset() + 9 + int32Get(buffer(), offset() + 5);
        default:
            throw new IllegalArgumentException();
        }
    }
}
