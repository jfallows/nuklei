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
package org.kaazing.nuklei.maven.plugin.internal.ast.parse;

import static org.junit.Assert.assertEquals;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Test;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstMemberNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstScopeNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstStructNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstType;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiLexer;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.MemberContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.ScopeContext;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.Struct_typeContext;

public class AstParserTest
{
    @Test
    public void shouldParseScope()
    {
        NukleiParser parser = newParser("scope common { }");
        ScopeContext ctx = parser.scope();
        AstScopeNode actual = new AstParser().visitScope(ctx);

        AstScopeNode expected = new AstScopeNode.Builder()
                .name("common")
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseNestedScopes()
    {
        NukleiParser parser = newParser("scope common { scope control { } scope stream { } }");
        ScopeContext ctx = parser.scope();
        AstScopeNode actual = new AstParser().visitScope(ctx);

        AstScopeNode expected = new AstScopeNode.Builder()
                .name("common")
                .scope(new AstScopeNode.Builder().depth(1).name("control").build())
                .scope(new AstScopeNode.Builder().depth(1).name("stream").build())
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseStructWithoutMembers()
    {
        NukleiParser parser = newParser("struct Person { }");
        Struct_typeContext ctx = parser.struct_type();
        AstStructNode actual = new AstParser().visitStruct_type(ctx);

        AstStructNode expected = new AstStructNode.Builder()
                .name("Person")
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseStructWithMembers()
    {
        NukleiParser parser = newParser("struct Person { string firstName; string lastName; }");
        Struct_typeContext ctx = parser.struct_type();
        AstStructNode actual = new AstParser().visitStruct_type(ctx);

        AstStructNode expected = new AstStructNode.Builder()
                .name("Person")
                .member(new AstMemberNode.Builder().type(AstType.STRING).name("firstName").build())
                .member(new AstMemberNode.Builder().type(AstType.STRING).name("lastName").build())
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseStructWithExtends()
    {
        NukleiParser parser = newParser("struct Employee extends common::Person { }");
        Struct_typeContext ctx = parser.struct_type();
        AstStructNode actual = new AstParser().visitStruct_type(ctx);

        AstStructNode expected = new AstStructNode.Builder()
                .name("Employee")
                .supertype("common::Person")
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseInt64Member()
    {
        NukleiParser parser = newParser("int64 field;");
        MemberContext ctx = parser.member();
        AstMemberNode actual = new AstParser().visitMember(ctx);

        AstMemberNode expected = new AstMemberNode.Builder()
                .type(AstType.INT64)
                .name("field")
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseUint8Member()
    {
        NukleiParser parser = newParser("uint8 field;");
        MemberContext ctx = parser.member();
        AstMemberNode actual = new AstParser().visitMember(ctx);

        AstMemberNode expected = new AstMemberNode.Builder()
                .type(AstType.UINT8)
                .unsignedType(AstType.INT32)
                .name("field")
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseUint16Member()
    {
        NukleiParser parser = newParser("uint16 field;");
        MemberContext ctx = parser.member();
        AstMemberNode actual = new AstParser().visitMember(ctx);

        AstMemberNode expected = new AstMemberNode.Builder()
                .type(AstType.UINT16)
                .unsignedType(AstType.INT32)
                .name("field")
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseStringMember()
    {
        NukleiParser parser = newParser("string field;");
        MemberContext ctx = parser.member();
        AstMemberNode actual = new AstParser().visitMember(ctx);

        AstMemberNode expected = new AstMemberNode.Builder()
                .type(AstType.STRING)
                .name("field")
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldParseListMember()
    {
        NukleiParser parser = newParser("list<string> field;");
        MemberContext ctx = parser.member();
        AstMemberNode actual = new AstParser().visitMember(ctx);

        AstMemberNode expected = new AstMemberNode.Builder()
                .type(AstType.LIST)
                .type(AstType.STRING)
                .name("field")
                .build();

        assertEquals(expected, actual);
    }

    private static NukleiParser newParser(
        String input)
    {
        ANTLRInputStream ais = new ANTLRInputStream(input);
        NukleiLexer lexer = new NukleiLexer(ais);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        NukleiParser parser = new NukleiParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        parser.removeErrorListeners();
        return parser;
    }
}
