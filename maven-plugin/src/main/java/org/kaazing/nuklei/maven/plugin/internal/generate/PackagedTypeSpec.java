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
package org.kaazing.nuklei.maven.plugin.internal.generate;

import com.squareup.javapoet.TypeSpec;

public final class PackagedTypeSpec
{
    private final String packageName;
    private final TypeSpec typeSpec;

    private PackagedTypeSpec(
        String packageName,
        TypeSpec typeSpec)
    {
        this.packageName = packageName;
        this.typeSpec = typeSpec;
    }

    public static PackagedTypeSpec of(
        String packageName,
        TypeSpec typeSpec)
    {
        return new PackagedTypeSpec(packageName, typeSpec);
    }

    public String packageName()
    {
        return packageName;
    }

    public TypeSpec typeSpec()
    {
        return typeSpec;
    }
}
