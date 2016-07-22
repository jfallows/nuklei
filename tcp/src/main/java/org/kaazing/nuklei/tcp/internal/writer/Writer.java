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
package org.kaazing.nuklei.tcp.internal.writer;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.conductor.Conductor;
import org.kaazing.nuklei.tcp.internal.connector.Connector;
import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;

import org.agrona.LangUtil;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * The {@code Writable} nukleus reads streams data from multiple {@code Source} nuklei and monitors completion of
 * partial socket writes via a {@code Target} nukleus.
 */
public final class Writer extends Nukleus.Composite
{
    private final Context context;
    private final Conductor conductor;
    private final Connector connector;
    private final String name;
    private final String sourceName;
    private final AtomicBuffer writeBuffer;
    private final Map<String, Target> targetsByName;
    private final Long2ObjectHashMap<Route> routesByRef;
    private final Long2ObjectHashMap<Reply> repliesByRef;

    public Writer(
        Context context,
        Conductor conductor,
        Connector connector,
        String sourceName)
    {
        this.context = context;
        this.conductor = conductor;
        this.connector = connector;
        this.sourceName = sourceName;
        this.name = String.format("writer[%s]", sourceName);
        this.writeBuffer = new UnsafeBuffer(new byte[context.maxMessageLength()]);
        this.targetsByName = new HashMap<>();
        this.routesByRef = new Long2ObjectHashMap<>();
        this.repliesByRef = new Long2ObjectHashMap<>();
    }

    @Override
    public String name()
    {
        return name;
    }

    public void onReadable(
        Path sourcePath)
    {
        StreamsLayout layout = new StreamsLayout.Builder()
            .path(sourcePath)
            .streamsCapacity(context.streamsBufferCapacity())
            .throttleCapacity(context.throttleBufferCapacity())
            .readonly(true)
            .build();

        final String partitionName = sourcePath.getFileName().toString();
        include(new Source(partitionName, this, routesByRef::get, repliesByRef::get, layout, writeBuffer));
    }

    public void doRoute(
        long correlationId,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        try
        {
            final Target target = targetsByName.computeIfAbsent(targetName, this::supplyTarget);
            final Route newRoute = new Route(sourceName, sourceRef, target, targetRef, replyName, address);

            routesByRef.put(sourceRef, newRoute);

            conductor.onRoutedResponse(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doUnroute(
        long correlationId,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        Route route = routesByRef.get(sourceRef);

        if (route != null &&
                route.sourceRef() == sourceRef &&
                route.target().name().equals(targetName) &&
                route.targetRef() == targetRef &&
                route.reply().equals(replyName) &&
                route.address().equals(address))
        {
            routesByRef.remove(sourceRef);
            conductor.onUnroutedResponse(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doRouteReply(
        String replyName,
        long replyRef,
        long replyId,
        SocketChannel channel)
    {
        final Target target = targetsByName.computeIfAbsent(replyName, this::supplyTarget);
        final Reply reply = repliesByRef.computeIfAbsent(replyRef, v -> new Reply());

        reply.register(replyId, target, channel);
    }

    public void doConnect(
        Source source,
        long sourceRef,
        long sourceId,
        String targetName,
        long targetRef,
        String replyName,
        long replyRef,
        long replyId,
        SocketChannel channel,
        InetSocketAddress address)
    {
        final Runnable onfailure = () -> onConnectFailed(source, sourceId);
        final Runnable onsuccess = () -> onConnectSucceeded(source, sourceId, sourceRef, replyId, replyRef);
        connector.doConnect(sourceName, sourceRef, sourceId, targetName, targetRef, replyName, replyRef, replyId,
                channel, address, onsuccess, onfailure);
    }

    public void onConnectSucceeded(
        Source source,
        long sourceId,
        long sourceRef,
        long replyId,
        long replyRef)
    {
        source.doBegin(sourceId, sourceRef, replyId, replyRef);
    }

    public void onConnectFailed(
        Source source,
        long sourceId)
    {
        source.doReset(sourceId);
    }

    private Target supplyTarget(
        String targetName)
    {
        return include(new Target(targetName));
    }
}
