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
package org.kaazing.nuklei.tcp.internal;

import static java.lang.String.format;
import static uk.co.real_logic.agrona.LangUtil.rethrowUnchecked;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;
import java.util.logging.Logger;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.tcp.internal.layouts.ControlLayout;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Context implements Closeable
{
    private final ControlLayout.Builder controlRW = new ControlLayout.Builder();

    private boolean readonly;
    private File configDirectory;
    private ControlLayout controlRO;
    private int streamsBufferCapacity;
    private Function<String, File> captureStreamsFile;
    private Function<String, File> routeStreamsFile;
    private IdleStrategy idleStrategy;
    private ErrorHandler errorHandler;
    private CountersManager countersManager;
    private Counters counters;
    private RingBuffer toConductorCommands;
    private BroadcastTransmitter fromConductorResponses;
    private AtomicBuffer conductorResponseBuffer;

    public Context readonly(boolean readonly)
    {
        this.readonly = readonly;
        return this;
    }

    public boolean readonly()
    {
        return readonly;
    }

    public Context controlFile(File controlFile)
    {
        this.controlRW.controlFile(controlFile);
        return this;
    }

    public int streamsBufferCapacity()
    {
        return streamsBufferCapacity;
    }

    public Context captureStreamsFile(Function<String, File> captureStreamsFile)
    {
        this.captureStreamsFile = captureStreamsFile;
        return this;
    }

    public Function<String, File> captureStreamsFile()
    {
        return captureStreamsFile;
    }

    public Context routeStreamsFile(Function<String, File> routeStreamsFile)
    {
        this.routeStreamsFile = routeStreamsFile;
        return this;
    }

    public Function<String, File> routeStreamsFile()
    {
        return routeStreamsFile;
    }

    public Context idleStrategy(IdleStrategy idleStrategy)
    {
        this.idleStrategy = idleStrategy;
        return this;
    }

    public IdleStrategy idleStrategy()
    {
        return idleStrategy;
    }

    public Context errorHandler(ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
        return this;
    }

    public ErrorHandler errorHandler()
    {
        return errorHandler;
    }

    public Context counterLabelsBuffer(AtomicBuffer counterLabelsBuffer)
    {
        controlRW.counterLabelsBuffer(counterLabelsBuffer);
        return this;
    }

    public Context counterValuesBuffer(AtomicBuffer counterValuesBuffer)
    {
        controlRW.counterValuesBuffer(counterValuesBuffer);
        return this;
    }

    public Context conductorCommands(RingBuffer conductorCommands)
    {
        this.toConductorCommands = conductorCommands;
        return this;
    }

    public RingBuffer conductorCommands()
    {
        return toConductorCommands;
    }

    public void conductorResponseBuffer(AtomicBuffer conductorResponseBuffer)
    {
        this.conductorResponseBuffer = conductorResponseBuffer;
    }

    public AtomicBuffer conductorResponseBuffer()
    {
        return conductorResponseBuffer;
    }

    public Context conductorResponses(BroadcastTransmitter conductorResponses)
    {
        this.fromConductorResponses = conductorResponses;
        return this;
    }

    public BroadcastTransmitter conductorResponses()
    {
        return fromConductorResponses;
    }

    public Logger acceptorLogger()
    {
        return Logger.getLogger("nuklei.tcp.acceptor");
    }

    public Context countersManager(CountersManager countersManager)
    {
        this.countersManager = countersManager;
        return this;
    }

    public CountersManager countersManager()
    {
        return countersManager;
    }

    public Counters counters()
    {
        return counters;
    }

    public Context conclude(Configuration config)
    {
        try
        {
            this.configDirectory = config.directory();

            this.streamsBufferCapacity = config.streamsBufferCapacity();

            captureStreamsFile((source) -> {
                return new File(configDirectory, format("tcp/streams/%s", source));
            });

            routeStreamsFile((destination) -> {
                return new File(configDirectory, format("%s/streams/tcp", destination));
            });

            this.controlRO = controlRW.controlFile(new File(configDirectory, "tcp/control"))
                                      .commandBufferCapacity(config.commandBufferCapacity())
                                      .responseBufferCapacity(config.responseBufferCapacity())
                                      .counterLabelsBufferCapacity(config.counterLabelsBufferCapacity())
                                      .counterValuesBufferCapacity(config.counterValuesBufferCapacity())
                                      .readonly(readonly())
                                      .build();

            conductorCommands(
                    new ManyToOneRingBuffer(controlRO.commandBuffer()));

            conductorResponseBuffer(controlRO.responseBuffer());

            conductorResponses(
                    new BroadcastTransmitter(conductorResponseBuffer()));

            concludeCounters();
        }
        catch (Exception ex)
        {
            rethrowUnchecked(ex);
        }

        return this;
    }

    @Override
    public void close() throws IOException
    {
        if (controlRO != null)
        {
            controlRO.close();
        }
    }

    private void concludeCounters()
    {
        if (countersManager == null)
        {
            countersManager(new CountersManager(controlRO.counterLabelsBuffer(), controlRO.counterValuesBuffer()));
        }

        if (counters == null)
        {
            counters = new Counters(countersManager);
        }
    }
}
