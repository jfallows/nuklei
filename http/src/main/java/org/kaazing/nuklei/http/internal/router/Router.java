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
package org.kaazing.nuklei.http.internal.router;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.http.internal.Context;
import org.kaazing.nuklei.http.internal.conductor.Conductor;
import org.kaazing.nuklei.http.internal.routable.Routable;

import uk.co.real_logic.agrona.collections.LongHashSet;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;

@Reaktive
public final class Router extends Nukleus.Composite
{
    private static final Pattern SOURCE_NAME = Pattern.compile("([^#]+)");

    private final Context context;
    private final LongHashSet routableRefs;
    private final Map<String, Routable> routablesBySource;

    private Conductor conductor;

    public Router(
        Context context)
    {
        this.context = context;
        this.routableRefs = new LongHashSet(-1L);
        this.routablesBySource = new HashMap<>();
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
        long correlationId)
    {
        final AtomicCounter streamsBound = context.counters().streamsBound();

        // bind reference: positive, odd
        final long referenceId = (streamsBound.increment() << 1L) | 1L;

        if (routableRefs.add(referenceId))
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
        if (routableRefs.remove(referenceId))
        {
            conductor.onUnboundResponse(correlationId, referenceId);
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

        // prepare reference: positive, even, non-zero
        streamsPrepared.increment();
        final long referenceId = streamsPrepared.get() << 1L;

        if (routableRefs.add(referenceId))
        {
            conductor.onPreparedResponse(correlationId, referenceId);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doUnprepare(
        long correlationId,
        long referenceId)
    {
        if (routableRefs.remove(referenceId))
        {
            conductor.onUnpreparedResponse(correlationId, referenceId);
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
        Map<String, String> headers)
    {
        if (routableRefs.contains(sourceRef))
        {
            Routable target = routablesBySource.computeIfAbsent(targetName, this::newRoutable);
            target.doRouteReply(replyName);

            Routable source = routablesBySource.computeIfAbsent(sourceName, this::newRoutable);
            source.doRoute(correlationId, sourceRef, targetName, targetRef, replyName, headers);
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
        Map<String, String> headers)
    {
        Routable routable = routablesBySource.get(sourceName);
        if (routable != null)
        {
            routable.doUnroute(correlationId, sourceRef, targetName, targetRef, replyName, headers);
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
        Routable routable = routablesBySource.computeIfAbsent(sourceName, this::newRoutable);
        routable.onReadable(sourcePath);
    }

    public void onExpired(
        Path sourcePath)
    {
        // TODO:
    }

    public void doRegisterReplyEncoder(
        String sourceName,
        long sourceRef,
        long sourceId,
        String targetName,
        long targetRef,
        long targetId,
        long replyRef,
        long replyId)
    {
        Routable routable = routablesBySource.computeIfAbsent(sourceName, this::newRoutable);
        routable.doRegisterReplyEncoder(sourceRef, sourceId, targetName, targetRef, targetId, replyRef, replyId);
    }

    public void doRegisterReplyDecoder(
        String sourceName,
        long sourceRef,
        long sourceId,
        String targetName,
        long targetRef,
        long targetId,
        long replyRef,
        long replyId)
    {
        Routable routable = routablesBySource.computeIfAbsent(sourceName, this::newRoutable);
        routable.doRegisterReplyDecoder(sourceRef, sourceId, targetName, targetRef, targetId, replyRef, replyId);
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
        return include(new Routable(context, sourceName, sourceName, this, conductor));
    }
}
