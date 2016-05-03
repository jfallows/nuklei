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

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.kaazing.nuklei.maven.plugin.internal.ast.AstNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstScopeNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstSpecificationNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstStructNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstType;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstUnionNode;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

public final class TypeResolver
{
    private final String packageName;
    private final Map<String, AstStructNode> structsByName;
    private final Map<AstType, TypeName> namesByType;

    public TypeResolver(
        String packageName)
    {
        this.structsByName = new HashMap<>();
        this.namesByType = initNamesByType(packageName);
        this.packageName = packageName;
    }

    public AstStructNode resolve(
        String qualifiedName)
    {
        AstStructNode structNode = structsByName.get(qualifiedName);
        if (structNode == null)
        {
            throw new IllegalArgumentException("Unable to resolve: " + qualifiedName);
        }
        return structNode;
    }

    public ClassName flyweightName()
    {
        return (ClassName) namesByType.get(AstType.STRUCT);
    }

    public TypeName resolveType(
        AstType type)
    {
        return namesByType.get(type);
    }

    public ClassName resolveClass(
        AstType type)
    {
        return (ClassName) namesByType.get(type);
    }

    public void visit(
        AstSpecificationNode specification)
    {
        structsByName.putAll(specification.accept(new QualifiedNameVisitor()));
        namesByType.putAll(specification.accept(new ClassNameVisitor(packageName)));
    }

    private static Map<AstType, TypeName> initNamesByType(
        String packageName)
    {
        Map<AstType, TypeName> namesByType = new HashMap<>();
        namesByType.put(AstType.STRUCT, ClassName.get(packageName, "Flyweight"));
        namesByType.put(AstType.STRING, ClassName.get(packageName, "StringFW"));
        namesByType.put(AstType.LIST, ClassName.get(packageName, "ListFW"));
        namesByType.put(AstType.OCTETS, TypeNames.DIRECT_BUFFER_TYPE);
        namesByType.put(AstType.INT8, TypeName.BYTE);
        namesByType.put(AstType.UINT8, TypeName.BYTE);
        namesByType.put(AstType.INT16, TypeName.SHORT);
        namesByType.put(AstType.UINT16, TypeName.SHORT);
        namesByType.put(AstType.INT32, TypeName.INT);
        namesByType.put(AstType.UINT32, TypeName.INT);
        namesByType.put(AstType.INT64, TypeName.LONG);
        namesByType.put(AstType.UINT64, TypeName.LONG);
        return namesByType;
    }

    private static final class QualifiedNameVisitor extends AstNode.Visitor<Map<String, AstStructNode>>
    {
        private final Map<String, AstStructNode> structsByName;
        private final Deque<String> nestedNames;

        private QualifiedNameVisitor()
        {
            this.structsByName = new HashMap<>();
            this.nestedNames = new LinkedList<>();
        }

        @Override
        public Map<String, AstStructNode> visitScope(
            AstScopeNode scopeNode)
        {
            try
            {
                nestedNames.addLast(scopeNode.name());
                return super.visitScope(scopeNode);
            }
            finally
            {
                nestedNames.removeLast();
            }
        }

        @Override
        public Map<String, AstStructNode> visitStruct(
            AstStructNode structNode)
        {
            try
            {
                nestedNames.addLast(structNode.name());
                String qualifiedName = String.join("::", nestedNames);
                structsByName.put(qualifiedName, structNode);
                return super.visitStruct(structNode);
            }
            finally
            {
                nestedNames.removeLast();
            }
        }

        @Override
        protected Map<String, AstStructNode> defaultResult()
        {
            return structsByName;
        }
    }

    private static final class ClassNameVisitor extends AstNode.Visitor<Map<AstType, TypeName>>
    {
        private final String packageName;
        private final Map<AstType, TypeName> namesByType;
        private final Deque<String> scopedNames;

        private ClassNameVisitor(
            String packageName)
        {
            this.packageName = packageName;
            this.namesByType = new HashMap<>();
            this.scopedNames = new LinkedList<>();
        }

        @Override
        public Map<AstType, TypeName> visitScope(
            AstScopeNode scopeNode)
        {
            try
            {
                scopedNames.addLast(scopeNode.name());
                return super.visitScope(scopeNode);
            }
            finally
            {
                scopedNames.removeLast();
            }
        }

        @Override
        public Map<AstType, TypeName> visitStruct(
            AstStructNode structNode)
        {
            return visitNamedType(structNode, structNode.name(), super::visitStruct);
        }

        @Override
        public Map<AstType, TypeName> visitUnion(
            AstUnionNode unionNode)
        {
            return visitNamedType(unionNode, unionNode.name(), super::visitUnion);
        }

        private <N extends AstNode> Map<AstType, TypeName> visitNamedType(
            N node,
            String nodeName,
            Function<N, Map<AstType, TypeName>> visit)
        {
            List<String> packageParts = new ArrayList<>(scopedNames);
            packageParts.set(0, packageName);
            String classPackage = String.join(".", packageParts);

            try
            {
                scopedNames.addLast(nodeName);

                String scopedName = String.join("::", scopedNames);
                AstType type = AstType.dynamicType(scopedName);

                String simpleName = nodeName + "FW";
                ClassName className = ClassName.get(classPackage, simpleName);
                namesByType.put(type, className);

                return visit.apply(node);
            }
            finally
            {
                scopedNames.removeLast();
            }
        }

        @Override
        protected Map<AstType, TypeName> defaultResult()
        {
            return namesByType;
        }

    }
}
