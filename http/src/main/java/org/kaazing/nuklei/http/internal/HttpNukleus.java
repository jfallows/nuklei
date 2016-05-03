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
package org.kaazing.nuklei.http.internal;

import java.io.Closeable;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.http.internal.conductor.Conductor;
import org.kaazing.nuklei.http.internal.reader.Reader;

public final class HttpNukleus extends Nukleus.Composite
{
    static final String NAME = "http";

    private final Closeable cleanup;

    HttpNukleus(
        Conductor conductor,
        Reader reader,
        Closeable cleanup)
    {
        super(conductor, reader);
        this.cleanup = cleanup;
    }

    @Override
    public String name()
    {
        return HttpNukleus.NAME;
    }

    @Override
    public void close() throws Exception
    {
        super.close();
        cleanup.close();
    }
}
