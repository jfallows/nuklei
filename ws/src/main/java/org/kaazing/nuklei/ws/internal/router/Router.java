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
package org.kaazing.nuklei.ws.internal.router;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.LongHashSet;
import org.agrona.concurrent.status.AtomicCounter;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.ws.internal.Context;
import org.kaazing.nuklei.ws.internal.conductor.Conductor;
import org.kaazing.nuklei.ws.internal.routable.Routable;

@Reaktive
public final class Router extends Nukleus.Composite
{
    private static final Pattern SOURCE_NAME = Pattern.compile("([^#]+).*");

    private final Context context;
    private final LongHashSet referenceIds;
    private final Map<String, Routable> routables;
    private final Long2ObjectHashMap<Correlation> correlations;

    private Conductor conductor;

    public Router(
        Context context)
    {
        this.context = context;
        this.referenceIds = new LongHashSet(-1L);
        this.routables = new HashMap<>();
        this.correlations = new Long2ObjectHashMap<>();
    }

    public void setConductor(Conductor conductor)
    {
        this.conductor = conductor;
    }

    @Override
    public String name()
    {
        return "router";
    }

    public void doBind(
        long correlationId,
        int kind)
    {
        final AtomicCounter targetsBound = context.counters().targetsBound();

        final RouteKind routeKind = RouteKind.of(kind);
        final long referenceId = routeKind.nextRef(targetsBound);

        if (referenceIds.add(referenceId))
        {
            conductor.onBoundResponse(correlationId, referenceId);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doUnbind(
        long correlationId,
        long referenceId)
    {
        if (referenceIds.remove(referenceId))
        {
            conductor.onUnboundResponse(correlationId);
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
        String protocol)
    {
        if (referenceIds.contains(sourceRef) && RouteKind.valid(sourceRef))
        {
            Routable routable = routables.computeIfAbsent(sourceName, this::newRoutable);
            routable.doRoute(correlationId, sourceRef, targetName, targetRef, protocol);
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
        String protocol)
    {
        final Routable routable = routables.get(sourceName);
        if (routable != null && referenceIds.contains(sourceRef))
        {
            routable.doUnroute(correlationId, sourceRef, targetName, targetRef, protocol);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void onReadable(
        Path sourcePath)
    {
        String sourceName = source(sourcePath);
        Routable routable = routables.computeIfAbsent(sourceName, this::newRoutable);
        String partitionName = sourcePath.getFileName().toString();
        routable.onReadable(partitionName);
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

    private Routable newRoutable(
        String sourceName)
    {
        return include(new Routable(context, conductor, sourceName, correlations::put, correlations::remove));
    }
}
