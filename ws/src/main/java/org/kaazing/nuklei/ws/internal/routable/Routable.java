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
package org.kaazing.nuklei.ws.internal.routable;

import static java.util.Collections.emptyList;
import static org.kaazing.nuklei.ws.internal.routable.Route.protocolMatches;
import static org.kaazing.nuklei.ws.internal.routable.Route.sourceMatches;
import static org.kaazing.nuklei.ws.internal.routable.Route.sourceRefMatches;
import static org.kaazing.nuklei.ws.internal.routable.Route.targetMatches;
import static org.kaazing.nuklei.ws.internal.routable.Route.targetRefMatches;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

import org.agrona.LangUtil;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.ws.internal.Context;
import org.kaazing.nuklei.ws.internal.conductor.Conductor;
import org.kaazing.nuklei.ws.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.ws.internal.router.Correlation;
import org.kaazing.nuklei.ws.internal.util.function.LongObjectBiConsumer;

@Reaktive
public final class Routable extends Nukleus.Composite
{
    private static final List<Route> EMPTY_ROUTES = emptyList();

    private final Context context;
    private final String sourceName;
    private final Conductor conductor;
    private final AtomicBuffer writeBuffer;
    private final Map<String, Source> sourcesByPartitionName;
    private final Map<String, Target> targetsByName;
    private final Long2ObjectHashMap<List<Route>> routesByRef;
    private final LongObjectBiConsumer<Correlation> correlateInitial;
    private final LongFunction<Correlation> correlateReply;
    private final LongSupplier supplyTargetId;

    public Routable(
        Context context,
        Conductor conductor,
        String sourceName,
        LongObjectBiConsumer<Correlation> correlateInitial,
        LongFunction<Correlation> correlateReply)
    {
        this.context = context;
        this.conductor = conductor;
        this.sourceName = sourceName;
        this.correlateInitial = correlateInitial;
        this.correlateReply = correlateReply;
        this.writeBuffer = new UnsafeBuffer(new byte[context.maxMessageLength()]);
        this.sourcesByPartitionName = new HashMap<>();
        this.targetsByName = new HashMap<>();
        this.routesByRef = new Long2ObjectHashMap<>();
        this.supplyTargetId = context.counters().streamsTargeted()::increment;
    }

    @Override
    public String name()
    {
        return sourceName;
    }

    public void onReadable(
        String partitionName)
    {
        sourcesByPartitionName.computeIfAbsent(partitionName, this::newSource);
    }

    public void doRoute(
        long correlationId,
        long sourceRef,
        String targetName,
        long targetRef,
        String protocol)
    {
        try
        {
            final Target target = targetsByName.computeIfAbsent(targetName, this::newTarget);
            final Route newRoute = new Route(sourceName, sourceRef, target, targetRef, protocol);

            routesByRef.computeIfAbsent(sourceRef, this::newRoutes)
                       .add(newRoute);

            conductor.onRoutedResponse(correlationId);
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
        String protocol)
    {
        final List<Route> routes = supplyRoutes(sourceRef);

        final Predicate<Route> filter =
                sourceMatches(sourceName)
                 .and(sourceRefMatches(sourceRef))
                 .and(targetMatches(targetName))
                 .and(targetRefMatches(targetRef))
                 .and(protocolMatches(protocol));

        if (routes.removeIf(filter))
        {
            conductor.onUnroutedResponse(correlationId);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    private List<Route> newRoutes(
        long sourceRef)
    {
        return new ArrayList<>();
    }

    private List<Route> supplyRoutes(
        long referenceId)
    {
        return routesByRef.getOrDefault(referenceId, EMPTY_ROUTES);
    }

    private Source newSource(
        String partitionName)
    {
        StreamsLayout layout = new StreamsLayout.Builder()
            .path(context.sourceStreamsPath().apply(partitionName))
            .streamsCapacity(context.streamsBufferCapacity())
            .throttleCapacity(context.throttleBufferCapacity())
            .readonly(true)
            .build();

        return include(new Source(sourceName, partitionName, layout, writeBuffer,
                                  this::supplyRoutes, supplyTargetId,
                                  correlateInitial, correlateReply));
    }

    private Target newTarget(
        String targetName)
    {
        StreamsLayout layout = new StreamsLayout.Builder()
                .path(context.targetStreamsPath().apply(sourceName, targetName))
                .streamsCapacity(context.streamsBufferCapacity())
                .throttleCapacity(context.throttleBufferCapacity())
                .readonly(false)
                .build();

        return include(new Target(targetName, layout, writeBuffer));
    }
}
