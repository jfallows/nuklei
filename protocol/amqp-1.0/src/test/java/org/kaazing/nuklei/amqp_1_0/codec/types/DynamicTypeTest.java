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

import static java.math.MathContext.DECIMAL128;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.fromString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.kaazing.nuklei.amqp_1_0.codec.util.FieldMutators.newMutator;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.amqp_1_0.codec.types.Type.Kind;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

@RunWith(Theories.class)
public class DynamicTypeTest
{

    private static final int BUFFER_CAPACITY = 512;
    private static final MutableDirectBufferMutator<String> WRITE_UTF_8 = newMutator(UTF_8);

    @DataPoint
    public static final int ZERO_OFFSET = 0;

    @DataPoint
    public static final int NON_ZERO_OFFSET = new Random().nextInt(BUFFER_CAPACITY - 256) + 1;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[BUFFER_CAPACITY]);

    @Theory
    public void shouldDecodeDynamicAsArray1(int offset)
    {
        ArrayType arrayType = new ArrayType();
        arrayType.wrap(buffer, offset, true);
        arrayType.maxLength(0xff);
        arrayType.limit(0x00, offset + 0x03);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.ARRAY, dynamicType.kind());
        assertEquals(arrayType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsArray4(int offset)
    {
        ArrayType arrayType = new ArrayType();
        arrayType.wrap(buffer, offset, true);
        arrayType.maxLength(0x100);
        arrayType.limit(0x00, offset + 0x09);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.ARRAY, dynamicType.kind());
        assertEquals(arrayType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsBinary1(int offset)
    {
        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);
        binaryType.set(WRITE_UTF_8, "Hello, world");

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.BINARY, dynamicType.kind());
        assertEquals(binaryType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsBinary4(int offset)
    {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');

        BinaryType binaryType = new BinaryType();
        binaryType.wrap(buffer, offset, true);
        binaryType.set(WRITE_UTF_8, new String(chars));

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.BINARY, dynamicType.kind());
        assertEquals(binaryType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsBooleanTrue(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(true);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.BOOLEAN, dynamicType.kind());
        assertEquals(booleanType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsBooleanFalse(int offset)
    {
        BooleanType booleanType = new BooleanType();
        booleanType.wrap(buffer, offset, true);
        booleanType.set(false);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.BOOLEAN, dynamicType.kind());
        assertEquals(booleanType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsByte(int offset)
    {
        ByteType byteType = new ByteType();
        byteType.wrap(buffer, offset, true);
        byteType.set((byte) 0x12);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.BYTE, dynamicType.kind());
        assertEquals(byteType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsChar(int offset)
    {
        CharType charType = new CharType();
        charType.wrap(buffer, offset, true);
        charType.set(0x12);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.CHAR, dynamicType.kind());
        assertEquals(charType.limit(), dynamicType.limit());
    }

    @Theory
    @Ignore("until decimal128 format implemented")
    public void shouldDecodeDynamicAsDecimal128(int offset)
    {
        Decimal128Type decimal128Type = new Decimal128Type();
        decimal128Type.wrap(buffer, offset, true);
        decimal128Type.set(new BigDecimal(1.23456, DECIMAL128));

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.DECIMAL128, dynamicType.kind());
        assertEquals(decimal128Type.limit(), dynamicType.limit());
    }

    @Theory
    @Ignore("until decimal32 format implemented")
    public void shouldDecodeDynamicAsDecimal32(int offset)
    {
        Decimal32Type decimal32Type = new Decimal32Type();
        decimal32Type.wrap(buffer, offset, true);
        decimal32Type.set(new BigDecimal(1.23456, DECIMAL128));

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.DECIMAL32, dynamicType.kind());
        assertEquals(decimal32Type.limit(), dynamicType.limit());
    }

    @Theory
    @Ignore("until decimal64 format implemented")
    public void shouldDecodeDynamicAsDecimal64(int offset)
    {
        Decimal64Type decimal64Type = new Decimal64Type();
        decimal64Type.wrap(buffer, offset, true);
        decimal64Type.set(new BigDecimal(1.23456, DECIMAL128));

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.DECIMAL64, dynamicType.kind());
        assertEquals(decimal64Type.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsDouble(int offset)
    {
        DoubleType doubleType = new DoubleType();
        doubleType.wrap(buffer, offset, true);
        doubleType.set(12345678d);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.DOUBLE, dynamicType.kind());
        assertEquals(doubleType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsFloat(int offset)
    {
        FloatType floatType = new FloatType();
        floatType.wrap(buffer, offset, true);
        floatType.set(12345678f);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.FLOAT, dynamicType.kind());
        assertEquals(floatType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsInt1(int offset)
    {
        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);
        intType.set(1);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.INT, dynamicType.kind());
        assertEquals(intType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsInt4(int offset)
    {
        IntType intType = new IntType();
        intType.wrap(buffer, offset, true);
        intType.set(0x12345678);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.INT, dynamicType.kind());
        assertEquals(intType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsList0(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0x00);
        listType.clear();

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.LIST, dynamicType.kind());
        assertEquals(listType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsList1(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0xff);
        listType.clear();

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.LIST, dynamicType.kind());
        assertEquals(listType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsList8(int offset)
    {
        ListType listType = new ListType();
        listType.wrap(buffer, offset, true);
        listType.maxLength(0x100);
        listType.clear();

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.LIST, dynamicType.kind());
        assertEquals(listType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsLong1(int offset)
    {
        LongType longType = new LongType();
        longType.wrap(buffer, offset, true);
        longType.set(1L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.LONG, dynamicType.kind());
        assertEquals(longType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsLong8(int offset)
    {
        LongType longType = new LongType();
        longType.wrap(buffer, offset, true);
        longType.set(12345678L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.LONG, dynamicType.kind());
        assertEquals(longType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsMap1(int offset)
    {
        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);
        mapType.maxLength(0xff);
        mapType.clear();

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.MAP, dynamicType.kind());
        assertEquals(mapType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsMap8(int offset)
    {
        MapType mapType = new MapType();
        mapType.wrap(buffer, offset, true);
        mapType.maxLength(0x100);
        mapType.clear();

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.MAP, dynamicType.kind());
        assertEquals(mapType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsNull(int offset)
    {
        NullType nullType = new NullType();
        nullType.wrap(buffer, offset, true);
        nullType.set(null);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.NULL, dynamicType.kind());
        assertEquals(nullType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsShort(int offset)
    {
        ShortType shortType = new ShortType();
        shortType.wrap(buffer, offset, true);
        shortType.set((short) 0x1234);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.SHORT, dynamicType.kind());
        assertEquals(shortType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsString1(int offset)
    {
        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);
        stringType.set(WRITE_UTF_8, "a");

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.STRING, dynamicType.kind());
        assertEquals(stringType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsString4(int offset)
    {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');

        StringType stringType = new StringType();
        stringType.wrap(buffer, offset, true);
        stringType.set(WRITE_UTF_8, new String(chars));

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.STRING, dynamicType.kind());
        assertEquals(stringType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsSymbol1(int offset)
    {
        SymbolType symbolType = new SymbolType();
        symbolType.wrap(buffer, offset, true);
        symbolType.set(WRITE_UTF_8, "a");

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.SYMBOL, dynamicType.kind());
        assertEquals(symbolType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsSymbol4(int offset)
    {
        char[] chars = new char[256];
        Arrays.fill(chars, 'a');

        SymbolType symbolType = new SymbolType();
        symbolType.wrap(buffer, offset, true);
        symbolType.set(WRITE_UTF_8, new String(chars));

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.SYMBOL, dynamicType.kind());
        assertEquals(symbolType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsTimestamp(int offset)
    {
        TimestampType timestampType = new TimestampType();
        timestampType.wrap(buffer, offset, true);
        timestampType.set(0x12345678L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.TIMESTAMP, dynamicType.kind());
        assertEquals(timestampType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsUByte(int offset)
    {
        UByteType ubyteType = new UByteType();
        ubyteType.wrap(buffer, offset, true);
        ubyteType.set(0x12);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.UBYTE, dynamicType.kind());
        assertEquals(ubyteType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsUInt0(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(0L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.UINT, dynamicType.kind());
        assertEquals(uintType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsUInt1(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(1L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.UINT, dynamicType.kind());
        assertEquals(uintType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsUInt4(int offset)
    {
        UIntType uintType = new UIntType();
        uintType.wrap(buffer, offset, true);
        uintType.set(0x12345678L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.UINT, dynamicType.kind());
        assertEquals(uintType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsULong0(int offset)
    {
        ULongType ulongType = new ULongType();
        ulongType.wrap(buffer, offset, true);
        ulongType.set(0L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.ULONG, dynamicType.kind());
        assertEquals(ulongType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsULong1(int offset)
    {
        ULongType ulongType = new ULongType();
        ulongType.wrap(buffer, offset, true);
        ulongType.set(1L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.ULONG, dynamicType.kind());
        assertEquals(ulongType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsULong8(int offset)
    {
        ULongType ulongType = new ULongType();
        ulongType.wrap(buffer, offset, true);
        ulongType.set(12345678L);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.ULONG, dynamicType.kind());
        assertEquals(ulongType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsUShort(int offset)
    {
        UShortType ushortType = new UShortType();
        ushortType.wrap(buffer, offset, true);
        ushortType.set(0x1234);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.USHORT, dynamicType.kind());
        assertEquals(ushortType.limit(), dynamicType.limit());
    }

    @Theory
    public void shouldDecodeDynamicAsUuid(int offset)
    {
        UuidType uuidType = new UuidType();
        uuidType.wrap(buffer, offset, true);
        uuidType.set(fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6"));

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);

        assertSame(Kind.UUID, dynamicType.kind());
        assertEquals(uuidType.limit(), dynamicType.limit());
    }

    @Theory
    @Test(expected = Exception.class)
    public void shouldNotDecode(int offset)
    {
        buffer.putByte(offset, (byte) 0x00);

        DynamicType dynamicType = new DynamicType();
        dynamicType.wrap(buffer, offset, true);
        dynamicType.limit();
    }

    @Theory
    @SuppressWarnings("unchecked")
    public void shouldNotifyChanged(int offset)
    {
        final Consumer<Flyweight> observer = mock(Consumer.class);

        NullType nullType = new NullType();
        nullType.watch(observer);
        nullType.wrap(buffer, offset, true);
        nullType.set(null);

        verify(observer).accept(nullType);
    }

}
