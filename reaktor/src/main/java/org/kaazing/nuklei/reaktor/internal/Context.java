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
package org.kaazing.nuklei.reaktor.internal;

import static uk.co.real_logic.agrona.IoUtil.mapNewFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;
import static uk.co.real_logic.agrona.LangUtil.rethrowUnchecked;

import java.io.File;
import java.nio.MappedByteBuffer;

import org.kaazing.nuklei.Configuration;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Context implements AutoCloseable
{
    private File cncFile;
    private IdleStrategy idleStrategy;
    private ErrorHandler errorHandler;
    private CountersManager countersManager;
    private AtomicBuffer counterLabelsBuffer;
    private AtomicBuffer counterValuesBuffer;
    private Counters counters;
    private RingBuffer toConductorCommands;
    private MappedByteBuffer cncByteBuffer;
    private UnsafeBuffer cncMetaDataBuffer;

    public Context cncFile(File cncFile)
    {
        this.cncFile = cncFile;
        return this;
    }

    public File cncFile()
    {
        return cncFile;
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
        this.counterLabelsBuffer = counterLabelsBuffer;
        return this;
    }

    public AtomicBuffer counterLabelsBuffer()
    {
        return counterLabelsBuffer;
    }

    public Context counterValuesBuffer(AtomicBuffer counterValuesBuffer)
    {
        this.counterValuesBuffer = counterValuesBuffer;
        return this;
    }

    public AtomicBuffer counterValuesBuffer()
    {
        return counterValuesBuffer;
    }

    public Context toConductorCommands(RingBuffer toConductorCommands)
    {
        this.toConductorCommands = toConductorCommands;
        return this;
    }

    public RingBuffer toConductorCommands()
    {
        return toConductorCommands;
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

    public Context conclude(Configuration config)
    {
        try
        {
            cncByteBuffer = mapNewFile(
                    cncFile(),
                    CncFileDescriptor.computeCncFileLength(
                        config.commandBufferCapacity() + config.responseBufferCapacity() +
                        config.counterLabelsBufferLength() + config.counterValuesBufferLength()));

            cncMetaDataBuffer = CncFileDescriptor.createMetaDataBuffer(cncByteBuffer);
            CncFileDescriptor.fillMetaData(
                cncMetaDataBuffer,
                config.commandBufferCapacity(),
                config.responseBufferCapacity(),
                config.counterLabelsBufferLength(),
                config.counterValuesBufferLength());

            toConductorCommands(
                    new ManyToOneRingBuffer(CncFileDescriptor.createToConductorBuffer(cncByteBuffer, cncMetaDataBuffer)));

            concludeCounters();
        }
        catch (Exception ex)
        {
            rethrowUnchecked(ex);
        }

        return this;
    }

    @Override
    public void close() throws Exception
    {
        unmap(cncByteBuffer);
    }

    private void concludeCounters()
    {
        if (countersManager() == null)
        {
            if (counterLabelsBuffer() == null)
            {
                counterLabelsBuffer(CncFileDescriptor.createCounterLabelsBuffer(cncByteBuffer, cncMetaDataBuffer));
            }

            if (counterValuesBuffer() == null)
            {
                counterValuesBuffer(CncFileDescriptor.createCounterValuesBuffer(cncByteBuffer, cncMetaDataBuffer));
            }

            countersManager(new CountersManager(counterLabelsBuffer(), counterValuesBuffer()));
        }

        if (null == counters)
        {
            counters = new Counters(countersManager);
        }
    }
}
