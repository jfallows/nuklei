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

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.NukleusFactorySpi;
import org.kaazing.nuklei.ws.internal.conductor.Conductor;
import org.kaazing.nuklei.ws.internal.router.Router;
import org.kaazing.nuklei.ws.internal.watcher.Watcher;

public final class WsNukleusFactorySpi implements NukleusFactorySpi
{
    @Override
    public String name()
    {
        return WsNukleus.NAME;
    }

    @Override
    public WsNukleus create(Configuration config)
    {
        Context context = new Context();
        context.conclude(config);

        Conductor conductor = new Conductor(context);
        Watcher watcher = new Watcher(context);
        Router router = new Router(context);

        conductor.setRouter(router);
        watcher.setRouter(router);
        router.setConductor(conductor);

        return new WsNukleus(conductor, watcher, router, context);
    }
}
