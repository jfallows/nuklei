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
package org.kaazing.nuklei.maven.plugin.internal.ast;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class AstScopeNode extends AstNode
{
    private final int depth;
    private final String name;
    private final List<AstScopeNode> scopes;
    private final List<AstStructNode> structs;
    private final List<AstUnionNode> unions;

    private AstScopeNode(
        int depth,
        String name,
        List<AstScopeNode> scopes,
        List<AstStructNode> structs,
        List<AstUnionNode> unions)
    {
        this.depth = depth;
        this.name = requireNonNull(name);
        this.scopes = unmodifiableList(scopes);
        this.structs = unmodifiableList(structs);
        this.unions = unmodifiableList(unions);
    }

    @Override
    public <R> R accept(
        Visitor<R> visitor)
    {
        return visitor.visitScope(this);
    }

    public int depth()
    {
        return depth;
    }

    public String name()
    {
        return name;
    }

    public List<AstStructNode> structs()
    {
        return structs;
    }

    public List<AstUnionNode> unions()
    {
        return unions;
    }

    public List<AstScopeNode> scopes()
    {
        return scopes;
    }

    @Override
    public int hashCode()
    {
        return (name.hashCode() << 11) ^ scopes.hashCode() << 7 ^ structs.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof AstScopeNode))
        {
            return false;
        }

        AstScopeNode that = (AstScopeNode)o;
        return this.depth == that.depth &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.structs, that.structs) &&
                Objects.equals(this.scopes, that.scopes);
    }

    public static final class Builder extends AstNode.Builder<AstScopeNode>
    {
        private int depth;
        private String name;
        private List<AstScopeNode> scopes;
        private List<AstStructNode> structs;
        private List<AstUnionNode> unions;

        public Builder()
        {
            this.structs = new LinkedList<>();
            this.scopes = new LinkedList<>();
            this.unions = new LinkedList<>();
        }

        public Builder depth(int depth)
        {
            this.depth = depth;
            return this;
        }

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public Builder struct(AstStructNode struct)
        {
            this.structs.add(struct);
            return this;
        }

        public Builder union(AstUnionNode union)
        {
            this.unions.add(union);
            return this;
        }

        public Builder scope(AstScopeNode scope)
        {
            this.scopes.add(scope);
            return this;
        }

        @Override
        public AstScopeNode build()
        {
            return new AstScopeNode(depth, name, scopes, structs, unions);
        }
    }
}
