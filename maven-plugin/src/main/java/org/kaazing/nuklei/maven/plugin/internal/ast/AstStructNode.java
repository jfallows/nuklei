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

public final class AstStructNode extends AstNode
{
    private final String name;
    private final int typeId;
    private final String supertype;
    private final List<AstMemberNode> members;

    @Override
    public <R> R accept(
        Visitor<R> visitor)
    {
        return visitor.visitStruct(this);
    }

    public String name()
    {
        return name;
    }

    public int typeId()
    {
        return typeId;
    }

    public String supertype()
    {
        return supertype;
    }

    public List<AstMemberNode> members()
    {
        return members;
    }

    @Override
    public int hashCode()
    {
        return (name.hashCode() << 11) ^ supertype.hashCode() << 7 ^ members.hashCode() << 3 ^ typeId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof AstStructNode))
        {
            return false;
        }

        AstStructNode that = (AstStructNode)o;
        return Objects.equals(this.name, that.name) &&
                this.typeId == that.typeId &&
                Objects.equals(this.supertype, that.supertype) &&
                Objects.equals(this.members, that.members);
    }

    private AstStructNode(
        String name,
        int typeId,
        String supertype,
        List<AstMemberNode> members)
    {
        this.name = requireNonNull(name);
        this.typeId = typeId;
        this.supertype = supertype;
        this.members = unmodifiableList(members);
    }

    public static final class Builder extends AstNode.Builder<AstStructNode>
    {
        private String name;
        private String supertype;
        private int typeId;
        private List<AstMemberNode> members;

        public Builder()
        {
            this.members = new LinkedList<>();
        }

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public Builder typeId(int typeId)
        {
            this.typeId = typeId;
            return this;
        }

        public Builder supertype(String supertype)
        {
            this.supertype = supertype;
            return this;
        }

        public Builder member(AstMemberNode member)
        {
            this.members.add(member);
            return this;
        }

        @Override
        public AstStructNode build()
        {
            return new AstStructNode(name, typeId, supertype, members);
        }
    }
}
