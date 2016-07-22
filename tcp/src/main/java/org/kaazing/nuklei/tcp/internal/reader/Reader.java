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
package org.kaazing.nuklei.tcp.internal.reader;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.TreeMap;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.tcp.internal.router.Router;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * The {@code Readable} nukleus reads network traffic via a {@code Source} nukleus and control flow commands
 * from multiple {@code Target} nuklei.
 */
@Reaktive
public final class Reader extends Nukleus.Composite
{
    private final Context context;
    private final String sourceName;
    private final Source source;
    private final Map<String, Target> targetsByName;
    private final AtomicBuffer writeBuffer;

    public Reader(
        Context context,
        Router router,
        String sourceName)
    {
        this.context = context;
        this.sourceName = sourceName;
        this.source = include(new Source(router, context.maxMessageLength()));
        this.writeBuffer = new UnsafeBuffer(new byte[context.maxMessageLength()]);
        this.targetsByName = new TreeMap<>();
    }

    @Override
    public String name()
    {
        return String.format("reader[%s]", sourceName);
    }

    public void doBegin(
        String targetName,
        long targetRef,
        long targetId,
        long replyRef,
        long replyId,
        SocketChannel channel)
    {
        Target target = targetsByName.computeIfAbsent(targetName, this::supplyTarget);
        source.doBegin(target, targetRef, targetId, replyRef, replyId, channel);
    }

    public void doRoute(
        String targetName)
    {
        targetsByName.computeIfAbsent(targetName, this::supplyTarget);
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
}
