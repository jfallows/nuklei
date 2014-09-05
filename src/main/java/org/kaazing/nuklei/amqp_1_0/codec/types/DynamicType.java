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
        case 0x40:
            return nullType.wrap(buffer, offset);
        case 0x41:
        case 0x42:
            return booleanType.wrap(buffer, offset);
        case 0x43:
            return uintType.wrap(buffer, offset);
        case 0x44:
            return ulongType.wrap(buffer, offset);
        case 0x45:
            return listType.wrap(buffer, offset);
        case 0x50:
            return ubyteType.wrap(buffer, offset);
        case 0x51:
            return byteType.wrap(buffer, offset);
        case 0x52:
            return uintType.wrap(buffer, offset);
        case 0x53:
            return ulongType.wrap(buffer, offset);
        case 0x54:
            return intType.wrap(buffer, offset);
        case 0x55:
            return longType.wrap(buffer, offset);
        case 0x56:
            return booleanType.wrap(buffer, offset);
        case 0x60:
            return ushortType.wrap(buffer, offset);
        case 0x61:
            return shortType.wrap(buffer, offset);
        case 0x70:
            return uintType.wrap(buffer, offset);
        case 0x71:
            return intType.wrap(buffer, offset);
        case 0x72:
            return floatType.wrap(buffer, offset);
        case 0x73:
            return charType.wrap(buffer, offset);
        case 0x74:
            return decimal32Type.wrap(buffer, offset);
        case 0x80:
            return ulongType.wrap(buffer, offset);
        case 0x81:
            return longType.wrap(buffer, offset);
        case 0x82:
            return doubleType.wrap(buffer, offset);
        case 0x83:
            return timestampType.wrap(buffer, offset);
        case 0x84:
            return decimal64Type.wrap(buffer, offset);
        case 0x94:
            return decimal128Type.wrap(buffer, offset);
        case 0x98:
            return uuidType.wrap(buffer, offset);
        case 0xa0:
            return binaryType.wrap(buffer, offset);
        case 0xa1:
            return stringType.wrap(buffer, offset);
        case 0xa3:
            return symbolType.wrap(buffer, offset);
        case 0xb0:
            return binaryType.wrap(buffer, offset);
        case 0xb1:
            return stringType.wrap(buffer, offset);
        case 0xb3:
            return symbolType.wrap(buffer, offset);
        case 0xc0:
            return listType.wrap(buffer, offset);
        case 0xc1:
            return mapType.wrap(buffer, offset);
        case 0xd0:
            return listType.wrap(buffer, offset);
        case 0xd1:
            return mapType.wrap(buffer, offset);
        case 0xe0:
            return arrayType.wrap(buffer, offset);
        case 0xf0:
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
