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
package org.kaazing.nuklei.tcp.internal.router;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.acceptor.Acceptor;
import org.kaazing.nuklei.tcp.internal.conductor.Conductor;
import org.kaazing.nuklei.tcp.internal.connector.Connector;
import org.kaazing.nuklei.tcp.internal.reader.Reader;
import org.kaazing.nuklei.tcp.internal.writer.Writer;

import org.agrona.collections.LongHashSet;
import org.agrona.concurrent.status.AtomicCounter;

/**
 * The {@code Router} nukleus manages in-bound and out-bound routes, coordinating with the {@code Acceptable},
 * {@code Readable} and {@code Writable} nuklei as needed.
 */
@Reaktive
public final class Router extends Nukleus.Composite
{
    private static final Pattern SOURCE_NAME = Pattern.compile("([^#]+).*");

    private final Context context;
    private final LongHashSet routableRefs;
    private final Map<String, Reader> readers;
    private final Map<String, Writer> writers;

    private Conductor conductor;
    private Acceptor acceptor;
    private Connector connector;

    public Router(
        Context context)
    {
        this.context = context;
        this.routableRefs = new LongHashSet(-1L);
        this.readers = new HashMap<>();
        this.writers = new HashMap<>();
    }

    public void setConductor(Conductor conductor)
    {
        this.conductor = conductor;
    }

    public void setAcceptor(Acceptor acceptor)
    {
        this.acceptor = acceptor;
    }

    public void setConnector(Connector connector)
    {
        this.connector = connector;
    }

    @Override
    public String name()
    {
        return "router";
    }

    public void doBind(
        long correlationId)
    {
        final AtomicCounter streamsBound = context.counters().streamsBound();

        final long routableRef = RouteKind.BIND.nextRef(streamsBound);

        if (routableRefs.add(routableRef))
        {
            conductor.onBoundResponse(correlationId, routableRef);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doUnbind(
        long correlationId,
        long routableRef)
    {
        if (routableRefs.remove(routableRef))
        {
            conductor.onUnboundResponse(correlationId, routableRef);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doPrepare(
        long correlationId)
    {
        final AtomicCounter streamsPrepared = context.counters().streamsPrepared();

        final long routableRef = RouteKind.PREPARE.nextRef(streamsPrepared);

        if (routableRefs.add(routableRef))
        {
            conductor.onPreparedResponse(correlationId, routableRef);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doUnprepare(
        long correlationId,
        long routableRef)
    {
        if (routableRefs.remove(routableRef))
        {
            conductor.onUnpreparedResponse(correlationId, routableRef);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doRoute(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        // TODO: reply name differs from source name
        if (routableRefs.contains(sourceRef) && replyName.equals(sourceName))
        {
            switch (RouteKind.of(sourceRef))
            {
            case BIND:
                doRouteBind(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
                break;
            case PREPARE:
                doRoutePrepare(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
                break;
            default:
                conductor.onErrorResponse(correlationId);
            }
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doUnroute(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        switch (RouteKind.of(sourceRef))
        {
        case BIND:
            doUnrouteBind(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
            break;
        case PREPARE:
            doUnroutePrepare(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
            break;
        default:
            conductor.onErrorResponse(correlationId);
            break;
        }
    }

    public void onAccepted(
        String sourceName,
        String targetName,
        long targetRef,
        long targetId,
        String replyName,
        long replyRef,
        long replyId,
        SocketChannel channel)
    {
        Writer writer = writers.computeIfAbsent(targetName, this::newWriter);
        writer.doRouteReply(replyName, replyRef, replyId, channel);

        Reader reader = readers.computeIfAbsent(sourceName, this::newReader);
        reader.doBegin(targetName, targetRef, targetId, replyRef, replyId, channel);
    }

    public void onConnected(
        String targetName,
        String sourceName,
        long sourceRef,
        long sourceId,
        String replyName,
        long replyRef,
        long replyId,
        SocketChannel channel)
    {
        Writer writer = writers.computeIfAbsent(sourceName, this::newWriter);
        writer.doRouteReply(replyName, replyRef, replyId, channel);

        Reader reader = readers.computeIfAbsent(targetName, this::newReader);
        reader.doBegin(replyName, replyRef, replyId, sourceRef, sourceId, channel);
    }

    public void onReadable(
        Path sourcePath)
    {
        String sourceName = source(sourcePath);
        Writer writer = writers.computeIfAbsent(sourceName, this::newWriter);
        writer.onReadable(sourcePath);
    }

    public void onExpired(
        Path sourcePath)
    {
        // TODO:
    }

    private static String source(
        Path path)
    {
        Matcher matcher = SOURCE_NAME.matcher(path.getName(path.getNameCount() - 1).toString());
        if (matcher.matches())
        {
            return matcher.group(1);
        }
        else
        {
            throw new IllegalStateException();
        }
    }

    private void doRouteBind(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        Reader reader = readers.computeIfAbsent(sourceName, this::newReader);
        reader.doRoute(targetName);

        acceptor.doRoute(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
    }

    private void doRoutePrepare(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        Reader reader = readers.computeIfAbsent(targetName, this::newReader);
        reader.doRoute(sourceName);

        Writer writer = writers.computeIfAbsent(sourceName, this::newWriter);
        writer.doRoute(correlationId, sourceRef, targetName, targetRef, replyName, address);
    }

    private void doUnrouteBind(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        if (acceptor != null)
        {
            acceptor.doUnroute(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    private void doUnroutePrepare(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        Writer writer = writers.get(sourceName);
        if (writer != null)
        {
            writer.doUnroute(correlationId, sourceRef, targetName, targetRef, replyName, address);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    private Reader newReader(
        String sourceName)
    {
        return include(new Reader(context, this, sourceName));
    }

    private Writer newWriter(
        String sourceName)
    {
        return include(new Writer(context, conductor, connector, sourceName));
    }
}
