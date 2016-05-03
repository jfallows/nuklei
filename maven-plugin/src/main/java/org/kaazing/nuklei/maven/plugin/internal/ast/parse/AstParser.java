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
package org.kaazing.nuklei.maven.plugin.internal.ast.parse;

import java.util.Deque;
import java.util.LinkedList;

import org.kaazing.nuklei.maven.plugin.internal.ast.AstCaseNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstCaseNode.Builder;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstMemberNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstScopeNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstSpecificationNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstStructNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstType;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstUnionNode;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiBaseVisitor;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Case_memberContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.DeclaratorContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Int16_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Int32_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Int64_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Int8_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Int_literalContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.List_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.MemberContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Octets_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.ScopeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Scoped_nameContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.SpecificationContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.String_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Struct_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Uint16_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Uint32_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Uint64_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Uint8_typeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Union_typeContext;

public final class AstParser extends NukleiBaseVisitor<AstNode>
{
    private final Deque<AstScopeNode.Builder> scopeBuilders;

    private AstSpecificationNode.Builder specificationBuilder;
    private AstStructNode.Builder structBuilder;
    private AstMemberNode.Builder memberBuilder;
    private AstUnionNode.Builder unionBuilder;

    private Builder caseBuilder;

    public AstParser()
    {
        this.scopeBuilders = new LinkedList<>();
    }

    @Override
    public AstSpecificationNode visitSpecification(
        SpecificationContext ctx)
    {
        specificationBuilder = new AstSpecificationNode.Builder();

        super.visitSpecification(ctx);

        return specificationBuilder.build();
    }

    @Override
    public AstScopeNode visitScope(
        ScopeContext ctx)
    {
        String name = ctx.ID().getText();

        AstScopeNode.Builder scopeBuilder = new AstScopeNode.Builder();
        scopeBuilder.depth(scopeBuilders.size());
        scopeBuilder.name(name);

        scopeBuilders.offer(scopeBuilder);
        super.visitScope(ctx);
        scopeBuilders.pollLast();

        AstScopeNode.Builder parent = scopeBuilders.peekLast();
        if (parent != null)
        {
            AstScopeNode scopeNode = scopeBuilder.build();
            parent.scope(scopeNode);
            return scopeNode;
        }
        else if (specificationBuilder != null)
        {
            AstScopeNode scopeNode = scopeBuilder.build();
            specificationBuilder.scope(scopeNode);
            return scopeNode;
        }
        else
        {
            return scopeBuilder.build();
        }
    }

    @Override
    public AstStructNode visitStruct_type(
        Struct_typeContext ctx)
    {
        structBuilder = new AstStructNode.Builder();
        structBuilder.name(ctx.ID().getText());
        structBuilder.typeId(parseInt(ctx.int_literal()));

        Scoped_nameContext scopedName = ctx.scoped_name();
        if (scopedName != null)
        {
            structBuilder.supertype(scopedName.getText());
        }

        super.visitStruct_type(ctx);

        AstScopeNode.Builder scopeBuilder = scopeBuilders.peekLast();
        if (scopeBuilder != null)
        {
            AstStructNode struct = structBuilder.build();
            scopeBuilder.struct(struct);
            return struct;
        }
        else
        {
            return structBuilder.build();
        }
    }

    @Override
    public AstMemberNode visitMember(
        MemberContext ctx)
    {
        memberBuilder = new AstMemberNode.Builder();

        super.visitMember(ctx);

        AstMemberNode member = memberBuilder.build();
        memberBuilder = null;

        if (caseBuilder != null)
        {
            caseBuilder.member(member);
        }
        else if (structBuilder != null)
        {
            structBuilder.member(member);
        }

        return member;
    }

