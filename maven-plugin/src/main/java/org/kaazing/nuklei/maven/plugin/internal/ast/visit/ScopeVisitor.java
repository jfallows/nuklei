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
package org.kaazing.nuklei.maven.plugin.internal.ast.visit;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.kaazing.nuklei.maven.plugin.internal.ast.AstNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstScopeNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstStructNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstType;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstUnionNode;
import org.kaazing.nuklei.maven.plugin.internal.generate.StructFlyweightGenerator;
import org.kaazing.nuklei.maven.plugin.internal.generate.TypeResolver;
import org.kaazing.nuklei.maven.plugin.internal.generate.TypeSpecGenerator;
import org.kaazing.nuklei.maven.plugin.internal.generate.UnionFlyweightGenerator;

import com.squareup.javapoet.ClassName;

public final class ScopeVisitor extends AstNode.Visitor<Collection<TypeSpecGenerator<?>>>
{
    private final String scopeName;
    private final String packageName;
    private final TypeResolver resolver;
    private final List<String> targetScopes;
    private final Collection<TypeSpecGenerator<?>> defaultResult;

    public ScopeVisitor(
        String scopeName,
        String packageName,
        TypeResolver resolver,
        List<String> targetScopes)
    {
        this.scopeName = requireNonNull(scopeName);
        this.packageName = requireNonNull(packageName);
        this.resolver = requireNonNull(resolver);
        this.targetScopes = requireNonNull(targetScopes);
        this.defaultResult = new LinkedList<>();
    }

    @Override
    public Collection<TypeSpecGenerator<?>> visitScope(
        AstScopeNode scopeNode)
    {
        if (!targetScopes.stream().anyMatch(this::shouldVisit))
        {
            return defaultResult();
        }

        return super.visitScope(scopeNode);
    }

    @Override
    public Collection<TypeSpecGenerator<?>> visitNestedScope(
        AstScopeNode scopeNode)
    {
        String nestedName = scopeNode.name();
        String subscopeName = String.format("%s::%s", scopeName, nestedName);
        String subpackageName = String.format("%s.%s", packageName, nestedName);
        return new ScopeVisitor(subscopeName, subpackageName, resolver, targetScopes).visitScope(scopeNode);
    }

    @Override
    public Collection<TypeSpecGenerator<?>> visitStruct(
        AstStructNode structNode)
    {
        if (!targetScopes.stream().anyMatch(this::shouldVisit))
        {
            return defaultResult();
        }

        String baseName = structNode.name();
        AstType structType = AstType.dynamicType(String.format("%s::%s", scopeName, baseName));
        ClassName structName = resolver.resolveClass(structType);
        StructFlyweightGenerator generator = new StructFlyweightGenerator(structName, resolver.flyweightName(), baseName);
        generator.typeId(findTypeId(structNode));

        return new StructVisitor(generator, resolver).visitStruct(structNode);
    }

    @Override
    public Collection<TypeSpecGenerator<?>> visitUnion(
        AstUnionNode unionNode)
    {
        if (!targetScopes.stream().anyMatch(this::shouldVisit))
        {
            return defaultResult();
        }

        String baseName = unionNode.name();
        AstType unionType = AstType.dynamicType(String.format("%s::%s", scopeName, baseName));
        ClassName unionName = resolver.resolveClass(unionType);
        UnionFlyweightGenerator generator = new UnionFlyweightGenerator(unionName, resolver.flyweightName(), baseName);

        return new UnionVisitor(generator, resolver).visitUnion(unionNode);
    }

    @Override
    protected Collection<TypeSpecGenerator<?>> defaultResult()
    {
        return defaultResult;
    }

    @Override
    protected Collection<TypeSpecGenerator<?>> aggregateResult(
        Collection<TypeSpecGenerator<?>> aggregate,
        Collection<TypeSpecGenerator<?>> nextResult)
    {
        if (nextResult != aggregate)
        {
            aggregate.addAll(nextResult);
        }
        return aggregate;
    }

    private boolean shouldVisit(
        String target)
    {
        return target.equals(scopeName) || scopeName.startsWith(target + "::") || target.startsWith(scopeName + "::");
    }

    private int findTypeId(
        AstStructNode structNode)
    {
        AstStructNode currentNode = structNode;
        while (currentNode != null && currentNode.typeId() == 0 && currentNode.supertype() != null)
        {
           currentNode = resolver.resolve(currentNode.supertype());
        }

        return (currentNode != null) ? currentNode.typeId() : 0;
    }
}
