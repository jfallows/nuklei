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
package org.kaazing.nuklei.tcp.internal;

import java.util.function.Consumer;

import org.kaazing.nuklei.CompositeNukleus;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.acceptor.Acceptor;
import org.kaazing.nuklei.tcp.internal.conductor.Conductor;
import org.kaazing.nuklei.tcp.internal.connector.Connector;
import org.kaazing.nuklei.tcp.internal.reader.Reader;
import org.kaazing.nuklei.tcp.internal.writer.Writer;

public final class TcpNukleus extends CompositeNukleus
{
    private final Context context;
    private final Conductor conductor;
    private final Acceptor acceptor;
    private final Connector connector;
    private final Writer writer;
    private final Reader reader;

    TcpNukleus(Context context)
    {
        this.context = context;
        this.conductor = new Conductor(context);
        this.acceptor = new Acceptor(context);
        this.connector = new Connector(context);
        this.reader = new Reader(context);
        this.writer = new Writer(context);
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += conductor.process();
        weight += acceptor.process();
        weight += connector.process();
        weight += reader.process();
        weight += writer.process();

        return weight;
    }

    @Override
    public String name()
    {
        return "tcp";
    }

    @Override
    public void close() throws Exception
    {
        conductor.close();
        acceptor.close();
        connector.close();
        reader.close();
        writer.close();
        context.close();
    }

    @Override
    public void forEach(Consumer<? super Nukleus> action)
    {
        action.accept(conductor);
        action.accept(acceptor);
        action.accept(connector);
        action.accept(reader);
        action.accept(writer);
    }
}
