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
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.BIT_UTIL_TYPE;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.DIRECT_BUFFER_TYPE;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.MUTABLE_DIRECT_BUFFER_TYPE;

import javax.annotation.Generated;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

@SuppressWarnings("squid:S1192")
public final class OctetsFlyweightGenerator extends ClassSpecGenerator
{
    private final ClassName visitorRawType;
    private final TypeSpec.Builder classBuilder;
    private final BuilderClassBuilder builderClassBuilder;

    public OctetsFlyweightGenerator(
        ClassName flyweightType)
    {
        super(flyweightType.peerClass("OctetsFW"));

        this.visitorRawType = flyweightType.nestedClass("Visitor");
        this.classBuilder = classBuilder(thisName).superclass(flyweightType).addModifiers(PUBLIC, FINAL)
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "nuklei").build());
        this.builderClassBuilder = new BuilderClassBuilder(thisName, flyweightType);
    }

    @Override
    public TypeSpec generate()
    {
        return classBuilder.addField(fieldOffsetLengthConstant())
                            .addField(fieldSizeLengthConstant())
                            .addMethod(getMethod())
                            .addMethod(limitMethod())
                            .addMethod(wrapMethod())
                            .addMethod(toStringMethod())
                            .addMethod(length0Method())
                            .addType(builderClassBuilder.build())
                            .build();
    }


    private FieldSpec fieldOffsetLengthConstant()
    {
        return FieldSpec.builder(int.class, "FIELD_OFFSET_LENGTH", PRIVATE, STATIC, FINAL)
                .initializer("0")
                .build();
    }

    private FieldSpec fieldSizeLengthConstant()
    {
        return FieldSpec.builder(int.class, "FIELD_SIZE_LENGTH", PRIVATE, STATIC, FINAL)
                .initializer("$T.SIZE_OF_BYTE", BIT_UTIL_TYPE)
                .build();
    }

    private MethodSpec getMethod()
    {
        TypeVariableName typeVarT = TypeVariableName.get("T");
        TypeName visitorType = ParameterizedTypeName.get(visitorRawType, typeVarT);

        return methodBuilder("get")
                .addModifiers(PUBLIC)
                .addTypeVariable(typeVarT)
                .addParameter(visitorType, "visitor")
                .returns(typeVarT)
                .addStatement("int length = buffer().getByte(offset() + FIELD_OFFSET_LENGTH) & 0xFF")
                .addStatement("return visitor.visit(buffer(), offset() + FIELD_SIZE_LENGTH, length)")
                .build();
    }

    private MethodSpec limitMethod()
    {
        return methodBuilder("limit")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(int.class)
                .addStatement("return maxLimit() == offset() ? offset() : offset() + FIELD_SIZE_LENGTH + length0()")
                .build();
    }

    private MethodSpec wrapMethod()
    {
        return methodBuilder("wrap")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(DIRECT_BUFFER_TYPE, "buffer")
                .addParameter(int.class, "offset")
                .addParameter(int.class, "maxLimit")
                .returns(thisName)
                .addStatement("super.wrap(buffer, offset, maxLimit)")
                .addStatement("checkLimit(limit(), maxLimit)")
                .addStatement("return this")
                .build();
    }

    private MethodSpec toStringMethod()
    {
        return methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(String.class)
                .addStatement("return maxLimit() == offset() ? \"null\" : String.format(\"length=%d\", length0())")
                .build();
    }

    private MethodSpec length0Method()
    {
        return methodBuilder("length0")
                .addModifiers(PRIVATE)
                .returns(int.class)
                .addStatement("return buffer().getByte(offset() + FIELD_OFFSET_LENGTH) & 0xFF")
                .build();
    }

    private static final class BuilderClassBuilder
    {
        private final TypeSpec.Builder classBuilder;
        private final ClassName classType;
        private final ClassName octetsType;
        private final ClassName visitorType;

        private BuilderClassBuilder(
            ClassName octetsType,
            ClassName flyweightType)
        {
            ClassName builderRawType = flyweightType.nestedClass("Builder");
            TypeName builderType = ParameterizedTypeName.get(builderRawType, octetsType);

            this.octetsType = octetsType;
            this.classType = octetsType.nestedClass("Builder");
            this.visitorType = builderRawType.nestedClass("Visitor");
            this.classBuilder = classBuilder(classType.simpleName())
                    .addModifiers(PUBLIC, STATIC, FINAL)
                    .superclass(builderType);
        }

        public TypeSpec build()
        {
            return classBuilder.addMethod(constructor())
                    .addMethod(wrapMethod())
                    .addMethod(setMethod())
                    .addMethod(setMethodViaBuffer())
                    .addMethod(setMethodViaByteArray())
                    .addMethod(setMethodViaMutator())
                    .build();
        }

        private MethodSpec constructor()
        {
            return constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addStatement("super(new $T())", octetsType)
                    .build();
        }

        private MethodSpec wrapMethod()
        {
            return methodBuilder("wrap")
                    .addModifiers(PUBLIC)
                    .returns(octetsType.nestedClass("Builder"))
                    .addParameter(MUTABLE_DIRECT_BUFFER_TYPE, "buffer")
                    .addParameter(int.class, "offset")
                    .addParameter(int.class, "maxLimit")
                    .addStatement("super.wrap(buffer, offset, maxLimit)")
                    .addStatement("return this")
                    .build();
        }

        private MethodSpec setMethod()
        {
            return methodBuilder("set")
                    .addModifiers(PUBLIC)
                    .returns(octetsType.nestedClass("Builder"))
                    .addParameter(octetsType, "value")
                    .addStatement("buffer().putBytes(offset(), value.buffer(), value.offset(), value.length())")
                    .addStatement("return this")
                    .build();
        }

        private MethodSpec setMethodViaBuffer()
        {
            return methodBuilder("set")
                    .addModifiers(PUBLIC)
                    .returns(octetsType.nestedClass("Builder"))
                    .addParameter(DIRECT_BUFFER_TYPE, "value")
                    .addParameter(int.class, "offset")
                    .addParameter(int.class, "length")
                    .addStatement("buffer().putByte(offset(), (byte) length)")
                    .addStatement("buffer().putBytes(offset() + FIELD_SIZE_LENGTH, value, offset, length)")
                    .addStatement("return this")
                    .build();
        }

        private MethodSpec setMethodViaByteArray()
        {
            return methodBuilder("set")
                    .addModifiers(PUBLIC)
                    .returns(octetsType.nestedClass("Builder"))
                    .addParameter(byte[].class, "value")
                    .addStatement("buffer().putByte(offset(), (byte) value.length)")
                    .addStatement("buffer().putBytes(offset() + FIELD_SIZE_LENGTH, value)")
                    .addStatement("return this")
                    .build();
        }

        private MethodSpec setMethodViaMutator()
        {
            return methodBuilder("set")
                    .addModifiers(PUBLIC)
                    .returns(octetsType.nestedClass("Builder"))
                    .addParameter(visitorType, "visitor")
                    .addStatement("int length = buffer().getByte(offset() + FIELD_OFFSET_LENGTH) & 0xFF")
                    .addStatement("visitor.visit(buffer(), offset() + FIELD_SIZE_LENGTH, length)")
                    .addStatement("return this")
                    .build();
        }
    }
}
