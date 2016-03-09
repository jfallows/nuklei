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

import java.io.File;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.NukleusFactorySpi;
import org.kaazing.nuklei.tcp.internal.acceptor.Acceptor;
import org.kaazing.nuklei.tcp.internal.conductor.Conductor;
import org.kaazing.nuklei.tcp.internal.connector.Connector;
import org.kaazing.nuklei.tcp.internal.reader.Reader;
import org.kaazing.nuklei.tcp.internal.writer.Writer;

public final class TcpNukleusFactorySpi implements NukleusFactorySpi
{
    @Override
    public String name()
    {
        return TcpNukleus.NAME;
    }

    @Override
    public TcpNukleus create(Configuration config)
    {
        Context context = new Context();
        context.controlFile(new File(config.directory(), "tcp/control"))
               .conclude(config);

        Conductor conductor = new Conductor(context);
        Acceptor acceptor = new Acceptor(context);
        Connector connector = new Connector(context);
        Reader reader = new Reader(context);
        Writer writer = new Writer(context);

        conductor.setAcceptor(acceptor);
        conductor.setConnector(connector);
        conductor.setReader(reader);
        conductor.setWriter(writer);

        acceptor.setConductor(conductor);
        acceptor.setReader(reader);
        acceptor.setWriter(writer);

        connector.setConductor(conductor);
        connector.setReader(reader);
        connector.setWriter(writer);

        reader.setConductor(conductor);
        reader.setConnector(connector);

        writer.setConductor(conductor);

        return new TcpNukleus(conductor, acceptor, connector, reader, writer, context);
    }
}
