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
package org.kaazing.nuklei.maven.plugin.internal.ast;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class AstType
{
    public static final AstType INT8 = new AstType("int8");
    public static final AstType INT16 = new AstType("int16");
    public static final AstType INT32 = new AstType("int32");
    public static final AstType INT64 = new AstType("int64");

    public static final AstType UINT8 = new AstType("uint8");
    public static final AstType UINT16 = new AstType("uint16");
    public static final AstType UINT32 = new AstType("uint32");
    public static final AstType UINT64 = new AstType("uint64");

    public static final AstType OCTETS = new AstType("octets");
    public static final AstType STRING = new AstType("string");

    public static final AstType LIST = new AstType("list");
    public static final AstType STRUCT = new AstType("struct");

    private final String name;

    private AstType(
        String name)
    {
        this.name = requireNonNull(name);
    }

    public String name()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(
        Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof AstType))
        {
            return false;
        }

        AstType that = (AstType)obj;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public String toString()
    {
        return name;
    }

    public static AstType dynamicType(
        String scopedName)
    {
        return new AstType(scopedName);
    }
}
