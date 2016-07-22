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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

public final class TypeNames
{
    private static final String AGRONA_PACKAGE = "org.agrona";
    private static final String AGRONA_CONCURRENT_PACKAGE = AGRONA_PACKAGE + ".concurrent";

    public static final ClassName BIT_UTIL_TYPE = ClassName.get(AGRONA_PACKAGE, "BitUtil");
    public static final ClassName DIRECT_BUFFER_TYPE = ClassName.get(AGRONA_PACKAGE, "DirectBuffer");
    public static final ClassName MUTABLE_DIRECT_BUFFER_TYPE = ClassName.get(AGRONA_PACKAGE, "MutableDirectBuffer");

    public static final ClassName UNSAFE_BUFFER_TYPE = ClassName.get(AGRONA_CONCURRENT_PACKAGE, "UnsafeBuffer");

    public static final TypeName BYTE_ARRAY = ArrayTypeName.of(byte.class);

    private TypeNames()
    {
        // no instances
    }
}
