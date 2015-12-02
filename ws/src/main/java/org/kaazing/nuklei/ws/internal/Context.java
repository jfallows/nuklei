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
package org.kaazing.nuklei.ws.internal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.kaazing.nuklei.Configuration;

public final class Context implements Closeable
{
    private boolean readonly;

    public Context readonly(boolean readonly)
    {
        this.readonly = readonly;
        return this;
    }

    public boolean readonly()
    {
        return readonly;
    }

    public Context controlFile(File controlFile)
    {
        // TODO
        return this;
    }

    public Context conclude(Configuration config)
    {
        // TODO
        return this;
    }

    @Override
    public void close() throws IOException
    {
        // TODO
    }
}
