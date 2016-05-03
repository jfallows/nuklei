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

import java.util.Objects;

public final class AstCaseNode extends AstNode
{
    private final int value;
    private final AstMemberNode member;

    @Override
    public <R> R accept(
        Visitor<R> visitor)
    {
        return visitor.visitCase(this);
    }

    public int value()
    {
        return value;
    }

    public AstMemberNode member()
    {
        return member;
    }

    @Override
    public int hashCode()
    {
        return (member.hashCode() << 11) ^ value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof AstCaseNode))
        {
            return false;
        }

        AstCaseNode that = (AstCaseNode)o;
        return this.value == that.value &&
                Objects.equals(this.member, that.member);
    }

    private AstCaseNode(
        int value,
        AstMemberNode member)
    {
        this.value = value;
        this.member = member;
    }

    @Override
    public String toString()
    {
        return String.format("CASE [value=%d, member=%s]", value, member);
    }

    public static final class Builder extends AstNode.Builder<AstCaseNode>
    {
        private int value;
        private AstMemberNode member;

        public Builder value(int value)
        {
            this.value = value;
            return this;
        }

        public Builder member(AstMemberNode member)
        {
            this.member = member;
            return this;
        }

        @Override
        public AstCaseNode build()
        {
            return new AstCaseNode(value, member);
        }
    }
}
