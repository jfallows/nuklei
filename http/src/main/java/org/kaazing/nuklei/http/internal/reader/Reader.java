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
import java.util.function.Function;

import javax.annotation.Resource;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.http.internal.Context;
import org.kaazing.nuklei.http.internal.conductor.Conductor;
import org.kaazing.nuklei.http.internal.layouts.StreamsLayout;
import org.kaazing.nuklei.http.internal.readable.Readable;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.collections.ArrayUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

@Reaktive
public final class Reader implements Nukleus
{
    private final Context context;

    private final Map<String, StreamsLayout> capturedStreams;
    private final Map<String, StreamsLayout> routedStreams;

    private final Long2ObjectHashMap<String> boundSources;
    private final Long2ObjectHashMap<String> preparedSources;

    private final Function<String, File> captureStreamsFile;
    private final Function<String, File> routeStreamsFile;
    private final int streamsCapacity;

    private Conductor conductor;
    private Readable[] readables;

    public Reader(Context context)
    {
        this.context = context;
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

        int length = readables.length;
        for (int i = 0; i < length; i++)
        {
            weight += readables[i].process();
        }

        return weight;
    }

    @Resource
    public void setConductor(Conductor conductor)
    {
        this.conductor = conductor;
    }

    @Override
    public String name()
    {
        return "reader";
    }

    public void doCapture(
        long correlationId,
        String source)
    {
        StreamsLayout capture = capturedStreams.get(source);
        if (capture != null)
        {
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                StreamsLayout newCapture = new StreamsLayout.Builder().streamsFile(captureStreamsFile.apply(source))
                                                                      .streamsCapacity(streamsCapacity)
                                                                      .createFile(true)
                                                                      .build();

                Readable newReadable = new Readable(context, conductor, this, source, newCapture.buffer());

                readables = ArrayUtil.add(readables, newReadable);

                newCapture.attach(newReadable);

                capturedStreams.put(source, newCapture);

                conductor.onCapturedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductor.onErrorResponse(correlationId);
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
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                Readable oldReadable = (Readable) oldCapture.attachment();

                readables = ArrayUtil.remove(readables, oldReadable);

                oldCapture.close();

                conductor.onUncapturedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductor.onErrorResponse(correlationId);
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
            conductor.onErrorResponse(correlationId);
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

                conductor.onRoutedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductor.onErrorResponse(correlationId);
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
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                oldRoute.close();
                conductor.onUnroutedResponse(correlationId);
            }
            catch (Exception ex)
            {
                conductor.onErrorResponse(correlationId);
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

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
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            Readable source = (Readable) sourceCapture.attachment();
            RingBuffer sourceBuffer = sourceRoute.buffer();
            Readable destination = (Readable) destinationCapture.attachment();
            RingBuffer destinationBuffer = destinationRoute.buffer();

            source.doBind(correlationId, destinationRef, headers, destination, sourceBuffer, destinationBuffer);
        }
    }

    public void doUnbind(
        long correlationId,
        long referenceId)
    {
        String source = boundSources.remove(referenceId);

        if (source == null)
        {
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            StreamsLayout capture = capturedStreams.get(source);
            Readable sourceReadable = (Readable) capture.attachment();

            sourceReadable.doUnbind(correlationId, referenceId);
        }
    }

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
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            Readable source = (Readable) sourceCapture.attachment();
            RingBuffer sourceBuffer = sourceRoute.buffer();

            Readable destination = (Readable) destinationCapture.attachment();
            RingBuffer destinationBuffer = destinationRoute.buffer();

            source.doPrepare(correlationId, destinationRef, headers, destination, sourceBuffer, destinationBuffer);
        }
    }

    public void doUnprepare(
        long correlationId,
        long referenceId)
    {
        String source = preparedSources.remove(referenceId);

        if (source == null)
        {
            conductor.onErrorResponse(correlationId);
        }
        else
        {
            StreamsLayout capture = capturedStreams.get(source);
            Readable sourceReadable = (Readable) capture.attachment();

            sourceReadable.doUnprepare(correlationId, referenceId);
        }
    }

    public void onBoundResponse(
        String source,
        long correlationId,
        long referenceId)
    {
        boundSources.put(referenceId, source);

        conductor.onBoundResponse(correlationId, referenceId);
    }

    public void onPreparedResponse(
        String source,
        long correlationId,
        long referenceId)
    {
        preparedSources.put(referenceId, source);

        conductor.onPreparedResponse(correlationId, referenceId);
    }
}
