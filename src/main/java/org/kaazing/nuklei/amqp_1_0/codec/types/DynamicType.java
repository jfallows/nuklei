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

import org.kaazing.nuklei.concurrent.AtomicBuffer;

public class DynamicType extends Type {

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
    
    private final NullType nullType;
    private final BooleanType booleanType;
    private final UByteType ubyteType;
    private final UShortType ushortType;
    private final UIntType uintType;
    private final ULongType ulongType;
    private final ByteType byteType;
    private final ShortType shortType;
    private final IntType intType;
    private final LongType longType;
    private final FloatType floatType;
    private final DoubleType doubleType;
    private final Decimal32Type decimal32Type;
    private final Decimal64Type decimal64Type;
    private final Decimal128Type decimal128Type;
    private final CharType charType;
    private final TimestampType timestampType;
    private final UuidType uuidType;
    private final BinaryType binaryType;
    private final StringType stringType;
    private SymbolType symbolType;
    private ListType listType;
    private MapType mapType;
    private ArrayType arrayType;

    public DynamicType() {
        nullType = new NullType();
        booleanType = new BooleanType();
        ubyteType = new UByteType();
        ushortType = new UShortType();
        uintType = new UIntType();
        ulongType = new ULongType();
        byteType = new ByteType();
        shortType = new ShortType();
        intType = new IntType();
        longType = new LongType();
        floatType = new FloatType();
        doubleType = new DoubleType();
        decimal32Type = new Decimal32Type();
        decimal64Type = new Decimal64Type();
        decimal128Type = new Decimal128Type();
        charType = new CharType();
        timestampType = new TimestampType();
        uuidType = new UuidType();
        binaryType = new BinaryType();
        stringType = new StringType();
        symbolType = new SymbolType();
        listType = new ListType();
        mapType = new MapType();
        arrayType = new ArrayType();
    }

    @Override
    public Type wrap(AtomicBuffer buffer, int offset) {
        super.wrap(buffer, offset);

        switch (uint8Get(buffer, offset)) {
        case WIDTH_KIND_0_NULL:
            return nullType.wrap(buffer, offset);
        case WIDTH_KIND_0_TRUE:
        case WIDTH_KIND_0_FALSE:
            return booleanType.wrap(buffer, offset);
        case WIDTH_KIND_0_UINT:
            return uintType.wrap(buffer, offset);
        case WIDTH_KIND_0_ULONG:
            return ulongType.wrap(buffer, offset);
        case WIDTH_KIND_0_LIST:
            return listType.wrap(buffer, offset);
        case WIDTH_KIND_1_UBYTE:
            return ubyteType.wrap(buffer, offset);
        case WIDTH_KIND_1_BYTE:
            return byteType.wrap(buffer, offset);
        case WIDTH_KIND_1_UINT:
            return uintType.wrap(buffer, offset);
        case WIDTH_KIND_1_ULONG:
            return ulongType.wrap(buffer, offset);
        case WIDTH_KIND_1_INT:
            return intType.wrap(buffer, offset);
        case WIDTH_KIND_1_LONG:
            return longType.wrap(buffer, offset);
        case WIDTH_KIND_1_BOOLEAN:
            return booleanType.wrap(buffer, offset);
        case WIDTH_KIND_2_USHORT:
            return ushortType.wrap(buffer, offset);
        case WIDTH_KIND_2_SHORT:
            return shortType.wrap(buffer, offset);
        case WIDTH_KIND_4_UINT:
            return uintType.wrap(buffer, offset);
        case WIDTH_KIND_4_INT:
            return intType.wrap(buffer, offset);
        case WIDTH_KIND_4_FLOAT:
            return floatType.wrap(buffer, offset);
        case WIDTH_KIND_4_CHAR:
            return charType.wrap(buffer, offset);
        case WIDTH_KIND_4_DECIMAL32:
            return decimal32Type.wrap(buffer, offset);
        case WIDTH_KIND_8_ULONG:
            return ulongType.wrap(buffer, offset);
        case WIDTH_KIND_8_LONG:
            return longType.wrap(buffer, offset);
        case WIDTH_KIND_8_DOUBLE:
            return doubleType.wrap(buffer, offset);
        case WIDTH_KIND_8_TIMESTAMP:
            return timestampType.wrap(buffer, offset);
        case WIDTH_KIND_8_DECIMAL64:
            return decimal64Type.wrap(buffer, offset);
        case WIDTH_KIND_16_DECIMAL128:
            return decimal128Type.wrap(buffer, offset);
        case WIDTH_KIND_16_UUID:
            return uuidType.wrap(buffer, offset);
        case WIDTH_KIND_1_BINARY:
            return binaryType.wrap(buffer, offset);
        case WIDTH_KIND_1_STRING:
            return stringType.wrap(buffer, offset);
        case WIDTH_KIND_1_SYMBOL:
            return symbolType.wrap(buffer, offset);
        case WIDTH_KIND_4_BINARY:
            return binaryType.wrap(buffer, offset);
        case WIDTH_KIND_4_STRING:
            return stringType.wrap(buffer, offset);
        case WIDTH_KIND_4_SYMBOL:
            return symbolType.wrap(buffer, offset);
        case WIDTH_KIND_1_LIST:
            return listType.wrap(buffer, offset);
        case WIDTH_KIND_1_MAP:
            return mapType.wrap(buffer, offset);
        case WIDTH_KIND_4_LIST:
            return listType.wrap(buffer, offset);
        case WIDTH_KIND_4_MAP:
            return mapType.wrap(buffer, offset);
        case WIDTH_KIND_1_ARRAY:
            return arrayType.wrap(buffer, offset);
        case WIDTH_KIND_4_ARRAY:
            return arrayType.wrap(buffer, offset);
        default:
            throw new IllegalArgumentException();
        }
        
    }

    @Override
    public Kind kind() {
        return Kind.UNKNOWN;
    }
}
