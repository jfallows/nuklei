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
package org.kaazing.nuklei.tcp.internal;

import java.io.Closeable;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.acceptor.Acceptor;
import org.kaazing.nuklei.tcp.internal.conductor.Conductor;
import org.kaazing.nuklei.tcp.internal.connector.Connector;
import org.kaazing.nuklei.tcp.internal.reader.Reader;
import org.kaazing.nuklei.tcp.internal.writer.Writer;

public final class TcpNukleus extends Nukleus.Composite
{
    static final String NAME = "tcp";

    private final Closeable cleaner;

    TcpNukleus(
        Conductor conductor,
        Acceptor acceptor,
        Connector connector,
        Reader reader,
        Writer writer,
        Closeable cleanup)
    {
        super(conductor, acceptor, connector, reader, writer);
        this.cleaner = cleanup;
    }

    @Override
    public String name()
    {
        return NAME;
    }

    @Override
    public void close() throws Exception
    {
        super.close();
        cleaner.close();
    }
}
