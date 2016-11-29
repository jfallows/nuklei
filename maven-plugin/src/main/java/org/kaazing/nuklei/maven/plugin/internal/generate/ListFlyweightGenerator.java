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
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.DIRECT_BUFFER_TYPE;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.MUTABLE_DIRECT_BUFFER_TYPE;

import java.util.Objects;
import java.util.function.Consumer;

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
public final class ListFlyweightGenerator extends ParameterizedTypeSpecGenerator
{
    private final TypeSpec.Builder classBuilder;
    private final BuilderClassBuilder builderClassBuilder;

    public ListFlyweightGenerator(
        ClassName flyweightType)
    {
        super(ParameterizedTypeName.get(flyweightType.peerClass("ListFW"), TypeVariableName.get("T")));

        TypeVariableName typeVarT = TypeVariableName.get("T");
        TypeVariableName itemType = typeVarT.withBounds(flyweightType);

        this.classBuilder = classBuilder(thisRawName).superclass(flyweightType)
                .addTypeVariable(itemType).addModifiers(PUBLIC, FINAL)
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "nuklei").build());
        this.builderClassBuilder = new BuilderClassBuilder(thisName, flyweightType.nestedClass("Builder"));
    }

    @Override
    public TypeSpec generate()
    {
        return classBuilder
                .addField(itemROField())
                .addMethod(constructor())
                .addMethod(limitMethod())
                .addMethod(wrapMethod())
                .addMethod(forEachMethod())
                .addMethod(toStringMethod())
                .addType(builderClassBuilder.build())
                .build();
    }

    private FieldSpec itemROField()
    {
        TypeName itemType = thisName.typeArguments.get(0);
        return FieldSpec.builder(itemType, "itemRO", PRIVATE, FINAL)
                .build();
    }

    private MethodSpec constructor()
    {
        TypeName itemType = thisName.typeArguments.get(0);
        return constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(itemType, "itemRO")
                .addStatement("this.itemRO = $T.requireNonNull(itemRO)", Objects.class)
                .build();
    }

    private MethodSpec limitMethod()
    {
        return methodBuilder("limit")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(int.class)
                .addStatement("return (maxLimit() == offset()) ? maxLimit() : itemRO.limit()")
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
                .addStatement("int currentOffset = offset")
                .beginControlFlow("while (currentOffset < maxLimit)")
                .addStatement("itemRO.wrap(buffer, currentOffset, maxLimit)")
                .addStatement("currentOffset = itemRO.limit()")
                .endControlFlow()
                .addStatement("checkLimit(limit(), maxLimit)")
                .addStatement("return this")
                .build();
    }

    private MethodSpec forEachMethod()
    {
        ClassName consumerRawType = ClassName.get(Consumer.class);
        TypeName itemType = thisName.typeArguments.get(0);
        TypeName consumerType = ParameterizedTypeName.get(consumerRawType, itemType);
        AnnotationSpec suppressWarningsUnchecked =
                AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build();

        return methodBuilder("forEach")
                .addAnnotation(suppressWarningsUnchecked)
                .addModifiers(PUBLIC)
                .addParameter(consumerType, "consumer")
                .returns(thisName)
                .addStatement("int offset = offset()")
                .beginControlFlow("while (offset < maxLimit())")
                .addStatement("consumer.accept((T) itemRO.wrap(buffer(), offset, maxLimit()))")
                .addStatement("offset = itemRO.limit()")
                .endControlFlow()
                .addStatement("return this")
                .build();
    }

    private MethodSpec toStringMethod()
    {
        return methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(String.class)
                .addStatement("return $S", "LIST")
                .build();
    }

    private static final class BuilderClassBuilder
    {
        private final TypeSpec.Builder classBuilder;
        private final ParameterizedTypeName classType;
        private final ParameterizedTypeName enclosingType;
        private final TypeVariableName typeVarB;
        private TypeVariableName typeVarT;

        private BuilderClassBuilder(
            ParameterizedTypeName enclosingType,
            ClassName builderRawType)
        {
            TypeName builderType = ParameterizedTypeName.get(builderRawType, enclosingType);
            ClassName flyweightType = builderRawType.enclosingClassName();
            ClassName classRawType = enclosingType.rawType.nestedClass("Builder");

            this.typeVarB = TypeVariableName.get("B");
            this.typeVarT = TypeVariableName.get("T");
            this.enclosingType = enclosingType;
            this.classType = ParameterizedTypeName.get(classRawType, typeVarB, typeVarT);
            this.classBuilder = classBuilder(classType.rawType)
                    .addModifiers(PUBLIC, STATIC, FINAL)
                    .addTypeVariable(typeVarB.withBounds(ParameterizedTypeName.get(builderRawType, typeVarT)))
                    .addTypeVariable(typeVarT.withBounds(flyweightType))
                    .superclass(builderType);
        }

        public TypeSpec build()
        {
            return classBuilder
                    .addField(itemRWField())
                    .addMethod(constructor())
                    .addMethod(wrapMethod())
                    .addMethod(itemMethod())
                    .build();
        }

        private FieldSpec itemRWField()
        {
            return FieldSpec.builder(typeVarB, "itemRW", PRIVATE, FINAL)
                    .build();
        }

        private MethodSpec constructor()
        {
            return constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(typeVarB, "itemRW")
                    .addParameter(typeVarT, "itemRO")
                    .addStatement("super(new $T(itemRO))", enclosingType)
                    .addStatement("this.itemRW = itemRW")
                    .build();
        }

        private MethodSpec wrapMethod()
        {
            return methodBuilder("wrap")
                    .addModifiers(PUBLIC)
                    .returns(classType)
                    .addParameter(MUTABLE_DIRECT_BUFFER_TYPE, "buffer")
                    .addParameter(int.class, "offset")
                    .addParameter(int.class, "maxLimit")
                    .addStatement("super.wrap(buffer, offset, maxLimit)")
                    .addStatement("super.limit(offset)")
                    .addStatement("itemRW.wrap(buffer, offset, maxLimit)")
                    .addStatement("return this")
                    .build();
        }

        private MethodSpec itemMethod()
        {
            ClassName consumerRawType = ClassName.get(Consumer.class);
            TypeName mutatorType = ParameterizedTypeName.get(consumerRawType, typeVarB);

            return methodBuilder("item")
                    .addModifiers(PUBLIC)
                    .returns(classType)
                    .addParameter(mutatorType, "mutator")
                    .addStatement("mutator.accept(itemRW)")
                    .addStatement("limit(itemRW.build().limit())")
                    .addStatement("itemRW.wrap(buffer(), limit(), maxLimit())")
                    .addStatement("return this")
                    .build();
        }
    }
}
