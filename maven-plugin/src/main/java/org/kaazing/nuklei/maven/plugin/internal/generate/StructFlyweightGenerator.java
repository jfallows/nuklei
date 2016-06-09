/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.maven.plugin.internal.generate;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.unmodifiableMap;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.BIT_UTIL_TYPE;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.BYTE_ARRAY;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.DIRECT_BUFFER_TYPE;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.MUTABLE_DIRECT_BUFFER_TYPE;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.UNSAFE_BUFFER_TYPE;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

import javax.annotation.Generated;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.FieldSpec.Builder;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

@SuppressWarnings("squid:S1192")
public final class StructFlyweightGenerator extends ClassSpecGenerator
{
    private final String baseName;
    private final TypeSpec.Builder builder;
    private final TypeIdGenerator typeId;
    private final MemberFieldGenerator memberField;
    private final MemberSizeConstantGenerator memberSizeConstant;
    private final MemberOffsetConstantGenerator memberOffsetConstant;
    private final MemberAccessorGenerator memberAccessor;
    private final WrapMethodGenerator wrapMethod;
    private final LimitMethodGenerator limitMethod;
    private final ToStringMethodGenerator toStringMethod;
    private final BuilderClassGenerator builderClass;

    public StructFlyweightGenerator(
        ClassName structName,
        ClassName flyweightName,
        String baseName)
    {
        super(structName);

        this.baseName = baseName;
        this.builder = classBuilder(structName).superclass(flyweightName).addModifiers(PUBLIC, FINAL)
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "nuklei").build());
        this.typeId = new TypeIdGenerator(structName, builder);
        this.memberSizeConstant = new MemberSizeConstantGenerator(structName, builder);
        this.memberOffsetConstant = new MemberOffsetConstantGenerator(structName, builder);
        this.memberField = new MemberFieldGenerator(structName, builder);
        this.memberAccessor = new MemberAccessorGenerator(structName, builder);
        this.wrapMethod = new WrapMethodGenerator();
        this.limitMethod = new LimitMethodGenerator();
        this.toStringMethod = new ToStringMethodGenerator();
        this.builderClass = new BuilderClassGenerator(structName, flyweightName);
    }

    public StructFlyweightGenerator typeId(
        int typeId)
    {
        this.typeId.typeId(typeId);
        return this;
    }

    public StructFlyweightGenerator addMember(
        String name,
        TypeName type,
        TypeName unsignedType)
    {
        memberOffsetConstant.addMember(name, type, unsignedType);
        memberSizeConstant.addMember(name, type, unsignedType);
        memberField.addMember(name, type, unsignedType);
        memberAccessor.addMember(name, type, unsignedType);

        limitMethod.addMember(name, type, unsignedType);
        wrapMethod.addMember(name, type, unsignedType);
        toStringMethod.addMember(name, type, unsignedType);
        builderClass.addMember(name, type, unsignedType);

        return this;
    }

    @Override
    public TypeSpec generate()
    {
        typeId.build();
        memberOffsetConstant.build();
        memberSizeConstant.build();
        memberField.build();
        memberAccessor.build();

        return builder.addMethod(wrapMethod.generate())
                      .addMethod(limitMethod.generate())
                      .addMethod(toStringMethod.generate())
                      .addType(builderClass.generate())
                      .build();
    }

    private static final class TypeIdGenerator extends ClassSpecMixinGenerator
    {
        private int typeId;

        private TypeIdGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public void typeId(
            int typeId)
        {
            this.typeId = typeId;
        }

        @Override
        public TypeSpec.Builder build()
        {
            if (typeId != 0)
            {
                builder.addField(FieldSpec.builder(int.class, "TYPE_ID", PUBLIC, STATIC, FINAL)
                        .initializer("$L", String.format("0x%08x", typeId))
                        .build());

                builder.addMethod(methodBuilder("typeId")
                        .addModifiers(PUBLIC)
                        .returns(int.class)
                        .addStatement("return TYPE_ID")
                        .build());
            }
            return builder;
        }
    }

    private static final class MemberSizeConstantGenerator extends ClassSpecMixinGenerator
    {
        private static final Map<TypeName, String> SIZEOF_BY_NAME = initSizeofByName();

        private MemberSizeConstantGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public MemberSizeConstantGenerator addMember(
            String name,
            TypeName type,
            TypeName unsignedType)
        {
            if (type.isPrimitive())
            {
                builder.addField(
                        FieldSpec.builder(int.class, size(name), PRIVATE, STATIC, FINAL)
                                 .initializer("$T.SIZE_OF_$L", BIT_UTIL_TYPE, SIZEOF_BY_NAME.get(type))
                                 .build());
            }
            return this;
        }

        private static Map<TypeName, String> initSizeofByName()
        {
            Map<TypeName, String> sizeofByName = new HashMap<>();
            sizeofByName.put(TypeName.BOOLEAN, "BOOLEAN");
            sizeofByName.put(TypeName.BYTE, "BYTE");
            sizeofByName.put(TypeName.CHAR, "CHAR");
            sizeofByName.put(TypeName.SHORT, "SHORT");
            sizeofByName.put(TypeName.INT, "INT");
            sizeofByName.put(TypeName.FLOAT, "FLOAT");
            sizeofByName.put(TypeName.LONG, "LONG");
            sizeofByName.put(TypeName.DOUBLE, "DOUBLE");
            return sizeofByName;
        }
    }

    private static final class MemberOffsetConstantGenerator extends ClassSpecMixinGenerator
    {
        private String previousName;

        private MemberOffsetConstantGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public MemberOffsetConstantGenerator addMember(
            String name,
            TypeName type,
            TypeName unsignedType)
        {
            builder.addField(
                    FieldSpec.builder(int.class, offset(name), PRIVATE, STATIC, FINAL)
                             .initializer((previousName == null)
                                     ? "0" : String.format("%s + %s", offset(previousName), size(previousName)))
                             .build());

            previousName = type.isPrimitive() ? name : null;

            return this;
        }
    }

    private static final class MemberFieldGenerator extends ClassSpecMixinGenerator
    {
        private MemberFieldGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public MemberFieldGenerator addMember(
            String name,
            TypeName type,
            TypeName unsignedType)
        {
            if (!type.isPrimitive())
            {
                String fieldRO = String.format("%sRO", name);
                Builder fieldBuilder = FieldSpec.builder(type, fieldRO, PRIVATE, FINAL);

                if (TypeNames.DIRECT_BUFFER_TYPE.equals(type))
                {
                    fieldBuilder.initializer("new $T(new byte[0])", UNSAFE_BUFFER_TYPE);
                }
                else if (type instanceof ParameterizedTypeName)
                {
                    ParameterizedTypeName parameterizedType = (ParameterizedTypeName) type;
                    TypeName typeArgument = parameterizedType.typeArguments.get(0);
                    fieldBuilder.initializer("new $T(new $T())", type, typeArgument);
                }
                else
                {
                    fieldBuilder.initializer("new $T()", type);
                }

                builder.addField(fieldBuilder.build());
            }
            return this;
        }
    }

    private static final class MemberAccessorGenerator extends ClassSpecMixinGenerator
    {
        private static final Map<TypeName, String> GETTER_NAMES;

        static
        {
            Map<TypeName, String> getterNames = new HashMap<>();
            getterNames.put(TypeName.BYTE, "getByte");
            getterNames.put(TypeName.CHAR, "getChar");
            getterNames.put(TypeName.SHORT, "getShort");
            getterNames.put(TypeName.FLOAT, "getFloat");
            getterNames.put(TypeName.INT, "getInt");
            getterNames.put(TypeName.DOUBLE, "getDouble");
            getterNames.put(TypeName.LONG, "getLong");
            GETTER_NAMES = unmodifiableMap(getterNames);
        }

        private String anchorName;
        private TypeName anchorType;

        private MemberAccessorGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public MemberAccessorGenerator addMember(
            String name,
            TypeName type,
            TypeName unsignedType)
        {
            TypeName publicType = (unsignedType != null) ? unsignedType : type;

            CodeBlock.Builder codeBlock = CodeBlock.builder();

            if (type.isPrimitive())
            {
                String getterName = GETTER_NAMES.get(type);
                if (getterName == null)
                {
                    throw new IllegalStateException("member type not supported: " + type);
                }

                codeBlock.add("$[").add("return ");

                if (publicType != type)
                {
                    codeBlock.add("($T)(", publicType);
                }

                if (anchorName != null)
                {
                    if (DIRECT_BUFFER_TYPE.equals(anchorType))
                    {
                        codeBlock.add("buffer().$L($L().capacity() + $L", getterName, anchorName, offset(name));
                    }
                    else
                    {
                        codeBlock.add("buffer().$L($L().limit() + $L", getterName, anchorName, offset(name));
                    }
                }
                else
                {
                    codeBlock.add("buffer().$L(offset() + $L", getterName, offset(name));
                }

                if (publicType != type)
                {
                    if (type == TypeName.BYTE)
                    {
                        codeBlock.add(") & 0xFF)");
                    }
                    else if (type == TypeName.SHORT)
                    {
                        codeBlock.add(", $T.BIG_ENDIAN) & 0xFFFF)", ByteOrder.class);
                    }
                    else if (type == TypeName.INT)
                    {
                        codeBlock.add(", $T.BIG_ENDIAN) & 0xFFFF_FFFF)", ByteOrder.class);
                    }
                    else
                    {
                        codeBlock.add(")");
                    }
                }
                else
                {
                    codeBlock.add(")");
                }

                codeBlock.add(";\n$]");
            }
            else
            {
                if (DIRECT_BUFFER_TYPE.equals(type))
                {
                    MethodSpec.Builder consumerMethod = methodBuilder(name)
                            .addModifiers(PUBLIC)
                            .addParameter(IntBinaryOperator.class, "accessor")
                            .returns(type);

                    if (anchorName != null)
                    {
                        consumerMethod.addStatement("accessor.applyAsInt($L().limit() + $L, $LRO.capacity())",
                                anchorName, offset(name), name);
                    }
                    else
                    {
                        consumerMethod.addStatement("accessor.applyAsInt(offset() + $L, $LRO.capacity())", offset(name), name);
                    }

                    builder.addMethod(consumerMethod
                            .addStatement("return $LRO", name)
                            .build());
                }

                codeBlock.addStatement("return $LRO", name);

                anchorName = name;
                anchorType = type;
            }

            builder.addMethod(methodBuilder(name)
                    .addModifiers(PUBLIC)
                    .returns(publicType)
                    .addCode(codeBlock.build())
                    .build());

            return this;
        }
    }

    private final class LimitMethodGenerator extends MethodSpecGenerator
    {
        private String anchorName;
        private TypeName anchorType;
        private String lastName;
        private TypeName lastType;

        private LimitMethodGenerator()
        {
            super(methodBuilder("limit")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .returns(int.class));
        }

        public LimitMethodGenerator addMember(
            String name,
            TypeName type,
            TypeName unsignedType)
        {
            if (!type.isPrimitive())
            {
                anchorName = name;
                anchorType = type;
            }

            lastName = name;
            lastType = type;

            return this;
        }

        @Override
        public MethodSpec generate()
        {
            if (lastName == null)
            {
                builder.addStatement("return offset()");
            }
            else if (anchorName != null)
            {
                if (lastType.isPrimitive())
                {
                    if (TypeNames.DIRECT_BUFFER_TYPE.equals(anchorType))
                    {
                        builder.addStatement("return $L().capacity() + $L + $L", anchorName, offset(lastName), size(lastName));
                    }
                    else
                    {
                        builder.addStatement("return $L().limit() + $L + $L", anchorName, offset(lastName), size(lastName));
                    }
                }
                else if (TypeNames.DIRECT_BUFFER_TYPE.equals(lastType))
                {
                    builder.addStatement("return offset() + $L + $L().capacity()", offset(lastName), lastName);
                }
                else
                {
                    builder.addStatement("return $L().limit()", lastName);
                }
            }
            else
            {
                builder.addStatement("return offset() + $L + $L", offset(lastName), size(lastName));
            }

            return builder.build();
        }

    }

    private final class WrapMethodGenerator extends MethodSpecGenerator
    {
        private String anchorName;

        private WrapMethodGenerator()
        {
            super(methodBuilder("wrap")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(DIRECT_BUFFER_TYPE, "buffer")
                    .addParameter(int.class, "offset")
                    .addParameter(int.class, "maxLimit")
                    .returns(thisName)
                    .addStatement("super.wrap(buffer, offset, maxLimit)"));
        }

        public WrapMethodGenerator addMember(
            String name,
            TypeName type,
            TypeName unsignedType)
        {
            if (DIRECT_BUFFER_TYPE.equals(type))
            {
                builder.addStatement("$LRO.wrap(buffer, offset + $L, maxLimit - (offset + $L))",
                        name, offset(name), offset(name));
            }
            else if (!type.isPrimitive())
            {
                if (anchorName != null)
                {
                    builder.addStatement("$LRO.wrap(buffer, $LRO.limit() + $L, maxLimit)", name, anchorName, offset(name));
                }
                else
                {
                    builder.addStatement("$LRO.wrap(buffer, offset + $L, maxLimit)", name, offset(name));
                }
                anchorName = name;
            }
            return this;
        }

        @Override
        public MethodSpec generate()
        {
            return builder.addStatement("checkLimit(limit(), maxLimit)")
                          .addStatement("return this")
                          .build();
        }
    }

    private final class ToStringMethodGenerator extends MethodSpecGenerator
    {
        private final List<String> formats = new LinkedList<>();
        private final List<String> args = new LinkedList<>();

        private ToStringMethodGenerator()
        {
            super(methodBuilder("toString")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .returns(String.class));
        }

        public ToStringMethodGenerator addMember(
            String name,
            TypeName type,
            TypeName unsignedType)
        {
            formats.add(String.format("%s=%%%s", name, type.isPrimitive() ? "d" : "s"));
            if (type instanceof ClassName && "StringFW".equals(((ClassName) type).simpleName()))
            {
                args.add(String.format("%sRO.asString()", name));
            }
            else
            {
                args.add(String.format("%s()", name));
            }
            return this;
        }

        @Override
        public MethodSpec generate()
        {
            String typeName = constant(baseName);
            if (formats.isEmpty())
            {
                builder.addStatement("return $S", typeName);
            }
            else
            {
                String format = String.format("%s [%s]", typeName, String.join(", ", formats));
                builder.addStatement("return String.format($S, $L)", format, String.join(", ", args));
            }
            return builder.build();
        }

    }

    private static final class BuilderClassGenerator extends ClassSpecGenerator
    {
        private final TypeSpec.Builder builder;
        private final ClassName structType;
        private final MemberFieldGenerator memberField;
        private final MemberAccessorGenerator memberAccessor;
        private final MemberMutatorGenerator memberMutator;
        private final WrapMethodGenerator wrapMethod;

        private BuilderClassGenerator(
            ClassName structType,
            ClassName flyweightType)
        {
            this(structType.nestedClass("Builder"), flyweightType.nestedClass("Builder"), structType);
        }

        private BuilderClassGenerator(
            ClassName thisType,
            ClassName builderRawType,
            ClassName structType)
        {
            super(thisType);
            this.builder = classBuilder(thisType.simpleName())
                    .addModifiers(PUBLIC, STATIC, FINAL)
                    .superclass(ParameterizedTypeName.get(builderRawType, structType));
            this.structType = structType;
            this.wrapMethod = new WrapMethodGenerator();
            this.memberField = new MemberFieldGenerator(thisType, builder);
            this.memberAccessor = new MemberAccessorGenerator(thisType, builder);
            this.memberMutator = new MemberMutatorGenerator(thisType, builder);
        }

        private void addMember(
            String name,
            TypeName type,
            TypeName unsignedType)
        {
            // TODO: eliminate need for lookahead
            memberMutator.lookaheadMember(name, type, unsignedType);

            memberField.addMember(name, type, unsignedType);
            memberAccessor.addMember(name, type, unsignedType);
            memberMutator.addMember(name, type, unsignedType);
            wrapMethod.addMember(name, type, unsignedType);
        }

        @Override
        public TypeSpec generate()
        {
            memberField.build();
            memberAccessor.build();
            memberMutator.build();
            return builder.addMethod(constructor())
                          .addMethod(wrapMethod.generate())
                          .build();
        }

        private MethodSpec constructor()
        {
            return constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addStatement("super(new $T())", structType)
                    .build();
        }

        private static final class MemberFieldGenerator extends ClassSpecMixinGenerator
        {
            private MemberFieldGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
            {
                super(thisType, builder);
            }

            public MemberFieldGenerator addMember(
                String name,
                TypeName type,
                TypeName unsignedType)
            {
                if (!type.isPrimitive())
                {
                    String fieldRW = String.format("%sRW", name);

                    if (TypeNames.DIRECT_BUFFER_TYPE.equals(type))
                    {
                        // skip
                    }
                    else if (type instanceof ParameterizedTypeName)
                    {
                        ParameterizedTypeName parameterizedType = (ParameterizedTypeName) type;
                        ClassName rawType = parameterizedType.rawType;
                        ClassName itemType = (ClassName) parameterizedType.typeArguments.get(0);
                        ClassName builderRawType = rawType.nestedClass("Builder");
                        ClassName itemBuilderType = itemType.nestedClass("Builder");
                        ParameterizedTypeName builderType = ParameterizedTypeName.get(builderRawType, itemBuilderType, itemType);

                        builder.addField(FieldSpec.builder(builderType, fieldRW, PRIVATE, FINAL)
                                .initializer("new $T(new $T(), new $T())", builderType, itemBuilderType, itemType)
                                .build());
                    }
                    else if (type instanceof ClassName)
                    {
                        ClassName classType = (ClassName) type;
                        TypeName builderType = classType.nestedClass("Builder");
                        builder.addField(FieldSpec.builder(builderType, fieldRW, PRIVATE, FINAL)
                                .initializer("new $T()", builderType)
                                .build());
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unsupported member type: " + type);
                    }
                }
                return this;
            }
        }

        private static final class MemberAccessorGenerator extends ClassSpecMixinGenerator
        {
            private String anchorName;

            private MemberAccessorGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
            {
                super(thisType, builder);
            }

            public MemberAccessorGenerator addMember(
                String name,
                TypeName type,
                TypeName unsignedType)
            {
                if (!type.isPrimitive())
                {
                    if (TypeNames.DIRECT_BUFFER_TYPE.equals(type))
                    {
                        // skip
                    }
                    else if (type instanceof ParameterizedTypeName)
                    {
                        ParameterizedTypeName parameterizedType = (ParameterizedTypeName) type;
                        ClassName rawType = parameterizedType.rawType;
                        ClassName itemType = (ClassName) parameterizedType.typeArguments.get(0);
                        ClassName builderRawType = rawType.nestedClass("Builder");
                        ClassName itemBuilderType = itemType.nestedClass("Builder");
                        ParameterizedTypeName builderType = ParameterizedTypeName.get(builderRawType, itemBuilderType, itemType);

                        builder.addMethod(methodBuilder(name)
                                .addModifiers(PRIVATE)
                                .addParameter(int.class, "offset")
                                .addStatement("return $LRW.wrap(buffer(), offset, maxLimit())", name)
                                .returns(builderType)
                                .build());
                    }
                    else if (type instanceof ClassName)
                    {
                        ClassName classType = (ClassName) type;
                        TypeName builderType = classType.nestedClass("Builder");

                        CodeBlock.Builder code = CodeBlock.builder();
                        if (anchorName != null)
                        {
                            code.addStatement("return $LRW.wrap(buffer(), $L().build().limit() + $L, maxLimit())",
                                    name, anchorName, offset(name));
                        }
                        else
                        {
                            code.addStatement("return $LRW.wrap(buffer(), offset() + $L, maxLimit())", name, offset(name));
                        }

                        builder.addMethod(methodBuilder(name)
                                .addModifiers(PRIVATE)
                                .addCode(code.build())
                                .returns(builderType)
                                .build());
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unsupported member type: " + type);
                    }

                    anchorName = name;
                }

                return this;
            }
        }

        private static final class MemberMutatorGenerator extends ClassSpecMixinGenerator
        {
            private static final Map<TypeName, String> PUTTER_NAMES;

            static
            {
                Map<TypeName, String> putterNames = new HashMap<>();
                putterNames.put(TypeName.BYTE, "putByte");
                putterNames.put(TypeName.CHAR, "putChar");
                putterNames.put(TypeName.SHORT, "putShort");
                putterNames.put(TypeName.FLOAT, "putFloat");
                putterNames.put(TypeName.INT, "putInt");
                putterNames.put(TypeName.DOUBLE, "putDouble");
                putterNames.put(TypeName.LONG, "putLong");
                PUTTER_NAMES = unmodifiableMap(putterNames);
            }

            private String anchorName;
            private TypeName anchorType;
            private String nextName;
            private TypeName nextType;
            private String deferredName;
            private TypeName deferredType;
            private TypeName deferredUnsignedType;

            private MemberMutatorGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
            {
                super(thisType, builder);
            }

            public void lookaheadMember(
                String name,
                TypeName type,
                TypeName unsignedType)
            {
                nextName = name;
                nextType = type;
                addDeferredMemberIfNecessary();
            }

            public MemberMutatorGenerator addMember(
                String name,
                TypeName type,
                TypeName unsignedType)
            {
                deferredName = name;
                deferredType = type;
                deferredUnsignedType = unsignedType;
                return this;
            }

            @Override
            public TypeSpec.Builder build()
            {
                addDeferredMemberIfNecessary();
                return super.build();
            }

            private MemberMutatorGenerator addDeferredMemberIfNecessary()
            {
                if (deferredName == null || deferredType == null)
                {
                    return this;
                }

                String name = deferredName;
                TypeName type = deferredType;
                TypeName unsignedType = deferredUnsignedType;

                deferredName = null;
                deferredType = null;
                deferredUnsignedType = null;

                if (type.isPrimitive())
                {
                    addPrimitiveMember(name, type, unsignedType);
                }
                else
                {
                    addNonPrimitiveMember(name, type);
                }

                return this;
            }

            private void addPrimitiveMember(
                String name,
                TypeName type,
                TypeName unsignedType)
            {
                String putterName = PUTTER_NAMES.get(type);
                if (putterName == null)
                {
                    throw new IllegalStateException("member type not supported: " + type);
                }

                if (anchorName != null)
                {
                    TypeName publicType = (unsignedType != null) ? unsignedType : type;
                    String statement = DIRECT_BUFFER_TYPE.equals(anchorType)
                            ? String.format("$L(offset -> { buffer().%s(offset() + $L, ", putterName)
                            : String.format("buffer().%s($L().build().limit() + $L, ", putterName);

                    CodeBlock.Builder code = CodeBlock.builder()
                            .add("$[")
                            .add(statement, anchorName, offset(name));

                    if (publicType != type)
                    {
                        code.add("($T)", type);

                        if (type == TypeName.BYTE)
                        {
                            code.add("(value & 0xFF))", ByteOrder.class);
                        }
                        else if (type == TypeName.SHORT)
                        {
                            code.add("(value & 0xFFFF), $T.BIG_ENDIAN)", ByteOrder.class);
                        }
                        else if (type == TypeName.INT)
                        {
                            code.add("(value & 0xFFFF_FFFF), $T.BIG_ENDIAN)", ByteOrder.class);
                        }
                        else
                        {
                            code.add("value)");
                        }
                    }
                    else
                    {
                        code.add("value)");
                    }

                    if (DIRECT_BUFFER_TYPE.equals(anchorType))
                    {
                        code.add("; return $L + $L; });\n$]", offset(name), size(name))
                            .addStatement("return this");
                    }
                    else
                    {
                        code.add(";\n$]");

                        if (nextType instanceof ParameterizedTypeName)
                        {
                            code.addStatement("$L($L().build().limit() + $L + $L)",
                                    nextName, anchorName, offset(name), size(name));
                        }

                        code.addStatement("limit($L().build().limit() + $L + $L)", anchorName, offset(name), size(name))
                            .addStatement("return this");
                    }

                    builder.addMethod(methodBuilder(name)
                            .addModifiers(PUBLIC)
                            .returns(thisType)
                            .addParameter(publicType, "value")
                            .addCode(code.build())
                            .build());
                }
                else
                {
                    String statement = String.format("buffer().%s(offset() + $L, value)", putterName);

                    CodeBlock.Builder code = CodeBlock.builder()
                            .addStatement(statement, offset(name));

                    if (nextType instanceof ParameterizedTypeName)
                    {
                        code.addStatement("$L(offset() + $L + $L)", nextName, offset(name), size(name));
                        code.addStatement("limit(offset() + $L + $L)", offset(name), size(name));
                    }

                    code.addStatement("return this");

                    builder.addMethod(methodBuilder(name)
                            .addModifiers(PUBLIC)
                            .addParameter(type, "value")
                            .returns(thisType)
                            .addCode(code.build())
                            .build());
                }
            }

            private void addNonPrimitiveMember(
                String name,
                TypeName type)
            {
                if (type instanceof ClassName)
                {
                    ClassName className = (ClassName) type;
                    addClassType(name, className);
                }
                else if (type instanceof ParameterizedTypeName)
                {
                    ParameterizedTypeName parameterizedType = (ParameterizedTypeName) type;
                    addParameterizedType(name, parameterizedType);
                }
                else
                {
                    builder.addMethod(methodBuilder(name)
                            .addModifiers(PUBLIC)
                            .returns(thisType)
                            .addParameter(type, "value")
                            .addStatement("$LRW.set(value)", name)
                            .addStatement("return this")
                            .build());
                }

                anchorName = name;
                anchorType = type;
            }

            private void addClassType(
                String name,
                ClassName className)
            {
                if ("StringFW".equals(className.simpleName()))
                {
                    CodeBlock.Builder codeBlock = CodeBlock.builder();

                    // TODO: handle optional fields
                    if (anchorName != null)
                    {
                        codeBlock.beginControlFlow("if (value == null)")
                            .addStatement("limit($L().build().limit() + $L)", anchorName, offset(name))
                            .nextControlFlow("else")
                            .addStatement("$L().set(value, $T.UTF_8)", name, StandardCharsets.class);

                        if (nextType instanceof ParameterizedTypeName)
                        {
                            codeBlock.addStatement("$L($L().build().limit())", nextName, name);
                        }

                        codeBlock.addStatement("limit($L().build().limit())", name)
                            .endControlFlow()
                            .addStatement("return this");
                    }
                    else
                    {
                        codeBlock.beginControlFlow("if (value == null)")
                            .addStatement("limit(offset() + $L)", offset(name))
                            .nextControlFlow("else")
                            .addStatement("$L().set(value, $T.UTF_8)", name, StandardCharsets.class);

                        if (nextType instanceof ParameterizedTypeName)
                        {
                            codeBlock.addStatement("$L($L().build().limit())", nextName, name);
                        }

                        codeBlock.addStatement("limit($L().build().limit())", name)
                            .endControlFlow()
                            .addStatement("return this");
                    }

                    builder.addMethod(methodBuilder(name)
                            .addModifiers(PUBLIC)
                            .returns(thisType)
                            .addParameter(String.class, "value")
                            .addCode(codeBlock.build())
                            .build());
                }
                else if (DIRECT_BUFFER_TYPE.equals(className))
                {
                    addDirectBufferType(name);
                }
                else
                {
                    ClassName consumerType = ClassName.get(Consumer.class);
                    ClassName builderType = className.nestedClass("Builder");
                    TypeName mutatorType = ParameterizedTypeName.get(consumerType, builderType);

                    builder.addMethod(methodBuilder(name)
                            .addModifiers(PUBLIC)
                            .returns(thisType)
                            .addParameter(mutatorType, "mutator")
                            .addStatement("mutator.accept($L())", name)
                            .addStatement("limit($L().build().limit())", name)
                            .addStatement("return this")
                            .build());

                }
            }

            private void addDirectBufferType(
                String name)
            {
                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(IntUnaryOperator.class, "mutator")
                        .addParameter(IntConsumer.class, "error")
                        .returns(thisType)
                        .addStatement("int length = mutator.applyAsInt(offset() + $L)", offset(name))
                        .beginControlFlow("if (length < 0)")
                        .addStatement("error.accept(length)")
                        .addStatement("limit(offset() + $L)", offset(name))
                        .nextControlFlow("else")
                        .addStatement("limit(offset() + $L + length)", offset(name))
                        .endControlFlow()
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(IntUnaryOperator.class, "mutator")
                        .returns(thisType)
                        .addStatement("int length = mutator.applyAsInt(offset() + $L)", offset(name))
                        .beginControlFlow("if (length < 0)")
                        .addStatement("throw new IllegalStateException()")
                        .endControlFlow()
                        .addStatement("limit(offset() + $L + length)", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(DIRECT_BUFFER_TYPE, "value")
                        .addParameter(int.class, "offset")
                        .addParameter(int.class, "length")
                        .returns(thisType)
                        .addStatement("buffer().putBytes(offset() + $L, value, offset, length)", offset(name))
                        .addStatement("limit(offset() + $L + length)", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(DIRECT_BUFFER_TYPE, "value")
                        .returns(thisType)
                        .addStatement("buffer().putBytes(offset() + $L, value, 0, value.capacity())", offset(name))
                        .addStatement("limit(offset() + $L + value.capacity())", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(BYTE_ARRAY, "value")
                        .addParameter(int.class, "offset")
                        .addParameter(int.class, "length")
                        .returns(thisType)
                        .addStatement("buffer().putBytes(offset() + $L, value, offset, length)", offset(name))
                        .addStatement("limit(offset() + $L + length)", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(BYTE_ARRAY, "value")
                        .returns(thisType)
                        .addStatement("buffer().putBytes(offset() + $L, value, 0, value.length)", offset(name))
                        .addStatement("limit(offset() + $L + value.length)", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(String.class, "value")
                        .returns(thisType)
                        .addStatement("int length = buffer().putStringWithoutLengthUtf8(offset() + $L, value)", offset(name))
                        .addStatement("limit(offset() + $L + length)", offset(name))
                        .addStatement("return this")
                        .build());
            }

            private void addParameterizedType(
                String name,
                ParameterizedTypeName parameterizedType)
            {
                ClassName rawType = parameterizedType.rawType;
                ClassName itemType = (ClassName) parameterizedType.typeArguments.get(0);
                ClassName builderRawType = rawType.nestedClass("Builder");
                ClassName itemBuilderType = itemType.nestedClass("Builder");
                ParameterizedTypeName builderType = ParameterizedTypeName.get(builderRawType, itemBuilderType, itemType);

                ClassName consumerType = ClassName.get(Consumer.class);
                TypeName mutatorType = ParameterizedTypeName.get(consumerType, builderType);

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .returns(thisType)
                        .addParameter(mutatorType, "mutator")
                        .addStatement("mutator.accept($LRW)", name)
                        .addStatement("super.limit($LRW.limit())", name)
                        .addStatement("return this")
                        .build());
            }
        }

        private final class WrapMethodGenerator extends MethodSpecGenerator
        {
            private String anchorName;

            private WrapMethodGenerator()
            {
                super(methodBuilder("wrap")
                        .addModifiers(PUBLIC)
                        .returns(thisName)
                        .addParameter(MUTABLE_DIRECT_BUFFER_TYPE, "buffer")
                        .addParameter(int.class, "offset")
                        .addParameter(int.class, "maxLimit")
                        .addStatement("super.wrap(buffer, offset, maxLimit)"));
            }

            public WrapMethodGenerator addMember(
                String name,
                TypeName type,
                TypeName unsignedType)
            {
                if (!type.isPrimitive())
                {
                    if (DIRECT_BUFFER_TYPE.equals(type))
                    {
                        // skip
                    }
                    else if (anchorName == null)
                    {
                        builder.addStatement("$LRW.wrap(buffer, offset + $L, maxLimit)", name, offset(name));
                    }

                    anchorName = name;
                }
                return this;
            }

            @Override
            public MethodSpec generate()
            {
                return builder.addStatement("return this")
                              .build();
            }

        }
    }

    private static String offset(
        String fieldName)
    {
        return String.format("FIELD_OFFSET_%s", constant(fieldName));
    }

    private static String size(
        String fieldName)
    {
        return String.format("FIELD_SIZE_%s", constant(fieldName));
    }

    private static String constant(
        String fieldName)
    {
        return fieldName.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toUpperCase();
    }
}
