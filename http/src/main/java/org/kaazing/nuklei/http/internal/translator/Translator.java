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
package org.kaazing.nuklei.http.internal.translator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.http.internal.Context;
import org.kaazing.nuklei.http.internal.conductor.ConductorProxy;
import org.kaazing.nuklei.http.internal.layouts.StreamsLayout;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class Translator implements Nukleus, Consumer<TranslatorCommand>
{
    private final ConductorProxy conductorProxy;
    private final OneToOneConcurrentArrayQueue<TranslatorCommand> commandQueue;

    private final AtomicCounter streamsBound;
    private final AtomicCounter streamsPrepared;

    private final Map<String, StreamsLayout> capturedStreams;
    private final Map<String, StreamsLayout> routedStreams;

    private final Long2ObjectHashMap<BindState> bindStateByRef;
    private final Long2ObjectHashMap<PrepareState> prepareStateByRef;

    private final Function<String, File> captureStreamsFile;
    private final Function<String, File> routeStreamsFile;
    private final int streamsCapacity;

    public Translator(Context context)
    {
        this.conductorProxy = new ConductorProxy(context);
        this.streamsBound = context.counters().streamsBound();
        this.streamsPrepared = context.counters().streamsPrepared();
        this.commandQueue = context.translatorCommandQueue();
        this.captureStreamsFile = context.captureStreamsFile();
        this.routeStreamsFile = context.routeStreamsFile();
        this.streamsCapacity = context.streamsCapacity();
        this.capturedStreams = new HashMap<>();
        this.routedStreams = new HashMap<>();
        this.bindStateByRef = new Long2ObjectHashMap<>();
        this.prepareStateByRef = new Long2ObjectHashMap<>();
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += commandQueue.drain(this);

        // TODO: read streams

        return weight;
    }

    @Override
    public String name()
    {
        return "translator";
    }

    @Override
    public void accept(TranslatorCommand command)
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

                TranslatorState newState = new TranslatorState(source, newCapture.buffer());
                newCapture.attach(newState);

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

    public void doBind(
        long correlationId,
        String source,
        long sourceRef,
        String handler,
        Object headers)
    {
        StreamsLayout capture = capturedStreams.get(source);
        StreamsLayout route = routedStreams.get(handler);

        if (capture == null || route == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                final long handlerRef = streamsBound.increment();

                TranslatorState state = (TranslatorState) capture.attachment();

                BindState newBindState = state.doBind(sourceRef, headers, handler, handlerRef, route.buffer());
                bindStateByRef.put(handlerRef, newBindState);

                conductorProxy.onBoundResponse(correlationId, handlerRef);
            }
            catch (Exception e)
            {
                conductorProxy.onErrorResponse(correlationId);
                throw new RuntimeException(e);
            }
        }
    }

    public void doUnbind(
        long correlationId,
        long referenceId)
    {
        BindState oldBindState = bindStateByRef.remove(referenceId);

        if (oldBindState == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                String source = oldBindState.source();
                long sourceRef = oldBindState.sourceRef();
                String handler = oldBindState.handler();
                Object headers = oldBindState.headers();

                // TODO: translatorState doUnbind

                conductorProxy.onUnboundResponse(correlationId, source, sourceRef, handler, headers);
            }
            catch (Exception e)
            {
                conductorProxy.onErrorResponse(correlationId);
                throw new RuntimeException(e);
            }
        }
    }

    public void doPrepare(
        long correlationId,
        String destination,
        long destinationRef,
        String handler,
        Object headers)
    {
        StreamsLayout capture = capturedStreams.get(destination);
        StreamsLayout route = routedStreams.get(handler);

        if (capture == null || route == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                final long handlerRef = streamsPrepared.increment();

                TranslatorState state = (TranslatorState) capture.attachment();

                PrepareState newPrepareState = state.doPrepare(destinationRef, headers, handler, handlerRef, route.buffer());
                prepareStateByRef.put(handlerRef, newPrepareState);

                conductorProxy.onPreparedResponse(correlationId, handlerRef);
            }
            catch (Exception e)
            {
                conductorProxy.onErrorResponse(correlationId);
                throw new RuntimeException(e);
            }
        }
    }

    public void doUnprepare(
        long correlationId,
        long referenceId)
    {
        PrepareState oldPrepareState = prepareStateByRef.remove(referenceId);

        if (oldPrepareState == null)
        {
            conductorProxy.onErrorResponse(correlationId);
        }
        else
        {
            try
            {
                String destination = oldPrepareState.destination();
                long destinationRef = oldPrepareState.destinationRef();
                String handler = oldPrepareState.handler();
                Object headers = oldPrepareState.headers();

                // TODO: translatorState doUnprepare

                conductorProxy.onUnpreparedResponse(correlationId, destination, destinationRef, handler, headers);
            }
            catch (Exception e)
            {
                conductorProxy.onErrorResponse(correlationId);
                throw new RuntimeException(e);
            }
        }
    }
}
