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
package org.kaazing.nuklei.http.internal.routable;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.LongSupplier;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.http.internal.Context;
import org.kaazing.nuklei.http.internal.conductor.Conductor;
import org.kaazing.nuklei.http.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.http.internal.router.Router;

import org.agrona.LangUtil;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;

@Reaktive
public final class Routable extends Nukleus.Composite
{
    private final Context context;
    private final String sourceName;
    private final Router router;
    private final Conductor conductor;
    private final AtomicBuffer writeBuffer;
    private final Map<String, Target> targetsByName;
    private final Long2ObjectHashMap<Route> routesByRef;
    private final Long2ObjectHashMap<Reply> repliesByRef;
    private final Target replyTo;

    public Routable(
        Context context,
        String sourceName,
        String replyName,
        Router router,
        Conductor conductor)
    {
        this.context = context;
        this.sourceName = sourceName;
        this.router = router;
        this.conductor = conductor;
        this.writeBuffer = new UnsafeBuffer(new byte[context.maxMessageLength()]);
        this.targetsByName = new TreeMap<>();
        this.routesByRef = new Long2ObjectHashMap<>();
        this.repliesByRef = new Long2ObjectHashMap<>();
        this.replyTo = targetsByName.computeIfAbsent(replyName, this::supplyTarget);
    }

    @Override
    public String name()
    {
        return sourceName;
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

        final String sourceName = sourcePath.getFileName().toString();
        include(new Source(sourceName, router, routesByRef::get, repliesByRef::get, layout, writeBuffer));
    }

    public void doRoute(
        long correlationId,
        long routableRef,
        String targetName,
        long targetRef,
        String replyName,
        Map<String, String> headers)
    {
        try
        {
            Target target = targetsByName.computeIfAbsent(targetName, this::supplyTarget);

            Route route = routesByRef.computeIfAbsent(routableRef, this::supplyRoute);

            route.add(headers, target, targetRef, replyName);

            conductor.onRoutedResponse(correlationId, sourceName, routableRef, targetName, targetRef, replyName, headers);
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doUnroute(
        long correlationId,
        long routableRef,
        String targetName,
        long targetRef,
        String replyName,
        Map<String, String> headers)
    {
        Route route = routesByRef.get(routableRef);
        Target target = targetsByName.get(targetName);

        if (route != null && target != null)
        {
            if (route.removeIf(headers, target, targetRef, replyName))
            {
                conductor.onUnroutedResponse(correlationId, sourceName, routableRef, targetName, targetRef, replyName, headers);
            }
            else
            {
                conductor.onErrorResponse(correlationId);
            }
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doRouteReply(
        String replyName)
    {
        targetsByName.computeIfAbsent(replyName, this::supplyTarget);
    }

    public void doRegisterReplyEncoder(
        long sourceRef,
        long sourceId,
        String targetName,
        long targetRef,
        long targetId,
        long replyRef,
        long replyId)
    {
        Target target = targetsByName.computeIfAbsent(targetName, this::supplyTarget);
        Reply reply = repliesByRef.computeIfAbsent(sourceRef, v -> new Reply());
        reply.register(sourceId, s -> s.newReplyEncodingStream(target, targetRef, targetId, replyRef, replyId));
    }

    public void doRegisterReplyDecoder(
        long sourceRef,
        long sourceId,
        String targetName,
        long targetRef,
        long targetId,
        long replyRef,
        long replyId)
    {
        Target target = targetsByName.computeIfAbsent(targetName, this::supplyTarget);
        Reply reply = repliesByRef.computeIfAbsent(sourceRef, v -> new Reply());
        reply.register(sourceId, s -> s.newReplyDecodingStream(sourceId, target, targetRef, targetId, replyRef, replyId));
    }

    private Target supplyTarget(
        String targetName)
    {
        StreamsLayout layout = new StreamsLayout.Builder()
                .path(context.routeStreamsPath().apply(sourceName, targetName))
                .streamsCapacity(context.streamsBufferCapacity())
                .throttleCapacity(context.throttleBufferCapacity())
                .readonly(false)
                .build();

        return include(new Target(targetName, layout, writeBuffer));
    }

    private Route supplyRoute(
        long routableRef)
    {
        final boolean isBindRef = (routableRef & 0x01L) == 0x01L;
        HandlerFactory handlerFactory = isBindRef ? Routable::newBindHandler : Routable::newPrepareHandler;
        LongSupplier newTargetId = isBindRef ? this::newAcceptId : this::newConnectId;
        return new Route(handlerFactory, replyTo, newTargetId);
    }

    private static MessageHandler newBindHandler(
        Route route,
        Source source,
        long streamId)
    {
        return source.newInitialDecodingStream(route, streamId);
    }

    private static MessageHandler newPrepareHandler(
        Route route,
        Source source,
        long streamId)
    {
        return source.newInitialEncodingStream(route, streamId);
    }

    private long newAcceptId()
    {
        return newStreamId(context.counters().streamsAccepted());
    }

    private long newConnectId()
    {
        return newStreamId(context.counters().streamsConnected());
    }

    private long newStreamId(
        final AtomicCounter streams)
    {
        streams.increment();
        return streams.get();
    }
}
