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
package org.kaazing.nuklei.echo.internal;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.echo.internal.acceptor.Acceptor;
import org.kaazing.nuklei.echo.internal.conductor.Conductor;
import org.kaazing.nuklei.echo.internal.connector.Connector;
import org.kaazing.nuklei.echo.internal.reflector.Reflector;

public final class EchoNukleus implements Nukleus
{
    private final Conductor conductor;
    private final Acceptor acceptor;
    private final Connector connector;
    private final Reflector reflector;

    public EchoNukleus(Context context)
    {
        this.conductor = new Conductor(context);
        this.acceptor = new Acceptor(context);
        this.connector = new Connector(context);
        this.reflector = new Reflector(context);
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += conductor.process();
        weight += acceptor.process();
        weight += connector.process();
        weight += reflector.process();

        return weight;
    }

    @Override
    public String name()
    {
        return "echo";
    }
}
