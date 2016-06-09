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
package org.kaazing.nuklei.ws.internal;

import java.io.Closeable;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.ws.internal.conductor.Conductor;
import org.kaazing.nuklei.ws.internal.router.Router;
import org.kaazing.nuklei.ws.internal.watcher.Watcher;

public final class WsNukleus extends Nukleus.Composite
{
    static final String NAME = "ws";

    private final Closeable cleanup;

    WsNukleus(
        Conductor conductor,
        Watcher watcher,
        Router router,
        Closeable cleanup)
    {
        super(conductor, watcher, router);
        this.cleanup = cleanup;
    }

    @Override
    public String name()
    {
        return WsNukleus.NAME;
    }

    @Override
    public void close() throws Exception
    {
        super.close();
        cleanup.close();
    }
}
