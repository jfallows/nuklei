/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.DIRECT_BUFFER_TYPE;
import static org.kaazing.nuklei.maven.plugin.internal.generate.TypeNames.MUTABLE_DIRECT_BUFFER_TYPE;

import java.util.function.Consumer;

import javax.annotation.Generated;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

@SuppressWarnings("squid:S1192")
public final class FlyweightGenerator extends ClassSpecGenerator
{
    private final TypeSpec.Builder classBuilder;
    private final BuilderClassBuilder builderClassBuilder;

    public FlyweightGenerator(
        ClassName flyweightType)
    {
        super(flyweightType);

        this.classBuilder = classBuilder(thisName).addModifiers(PUBLIC, ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "nuklei").build());
        this.builderClassBuilder = new BuilderClassBuilder(thisName);
    }

    @Override
    public TypeSpec generate()
    {
        return classBuilder.addField(bufferField())
                    .addField(offsetField())
                    .addField(maxLimitField())
                    .addMethod(offsetMethod())
                    .addMethod(bufferMethod())
                    .addMethod(limitMethod())
                    .addMethod(lengthMethod())
                    .addMethod(maxLimitMethod())
                    .addMethod(wrapMethod())
                    .addMethod(checkLimitMethod())
                    .addType(visitorInterface())
                    .addType(builderClassBuilder.build())
                    .build();
    }

    private TypeSpec visitorInterface()
    {
        TypeVariableName typeVarT = TypeVariableName.get("T");
        return TypeSpec.interfaceBuilder(thisName.nestedClass("Visitor"))
                .addModifiers(PUBLIC)
                .addTypeVariable(typeVarT)
                .addAnnotation(FunctionalInterface.class)
                .addMethod(MethodSpec.methodBuilder("visit")
                        .addModifiers(PUBLIC, ABSTRACT)
                        .returns(typeVarT)
                        .addParameter(DIRECT_BUFFER_TYPE, "buffer")
                        .addParameter(int.class, "offset")
                        .addParameter(int.class, "length")
                        .build())
                .build();
    }

    private FieldSpec bufferField()
    {
        return FieldSpec.builder(DIRECT_BUFFER_TYPE, "buffer", PRIVATE).build();
    }

    private FieldSpec offsetField()
    {
        return FieldSpec.builder(int.class, "offset", PRIVATE).build();
    }

    private FieldSpec maxLimitField()
    {
        return FieldSpec.builder(int.class, "maxLimit", PRIVATE).build();
    }

    private MethodSpec maxLimitMethod()
    {
        return methodBuilder("maxLimit")
                  .addModifiers(PROTECTED, FINAL)
                  .returns(int.class)
                  .addStatement("return maxLimit")
                  .build();
    }

    private MethodSpec offsetMethod()
    {
        return methodBuilder("offset")
                  .addModifiers(PUBLIC, FINAL)
                  .returns(int.class)
                  .addStatement("return offset")
                  .build();
    }

    private MethodSpec bufferMethod()
    {
        return methodBuilder("buffer")
                  .addModifiers(PUBLIC, FINAL)
                  .returns(DIRECT_BUFFER_TYPE)
                  .addStatement("return buffer")
                  .build();
    }

    private MethodSpec limitMethod()
    {
        return methodBuilder("limit")
                  .addModifiers(PUBLIC, ABSTRACT)
                  .returns(int.class)
                  .build();
    }

    private MethodSpec lengthMethod()
    {
        return methodBuilder("length")
                  .addModifiers(PUBLIC, FINAL)
                  .returns(int.class)
                  .addStatement("return limit() - offset()")
                  .build();
    }

    private MethodSpec wrapMethod()
    {
        return methodBuilder("wrap")
                  .addModifiers(PROTECTED)
                  .addParameter(DIRECT_BUFFER_TYPE, "buffer")
                  .addParameter(int.class, "offset")
                  .addParameter(int.class, "maxLimit")
                  .returns(thisName)
                  .addStatement("this.buffer = buffer")
                  .addStatement("this.offset = offset")
                  .addStatement("this.maxLimit = maxLimit")
                  .addStatement("return this")
                  .build();
    }

    private MethodSpec checkLimitMethod()
    {
        return methodBuilder("checkLimit")
                  .addModifiers(PROTECTED, STATIC, FINAL)
                  .addParameter(int.class, "maxLimit")
                  .addParameter(int.class, "actingLimit")
                  .beginControlFlow("if (maxLimit > actingLimit)")
                  .addStatement("final String msg = String.format(\"maxLimit=%d is beyond actingLimit=%d\", " +
                          "maxLimit, actingLimit)")
                  .addStatement("throw new IndexOutOfBoundsException(msg)")
                  .endControlFlow()
                  .build();
    }

    private static final class BuilderClassBuilder
    {
        private final TypeSpec.Builder classBuilder;
        private final ParameterizedTypeName classType;
        private final TypeVariableName typeVarT;

        private BuilderClassBuilder(
            ClassName flyweightType)
        {
            ClassName classRawType = flyweightType.nestedClass("Builder");

            this.typeVarT = TypeVariableName.get("T");
            this.classType = ParameterizedTypeName.get(classRawType, typeVarT);

            this.classBuilder = classBuilder(classRawType.simpleName())
                    .addTypeVariable(typeVarT.withBounds(flyweightType))
                    .addModifiers(PUBLIC, ABSTRACT, STATIC);
        }

        public TypeSpec build()
        {
            return classBuilder.addField(flyweightField())
                    .addField(bufferField())
                    .addField(offsetField())
                    .addField(limitField())
                    .addField(maxLimitField())
                    .addMethod(limitAccessor())
                    .addMethod(maxLimitAccessor())
                    .addMethod(buildMethod())
                    .addMethod(constructor())
                    .addMethod(flyweightAccessor())
                    .addMethod(bufferAccessor())
                    .addMethod(offsetAccessor())
                    .addMethod(limitMutator())
                    .addMethod(wrapMethod())
                    .addMethod(iterateMethod())
                    .build();
        }

        private FieldSpec flyweightField()
        {
            return FieldSpec.builder(typeVarT, "flyweight", PRIVATE, FINAL).build();
        }

        private FieldSpec bufferField()
        {
            return FieldSpec.builder(MUTABLE_DIRECT_BUFFER_TYPE, "buffer", PRIVATE).build();
        }

        private FieldSpec offsetField()
        {
            return FieldSpec.builder(int.class, "offset", PRIVATE).build();
        }

        private FieldSpec limitField()
        {
            return FieldSpec.builder(int.class, "limit", PRIVATE).build();
        }

        private FieldSpec maxLimitField()
        {
            return FieldSpec.builder(int.class, "maxLimit", PRIVATE).build();
        }

        private MethodSpec limitAccessor()
        {
            return methodBuilder("limit")
                      .addModifiers(PUBLIC, FINAL)
                      .returns(int.class)
                      .addStatement("return limit")
                      .build();
        }

        private MethodSpec maxLimitAccessor()
        {
            return methodBuilder("maxLimit")
                      .addModifiers(PUBLIC, FINAL)
                      .returns(int.class)
                      .addStatement("return maxLimit")
                      .build();
        }

        private MethodSpec buildMethod()
        {
            return methodBuilder("build")
                      .addModifiers(PUBLIC, FINAL)
                      .returns(typeVarT)
                      .addStatement("flyweight.wrap(buffer, offset, limit)")
                      .addStatement("return flyweight")
                      .build();
        }

        private MethodSpec constructor()
        {
            return constructorBuilder()
                      .addModifiers(PROTECTED)
                      .addParameter(typeVarT, "flyweight")
                      .addStatement("this.flyweight = flyweight")
                      .build();
        }

        private MethodSpec flyweightAccessor()
        {
            return methodBuilder("flyweight")
                      .addModifiers(PROTECTED, FINAL)
                      .returns(typeVarT)
                      .addStatement("return flyweight")
                      .build();
        }

        private MethodSpec bufferAccessor()
        {
            return methodBuilder("buffer")
                      .addModifiers(PROTECTED, FINAL)
                      .returns(MUTABLE_DIRECT_BUFFER_TYPE)
                      .addStatement("return buffer")
                      .build();
        }

        private MethodSpec offsetAccessor()
        {
            return methodBuilder("offset")
                      .addModifiers(PROTECTED, FINAL)
                      .returns(int.class)
                      .addStatement("return offset")
                      .build();
        }

        private MethodSpec limitMutator()
        {
            return methodBuilder("limit")
                      .addModifiers(PROTECTED, FINAL)
                      .addParameter(int.class, "limit")
                      .addStatement("this.limit = limit")
                      .build();
        }

        private MethodSpec wrapMethod()
        {
            return methodBuilder("wrap")
                      .addModifiers(PROTECTED)
                      .returns(classType)
                      .addParameter(MUTABLE_DIRECT_BUFFER_TYPE, "buffer")
                      .addParameter(int.class, "offset")
                      .addParameter(int.class, "maxLimit")
                      .addStatement("this.buffer = buffer")
                      .addStatement("this.offset = offset")
                      .addStatement("this.limit = maxLimit")
                      .addStatement("this.maxLimit = maxLimit")
                      .addStatement("return this")
                      .build();
        }

        private MethodSpec iterateMethod()
        {
            TypeVariableName typeVarE = TypeVariableName.get("E");
            ClassName consumerType = ClassName.get(Consumer.class);
            ClassName iterableType = ClassName.get(Iterable.class);

            return methodBuilder("iterate")
                    .addModifiers(PUBLIC)
                    .addTypeVariable(typeVarE)
                    .returns(classType)
                    .addParameter(ParameterizedTypeName.get(iterableType, typeVarE), "iterable")
                    .addParameter(ParameterizedTypeName.get(consumerType, typeVarE), "action")
                    .addStatement("iterable.forEach(action)")
                    .addStatement("return this")
                    .build();
        }
    }
}