    @Override
    public AstUnionNode visitUnion_type(
        Union_typeContext ctx)
    {
        unionBuilder = new AstUnionNode.Builder();
        unionBuilder.name(ctx.ID().getText());

        super.visitUnion_type(ctx);

        AstUnionNode union = unionBuilder.build();
        unionBuilder = null;

        AstScopeNode.Builder scopeBuilder = scopeBuilders.peekLast();
        if (scopeBuilder != null)
        {
            scopeBuilder.union(union);
        }

        return union;
    }

    @Override
    public AstCaseNode visitCase_member(
        Case_memberContext ctx)
    {
        caseBuilder = new AstCaseNode.Builder()
                .value(Integer.parseInt(ctx.int_literal().getText()));

        super.visitCase_member(ctx);

        AstCaseNode caseN = caseBuilder.build();
        caseBuilder = null;

        if (unionBuilder != null)
        {
            unionBuilder.caseN(caseN);
        }

        return caseN;
    }

    @Override
    public AstNode visitDeclarator(
        DeclaratorContext ctx)
    {
        memberBuilder.name(ctx.ID().toString());
        return super.visitDeclarator(ctx);
    }

    @Override
    public AstNode visitInt64_type(
        Int64_typeContext ctx)
    {
        memberBuilder.type(AstType.INT64);
        return super.visitInt64_type(ctx);
    }

    @Override
    public AstNode visitInt32_type(
        Int32_typeContext ctx)
    {
        memberBuilder.type(AstType.INT32);
        return super.visitInt32_type(ctx);
    }

    @Override
    public AstNode visitInt16_type(
        Int16_typeContext ctx)
    {
        memberBuilder.type(AstType.INT16);
        return super.visitInt16_type(ctx);
    }

    @Override
    public AstNode visitInt8_type(
        Int8_typeContext ctx)
    {
        memberBuilder.type(AstType.INT8);
        return super.visitInt8_type(ctx);
    }

    @Override
    public AstNode visitUint64_type(
        Uint64_typeContext ctx)
    {
        memberBuilder.type(AstType.UINT64);
        return super.visitUint64_type(ctx);
    }

    @Override
    public AstNode visitUint32_type(
        Uint32_typeContext ctx)
    {
        memberBuilder.type(AstType.UINT32).unsignedType(AstType.INT64);
        return super.visitUint32_type(ctx);
    }

    @Override
    public AstNode visitUint16_type(
        Uint16_typeContext ctx)
    {
        memberBuilder.type(AstType.UINT16).unsignedType(AstType.INT32);
        return super.visitUint16_type(ctx);
    }

    @Override
    public AstNode visitUint8_type(
        Uint8_typeContext ctx)
    {
        memberBuilder.type(AstType.UINT8).unsignedType(AstType.INT32);
        return super.visitUint8_type(ctx);
    }

    @Override
    public AstNode visitString_type(
        String_typeContext ctx)
    {
        memberBuilder.type(AstType.STRING);
        return super.visitString_type(ctx);
    }

    @Override
    public AstNode visitOctets_type(
        Octets_typeContext ctx)
    {
        memberBuilder.type(AstType.OCTETS);
        if (ctx.positive_int_const() != null)
        {
            memberBuilder.size(Integer.parseInt(ctx.positive_int_const().getText()));
        }
        return super.visitOctets_type(ctx);
    }

    @Override
    public AstNode visitList_type(
        List_typeContext ctx)
    {
        memberBuilder.type(AstType.LIST);
        return super.visitList_type(ctx);
    }

    @Override
    public AstNode visitScoped_name(
        Scoped_nameContext ctx)
    {
        if (memberBuilder != null)
        {
            memberBuilder.type(AstType.dynamicType(ctx.getText()));
        }
        return super.visitScoped_name(ctx);
    }

    private static int parseInt(
        Int_literalContext ctx)
    {
        if (ctx == null)
        {
            return 0;
        }

        String text = ctx.getText();
        if (text.startsWith("0x"))
        {
            return Integer.parseInt(text.substring(2), 16);
        }
        else
        {
            return Integer.parseInt(text);
        }
    }
}
