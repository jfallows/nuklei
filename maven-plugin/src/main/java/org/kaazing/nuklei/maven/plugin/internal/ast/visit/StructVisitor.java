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
package org.kaazing.nuklei.maven.plugin.internal.ast.visit;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Set;

import org.kaazing.nuklei.maven.plugin.internal.ast.AstMemberNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstStructNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstType;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstUnionNode;
import org.kaazing.nuklei.maven.plugin.internal.generate.StructFlyweightGenerator;
import org.kaazing.nuklei.maven.plugin.internal.generate.TypeResolver;
import org.kaazing.nuklei.maven.plugin.internal.generate.TypeSpecGenerator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

public final class StructVisitor extends AstNode.Visitor<Collection<TypeSpecGenerator<?>>>
{
    private final StructFlyweightGenerator generator;
    private final TypeResolver resolver;
    private final Set<TypeSpecGenerator<?>> defaultResult;

    public StructVisitor(
        StructFlyweightGenerator generator,
        TypeResolver resolver)
    {
        this.generator = generator;
        this.resolver = resolver;
        this.defaultResult = singleton(generator);
    }

    @Override
    public Collection<TypeSpecGenerator<?>> visitStruct(
        AstStructNode structNode)
    {
        String supertype = structNode.supertype();
        if (supertype != null)
        {
            AstStructNode superNode = resolver.resolve(supertype);
            visitStruct(superNode);
        }

        super.visitStruct(structNode);
        return defaultResult();
    }

    @Override
    public Collection<TypeSpecGenerator<?>> visitUnion(
        AstUnionNode unionNode)
    {
        return defaultResult();
    }

    @Override
    public Collection<TypeSpecGenerator<?>> visitMember(
        AstMemberNode memberNode)
    {
        String memberName = memberNode.name();
        AstType memberType = memberNode.type();
        AstType memberUnsignedType = memberNode.unsignedType();

        if (memberType == AstType.LIST)
        {
            ClassName rawType = resolver.resolveClass(memberType);
            TypeName[] typeArguments = memberNode.types()
                    .stream()
                    .skip(1)
                    .map(resolver::resolveType)
                    .collect(toList())
                    .toArray(new TypeName[0]);
            ParameterizedTypeName memberTypeName = ParameterizedTypeName.get(rawType, typeArguments);
            TypeName memberUnsignedTypeName = resolver.resolveType(memberUnsignedType);
            generator.addMember(memberName, memberTypeName, memberUnsignedTypeName);
        }
        else
        {
            TypeName memberTypeName = resolver.resolveType(memberType);
            TypeName memberUnsignedTypeName = resolver.resolveType(memberUnsignedType);
            generator.addMember(memberName, memberTypeName, memberUnsignedTypeName);
        }

        return defaultResult();
    }

    @Override
    protected Collection<TypeSpecGenerator<?>> defaultResult()
    {
        return defaultResult;
    }
}
