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
package org.kaazing.nuklei.http.internal.reader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.http.internal.Context;
import org.kaazing.nuklei.http.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.http.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.http.internal.readable.Readable;
import org.kaazing.nuklei.http.internal.readable.ReadableProxy;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.collections.ArrayUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Reader implements Nukleus, Consumer<ReaderCommand>
{
    private final ConductorProxy conductorProxy;
    private final ManyToOneConcurrentArrayQueue<ReaderCommand> commandQueue;

    private final Context context;

    private final Map<String, StreamsLayout> capturedStreams;
    private final Map<String, StreamsLayout> routedStreams;

    private final Long2ObjectHashMap<String> boundSources;
    private final Long2ObjectHashMap<String> preparedSources;

    private final Function<String, File> captureStreamsFile;
    private final Function<String, File> routeStreamsFile;
    private final int streamsCapacity;

    private Readable[] readables;

    public Reader(Context context)
    {
        this.context = context;
        this.conductorProxy = new ConductorProxy(context);
        this.commandQueue = context.readerCommandQueue();
        this.captureStreamsFile = context.captureStreamsFile();
        this.routeStreamsFile = context.routeStreamsFile();
        this.streamsCapacity = context.streamsBufferCapacity();
        this.capturedStreams = new HashMap<>();
        this.routedStreams = new HashMap<>();
        this.boundSources = new Long2ObjectHashMap<>();
        this.preparedSources = new Long2ObjectHashMap<>();
        this.readables = new Readable[0];
    }

    @Override
    public int process()
    {
        int weight = 0;

        weight += commandQueue.drain(this);

        int length = readables.length;
        for (int i = 0; i < length; i++)
        {
            weight += readables[i].process();
        }

        return weight;
    }

    @Override
    public String name()
    {
        return "reader";
    }

    @Override
    public void accept(ReaderCommand command)
    {
        command.execute(this);
    }

    public void doCapture(
        long correlationId,
        String source)
    {
        StreamsLayout capture = capturedStreams.get(source);
        if (capture != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                StreamsLayout newCapture = new StreamsLayout.Builder().streamsFile(captureStreamsFile.apply(source))
                                                                      .streamsCapacity(streamsCapacity)
                                                                      .createFile(true)
                                                                      .build();

                Readable newReadable = new Readable(context, source, newCapture.buffer());

                readables = ArrayUtil.add(readables, newReadable);

                newCapture.attach(newReadable);

                capturedStreams.put(source, newCapture);

                conductorProxy.onCapturedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doUncapture(
        long correlationId,
        String source)
    {
        StreamsLayout oldCapture = capturedStreams.remove(source);
        if (oldCapture == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                Readable oldReadable = (Readable) oldCapture.attachment();

                readables = ArrayUtil.remove(readables, oldReadable);

                oldCapture.close();

                conductorProxy.onUncapturedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doRoute(
        long correlationId,
        String destination)
    {
        StreamsLayout route = routedStreams.get(destination);
        if (route != null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                StreamsLayout newRoute = new StreamsLayout.Builder().streamsFile(routeStreamsFile.apply(destination))
                                                                    .streamsCapacity(streamsCapacity)
                                                                    .createFile(false)
                                                                    .build();

                routedStreams.put(destination, newRoute);

                conductorProxy.onRoutedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    public void doUnroute(
        long correlationId,
        String destination)
    {
        StreamsLayout oldRoute = routedStreams.remove(destination);
        if (oldRoute == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                oldRoute.close();
                conductorProxy.onUnroutedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductorProxy.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    @Reaktive
    public void doBind(
        long correlationId,
        String destinationName,
        long destinationRef,
        String sourceName,
        Map<String, String> headers)
    {
        StreamsLayout sourceCapture = capturedStreams.get(sourceName);
        StreamsLayout sourceRoute = routedStreams.get(sourceName);
        StreamsLayout destinationCapture = capturedStreams.get(destinationName);
        StreamsLayout destinationRoute = routedStreams.get(destinationName);

        if (sourceCapture == null || sourceRoute == null || destinationCapture == null || destinationRoute == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            Readable source = (Readable) sourceCapture.attachment();
            ReadableProxy sourceProxy = source.proxy();
            RingBuffer sourceBuffer = sourceRoute.buffer();
            Readable destination = (Readable) destinationCapture.attachment();
            ReadableProxy destinationProxy = destination.proxy();
            RingBuffer destinationBuffer = destinationRoute.buffer();

            sourceProxy.doBind(correlationId, destinationRef, headers, destinationProxy, sourceBuffer, destinationBuffer);
        }
    }

    @Reaktive
    public void doUnbind(
        long correlationId,
        long referenceId)
    {
        String source = boundSources.remove(referenceId);

        if (source == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            StreamsLayout capture = capturedStreams.get(source);
            Readable sourceReadable = (Readable) capture.attachment();

            ReadableProxy sourceProxy = sourceReadable.proxy();
            sourceProxy.doUnbind(correlationId, referenceId);
        }
    }

    @Reaktive
    public void doPrepare(
        long correlationId,
        String destinationName,
        long destinationRef,
        String sourceName,
        Map<String, String> headers)
    {
        StreamsLayout sourceCapture = capturedStreams.get(sourceName);
        StreamsLayout sourceRoute = routedStreams.get(sourceName);
        StreamsLayout destinationCapture = capturedStreams.get(destinationName);
        StreamsLayout destinationRoute = routedStreams.get(destinationName);

        if (sourceCapture == null || sourceRoute == null || destinationCapture == null || destinationRoute == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            Readable source = (Readable) sourceCapture.attachment();
            ReadableProxy sourceProxy = source.proxy();
            RingBuffer sourceBuffer = sourceRoute.buffer();

            Readable destination = (Readable) destinationCapture.attachment();
            ReadableProxy destinationProxy = destination.proxy();
            RingBuffer destinationBuffer = destinationRoute.buffer();

            sourceProxy.doPrepare(correlationId, destinationRef, headers, destinationProxy, sourceBuffer, destinationBuffer);
        }
    }

    @Reaktive
    public void doUnprepare(
        long correlationId,
        long referenceId)
    {
        String source = preparedSources.remove(referenceId);

        if (source == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            StreamsLayout capture = capturedStreams.get(source);
            Readable sourceReadable = (Readable) capture.attachment();

            ReadableProxy sourceProxy = sourceReadable.proxy();
            sourceProxy.doUnprepare(correlationId, referenceId);
        }
    }

    @Reaktive
    public void onBoundResponse(
        String source,
        long correlationId,
        long referenceId)
    {
        boundSources.put(referenceId, source);

        conductorProxy.onBoundResponse(correlationId, referenceId);
    }

    @Reaktive
    public void onPreparedResponse(
        String source,
        long correlationId,
        long referenceId)
    {
        preparedSources.put(referenceId, source);

        conductorProxy.onPreparedResponse(correlationId, referenceId);
    }
}
