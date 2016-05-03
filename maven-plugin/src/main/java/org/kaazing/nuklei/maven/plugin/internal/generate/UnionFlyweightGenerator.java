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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
import com.squareup.javapoet.TypeVariableName;

@SuppressWarnings("squid:S1192")
public final class UnionFlyweightGenerator extends ClassSpecGenerator
{
    private final String baseName;
    private final TypeSpec.Builder builder;
    private final MemberFieldGenerator memberField;
    private final KindConstantGenerator memberKindConstant;
    private final MemberSizeConstantGenerator memberSizeConstant;
    private final MemberOffsetConstantGenerator memberOffsetConstant;
    private final KindAccessorGenerator kindAccessor;
    private final MemberAccessorGenerator memberAccessor;
    private final WrapMethodGenerator wrapMethod;
    private final LimitMethodGenerator limitMethod;
    private final ToStringMethodGenerator toStringMethod;
    private final BuilderClassGenerator builderClass;

    public UnionFlyweightGenerator(
        ClassName structName,
        ClassName flyweightName,
        String baseName)
    {
        super(structName);

        this.baseName = baseName;
        this.builder = classBuilder(structName).superclass(flyweightName).addModifiers(PUBLIC, FINAL)
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "nuklei").build());
        this.memberKindConstant = new KindConstantGenerator(structName, builder);
        this.memberSizeConstant = new MemberSizeConstantGenerator(structName, builder);
        this.memberOffsetConstant = new MemberOffsetConstantGenerator(structName, builder);
        this.memberField = new MemberFieldGenerator(structName, builder);
        this.kindAccessor = new KindAccessorGenerator(structName, builder);
        this.memberAccessor = new MemberAccessorGenerator(structName, flyweightName.nestedClass("Visitor"), builder);
        this.wrapMethod = new WrapMethodGenerator();
        this.limitMethod = new LimitMethodGenerator();
        this.toStringMethod = new ToStringMethodGenerator();
        this.builderClass = new BuilderClassGenerator(structName, flyweightName);
    }

    public UnionFlyweightGenerator addMember(
        int value,
        String name,
        TypeName type,
        TypeName unsignedType,
        int size)
    {
        memberKindConstant.addMember(value, name);
        memberOffsetConstant.addMember(name);
        memberSizeConstant.addMember(name, type, size);
        memberField.addMember(name, type);
        memberAccessor.addMember(name, type, unsignedType);
        wrapMethod.addMember(name, type);
        limitMethod.addMember(name, type);
        toStringMethod.addMember(name, type);
        builderClass.addMember(name, type, size);
        return this;
    }

    @Override
    public TypeSpec generate()
    {
        memberKindConstant.build();
        memberOffsetConstant.build();
        memberSizeConstant.build();
        memberField.build();
        kindAccessor.build();
        memberAccessor.build();

        return builder.addMethod(wrapMethod.generate())
                      .addMethod(limitMethod.generate())
                      .addMethod(toStringMethod.generate())
                      .addType(builderClass.generate())
                      .build();
    }

    private static final class KindConstantGenerator extends ClassSpecMixinGenerator
    {
        private KindConstantGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public KindConstantGenerator addMember(
            int value,
            String name)
        {
            builder.addField(
                    FieldSpec.builder(int.class, kind(name), PUBLIC, STATIC, FINAL)
                             .initializer("$L", value)
                             .build());

            return this;
        }
    }

    private static final class MemberSizeConstantGenerator extends ClassSpecMixinGenerator
    {
        private static final Map<TypeName, String> SIZEOF_BY_NAME = initSizeofByName();

        private MemberSizeConstantGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder.addField(
                    FieldSpec.builder(int.class, size("kind"), PRIVATE, STATIC, FINAL)
                    .initializer("$T.SIZE_OF_BYTE", BIT_UTIL_TYPE)
                    .build()));
        }

        public MemberSizeConstantGenerator addMember(
            String name,
            TypeName type,
            int size)
        {
            if (type.isPrimitive())
            {
                builder.addField(
                        FieldSpec.builder(int.class, size(name), PRIVATE, STATIC, FINAL)
                                 .initializer("$T.SIZE_OF_$L", BIT_UTIL_TYPE, SIZEOF_BY_NAME.get(type))
                                 .build());
            }
            else if (size > 0)
            {
                builder.addField(
                        FieldSpec.builder(int.class, size(name), PRIVATE, STATIC, FINAL)
                                 .initializer("$L", size)
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
        private MemberOffsetConstantGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder.addField(
                    FieldSpec.builder(int.class, offset("kind"), PRIVATE, STATIC, FINAL)
                    .initializer("0")
                    .build()));
        }

        public MemberOffsetConstantGenerator addMember(
            String name)
        {
            builder.addField(
                    FieldSpec.builder(int.class, offset(name), PRIVATE, STATIC, FINAL)
                             .initializer(String.format("%s + %s", offset("kind"), size("kind")))
                             .build());
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
            TypeName type)
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

    private static final class KindAccessorGenerator extends ClassSpecMixinGenerator
    {
        private KindAccessorGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        @Override
        public TypeSpec.Builder build()
        {
            builder.addMethod(methodBuilder("kind")
                    .addModifiers(PUBLIC)
                    .returns(int.class)
                    .addStatement("return buffer().getByte(offset() + $L) & 0xFF", offset("kind"))
                    .build());

            return super.build();
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

        private final ClassName visitorRawType;

        private MemberAccessorGenerator(
            ClassName thisType,
            ClassName visitorRawType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
            this.visitorRawType = visitorRawType;
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

                codeBlock.add("buffer().$L(offset() + $L", getterName, offset(name));

                if (publicType != type)
                {
                    if (type == TypeName.BYTE)
                    {
                        codeBlock.add(", $T.BIG_ENDIAN) & 0xFF)", ByteOrder.class);
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
                    TypeVariableName typeVarT = TypeVariableName.get("T");
                    ParameterizedTypeName visitorType = ParameterizedTypeName.get(visitorRawType, typeVarT);

                    builder.addMethod(methodBuilder(name)
                            .addTypeVariable(typeVarT)
                            .addModifiers(PUBLIC)
                            .addParameter(visitorType, "visitor")
                            .returns(typeVarT)
                            .addStatement("return visitor.visit($LRO, 0, $L)", name, size(name))
                            .build());
                }

                codeBlock.addStatement("return $LRO", name);
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
        private LimitMethodGenerator()
        {
            super(methodBuilder("limit")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .returns(int.class)
                    .beginControlFlow("switch (kind())"));
        }

        public LimitMethodGenerator addMember(
            String name,
            TypeName type)
        {
            builder.beginControlFlow("case $L:", kind(name));

            if (DIRECT_BUFFER_TYPE.equals(type))
            {
                builder.addStatement("return offset() + $L + $L", offset(name), size(name));
            }
            else
            {
                builder.addStatement("return $L().limit()", name);
            }

            builder.endControlFlow();

            return this;
        }

        @Override
        public MethodSpec generate()
        {
            return builder.beginControlFlow("default:")
                          .addStatement("return offset()")
                          .endControlFlow()
                          .endControlFlow()
                          .build();
        }

    }

    private final class WrapMethodGenerator extends MethodSpecGenerator
    {
        private WrapMethodGenerator()
        {
            super(methodBuilder("wrap")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(DIRECT_BUFFER_TYPE, "buffer")
                    .addParameter(int.class, "offset")
                    .addParameter(int.class, "maxLimit")
                    .returns(thisName)
                    .addStatement("super.wrap(buffer, offset, maxLimit)")
                    .beginControlFlow("switch (kind())"));
        }

        public WrapMethodGenerator addMember(
            String name,
            TypeName type)
        {
            builder.beginControlFlow("case $L:", kind(name));

            if (DIRECT_BUFFER_TYPE.equals(type))
            {
                builder.addStatement("$LRO.wrap(buffer, offset + $L, $L)",
                        name, offset(name), size(name));
            }
            else if (!type.isPrimitive())
            {
                builder.addStatement("$LRO.wrap(buffer, offset + $L, maxLimit)", name, offset(name));
            }

            builder.addStatement("break").endControlFlow();

            return this;
        }

        @Override
        public MethodSpec generate()
        {
            return builder.beginControlFlow("default:")
                          .addStatement("break")
                          .endControlFlow()
                          .endControlFlow()
                          .addStatement("checkLimit(limit(), maxLimit)")
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
            TypeName type)
        {
            formats.add(String.format("%s=%%%s", name, type.isPrimitive() ? "d" : "s"));
            if (type instanceof ClassName && "StringFW".equals(((ClassName) type).simpleName()))
            {
                args.add(String.format("%sRO.asString()", name));
            }
            else
            {
                args.add(String.format("%s((Visitor<String>)(b, o, l) -> \"TODO\")", name));
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
            int size)
        {
            // TODO: eliminate need for lookahead
            memberMutator.lookaheadMember(name, type);

            memberField.addMember(name, type);
            memberAccessor.addMember(name, type, size);
            memberMutator.addMember(name, type);

            //   setMethod
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
                TypeName type)
            {
                if (!type.isPrimitive())
                {
                    String fieldRW = String.format("%sRW", name);

                    if (TypeNames.DIRECT_BUFFER_TYPE.equals(type))
                    {
                        builder.addField(FieldSpec.builder(MUTABLE_DIRECT_BUFFER_TYPE, fieldRW, PRIVATE, FINAL)
                                .initializer("new $T(new byte[0])", UNSAFE_BUFFER_TYPE)
                                .build());
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
            private MemberAccessorGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
            {
                super(thisType, builder);
            }

            public MemberAccessorGenerator addMember(
                String name,
                TypeName type,
                int size)
            {
                if (!type.isPrimitive())
                {
                    if (TypeNames.DIRECT_BUFFER_TYPE.equals(type))
                    {
                        String limit = (size > 0) ? size(name) : "maxLimit()";
                        builder.addMethod(methodBuilder(name)
                                .addModifiers(PRIVATE)
                                .addStatement("$LRW.wrap(buffer(), offset() + $L, $L)", name, offset(name), limit)
                                .addStatement("return $LRW", name)
                                .returns(MUTABLE_DIRECT_BUFFER_TYPE)
                                .build());

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

                        builder.addMethod(methodBuilder(name)
                                .addModifiers(PRIVATE)
                                .addStatement("return $LRW.wrap(buffer(), offset() + $L, maxLimit())", name, offset(name))
                                .returns(builderType)
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

            private String nextName;
            private TypeName nextType;

            private String deferredName;
            private TypeName deferredType;

            private MemberMutatorGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
            {
                super(thisType, builder.addMethod(
                        MethodSpec.methodBuilder("kind")
                        .addModifiers(PRIVATE)
                        .addParameter(int.class, "value")
                        .returns(thisType)
                        .addStatement("buffer().putByte(offset() + $L, (byte)(value & 0xFF))", offset("kind"))
                        .addStatement("return this")
                        .build()));
            }

            public void lookaheadMember(
                String name,
                TypeName type)
            {
                nextName = name;
                nextType = type;
                addDeferredMemberIfNecessary();
            }

            public MemberMutatorGenerator addMember(
                String name,
                TypeName type)
            {
                deferredName = name;
                deferredType = type;
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

                deferredName = null;
                deferredType = null;

                if (type.isPrimitive())
                {
                    addPrimitiveMember(name, type);
                }
                else
                {
                    addNonPrimitiveMember(name, type);
                }

                return this;
            }

            private void addPrimitiveMember(
                String name,
                TypeName type)
            {
                String putterName = PUTTER_NAMES.get(type);
                if (putterName == null)
                {
                    throw new IllegalStateException("member type not supported: " + type);
                }

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
                            .addStatement("kind($L)", kind(name))
                            .addStatement("$L().set(value)", name)
                            .addStatement("return this")
                            .build());
                }
            }

            private void addClassType(
                String name,
                ClassName className)
            {
                if ("StringFW".equals(className.simpleName()))
                {
                    CodeBlock.Builder codeBlock = CodeBlock.builder();

                    // TODO: handle optional fields
                    codeBlock.beginControlFlow("if (value == null)")
                        .addStatement("limit(offset() + $L)", offset(name))
                        .nextControlFlow("else")
                        .addStatement("kind($L)", kind(name))
                        .addStatement("$L().set(value, $T.UTF_8)", name, StandardCharsets.class);

                    if (nextType instanceof ParameterizedTypeName)
                    {
                        codeBlock.addStatement("$L($L().build().limit())", nextName, name);
                    }

                    codeBlock.addStatement("limit($L().build().limit())", name)
                        .endControlFlow()
                        .addStatement("return this");

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
                            .addStatement("kind($L)", kind(name))
                            .addStatement("mutator.accept($L())", name)
                            .addStatement("limit($L().build().limit())", name)
                            .addStatement("return this")
                            .build());

                }
            }

            private void addDirectBufferType(
                String name)
            {
                TypeVariableName typeVarT = TypeVariableName.get("T");
                ClassName biconsumerRawType = ClassName.get(BiConsumer.class);
                ParameterizedTypeName consumerType = ParameterizedTypeName.get(Consumer.class, byte[].class);
                ParameterizedTypeName converterType = ParameterizedTypeName.get(biconsumerRawType, typeVarT, consumerType);

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addTypeVariable(typeVarT)
                        .addParameter(typeVarT, "value")
                        .addParameter(converterType, "converter")
                        .returns(thisType)
                        .addStatement("converter.accept(value, this::$L)", name)
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(DIRECT_BUFFER_TYPE, "value")
                        .addParameter(int.class, "offset")
                        .addParameter(int.class, "length")
                        .returns(thisType)
                        .addStatement("kind($L)", kind(name))
                        .addStatement("$L().putBytes(0, value, offset, length)", name)
                        .addStatement("limit(offset() + $L + length)", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(DIRECT_BUFFER_TYPE, "value")
                        .returns(thisType)
                        .addStatement("kind($L)", kind(name))
                        .addStatement("$L().putBytes(0, value, 0, value.capacity())", name)
                        .addStatement("limit(offset() + $L + value.capacity())", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(BYTE_ARRAY, "value")
                        .addParameter(int.class, "offset")
                        .addParameter(int.class, "length")
                        .returns(thisType)
                        .addStatement("kind($L)", kind(name))
                        .addStatement("$L().putBytes(0, value, offset, length)", name)
                        .addStatement("limit(offset() + $L + length)", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(BYTE_ARRAY, "value")
                        .returns(thisType)
                        .addStatement("kind($L)", kind(name))
                        .addStatement("$L().putBytes(0, value, 0, value.length)", name)
                        .addStatement("limit(offset() + $L + value.length)", offset(name))
                        .addStatement("return this")
                        .build());

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(String.class, "value")
                        .returns(thisType)
                        .addStatement("kind($L)", kind(name))
                        .addStatement("int length = buffer().putStringWithoutLengthUtf8(offset() + $L, value)", offset(name))
                        .addStatement("limit(offset() + $L + length)", offset(name))
                        .addStatement("return this")
                        .build());
            }

            private void addParameterizedType(
                String name,
                ParameterizedTypeName parameterizedType)
            {
                ClassName consumerType = ClassName.get(Consumer.class);
                ClassName itemType = (ClassName) parameterizedType.typeArguments.get(0);
                ClassName itemBuilderType = itemType.nestedClass("Builder");
                TypeName mutatorType = ParameterizedTypeName.get(consumerType, itemBuilderType);

                builder.addMethod(methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .returns(thisType)
                        .addParameter(mutatorType, "mutator")
                        .addStatement("kind($L)", kind(name))
                        .addStatement("$L().item(mutator)", name)
                        .addStatement("super.limit($LRW.limit())", name)
                        .addStatement("return this")
                        .build());
            }
        }

        private final class WrapMethodGenerator extends MethodSpecGenerator
        {
            private WrapMethodGenerator()
            {
                super(methodBuilder("wrap")
                        .addModifiers(PUBLIC)
                        .returns(thisName)
                        .addParameter(MUTABLE_DIRECT_BUFFER_TYPE, "buffer")
                        .addParameter(int.class, "offset")
                        .addParameter(int.class, "maxLimit")
                        .addStatement("super.wrap(buffer, offset, maxLimit)")
                        .addStatement("return this"));
            }

            @Override
            public MethodSpec generate()
            {
                return builder.build();
            }

        }
    }

    private static String kind(
        String fieldName)
    {
        return String.format("KIND_%s", constant(fieldName));
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
