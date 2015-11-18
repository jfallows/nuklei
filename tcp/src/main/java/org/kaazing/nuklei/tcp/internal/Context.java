/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal;

import static org.kaazing.nuklei.tcp.internal.ControlFileDescriptor.createCommandBuffer;
import static org.kaazing.nuklei.tcp.internal.ControlFileDescriptor.createCounterLabelsBuffer;
import static org.kaazing.nuklei.tcp.internal.ControlFileDescriptor.createCounterValuesBuffer;
import static org.kaazing.nuklei.tcp.internal.ControlFileDescriptor.createMetaDataBuffer;
import static org.kaazing.nuklei.tcp.internal.ControlFileDescriptor.createResponseBuffer;
import static uk.co.real_logic.agrona.IoUtil.mapNewFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;
import static uk.co.real_logic.agrona.LangUtil.rethrowUnchecked;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.logging.Logger;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.tcp.internal.acceptor.AcceptorCommand;
import org.kaazing.nuklei.tcp.internal.conductor.ConductorResponse;
import org.kaazing.nuklei.tcp.internal.connector.ConnectorCommand;
import org.kaazing.nuklei.tcp.internal.reader.ReaderCommand;
import org.kaazing.nuklei.tcp.internal.writer.WriterCommand;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Context implements Closeable
{
    private File controlFile;
    private IdleStrategy idleStrategy;
    private ErrorHandler errorHandler;
    private CountersManager countersManager;
    private AtomicBuffer counterLabelsBuffer;
    private AtomicBuffer counterValuesBuffer;
    private Counters counters;
    private RingBuffer toConductorCommands;
    private MappedByteBuffer controlByteBuffer;
    private UnsafeBuffer controlMetaDataBuffer;
    private BroadcastTransmitter fromConductorResponses;

    private OneToOneConcurrentArrayQueue<AcceptorCommand> toAcceptorFromConductorCommands;
    private OneToOneConcurrentArrayQueue<ConductorResponse> fromAcceptorToConductorResponses;
    private OneToOneConcurrentArrayQueue<ReaderCommand> fromAcceptorToReaderCommands;
    private OneToOneConcurrentArrayQueue<WriterCommand> fromAcceptorToWriterCommands;
    private OneToOneConcurrentArrayQueue<ConnectorCommand> toConnectorFromConductorCommands;
    private OneToOneConcurrentArrayQueue<ConductorResponse> fromConnectorToConductorResponses;

    public Context controlFile(File controlFile)
    {
        this.controlFile = controlFile;
        return this;
    }

    public File controlFile()
    {
        return controlFile;
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

    public Context conductorCommands(RingBuffer conductorCommands)
    {
        this.toConductorCommands = conductorCommands;
        return this;
    }

    public RingBuffer conductorCommands()
    {
        return toConductorCommands;
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

    public void acceptorCommandQueue(
            OneToOneConcurrentArrayQueue<AcceptorCommand> acceptorCommandQueue)
    {
        this.toAcceptorFromConductorCommands = acceptorCommandQueue;
    }

    public OneToOneConcurrentArrayQueue<AcceptorCommand> acceptorCommandQueue()
    {
        return toAcceptorFromConductorCommands;
    }

    public Context acceptorResponseQueue(
            OneToOneConcurrentArrayQueue<ConductorResponse> acceptorResponseQueue)
    {
        this.fromAcceptorToConductorResponses = acceptorResponseQueue;
        return this;
    }

    public OneToOneConcurrentArrayQueue<ConductorResponse> acceptorResponseQueue()
    {
        return fromAcceptorToConductorResponses;
    }

    public void connectorCommandQueue(
            OneToOneConcurrentArrayQueue<ConnectorCommand> connectorCommandQueue)
    {
        this.toConnectorFromConductorCommands = connectorCommandQueue;
    }

    public OneToOneConcurrentArrayQueue<ConnectorCommand> connectorCommandQueue()
    {
        return toConnectorFromConductorCommands;
    }

    public Context connectorResponseQueue(
            OneToOneConcurrentArrayQueue<ConductorResponse> connectorResponseQueue)
    {
        this.fromConnectorToConductorResponses = connectorResponseQueue;
        return this;
    }

    public OneToOneConcurrentArrayQueue<ConductorResponse> connectorResponseQueue()
    {
        return fromConnectorToConductorResponses;
    }

    public Context readerCommandQueue(
            OneToOneConcurrentArrayQueue<ReaderCommand> readerCommandQueue)
    {
        this.fromAcceptorToReaderCommands = readerCommandQueue;
        return this;
    }

    public OneToOneConcurrentArrayQueue<ReaderCommand> readerCommandQueue()
    {
        return fromAcceptorToReaderCommands;
    }

    public Context writerCommandQueue(
            OneToOneConcurrentArrayQueue<WriterCommand> writerCommandQueue)
    {
        this.fromAcceptorToWriterCommands = writerCommandQueue;
        return this;
    }

    public OneToOneConcurrentArrayQueue<WriterCommand> writerCommandQueue()
    {
        return fromAcceptorToWriterCommands;
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
            controlByteBuffer = mapNewFile(
                    controlFile(),
                    ControlFileDescriptor.computeControlFileLength(
                        config.conductorBufferLength() + config.broadcastBufferLength() +
                        config.counterLabelsBufferLength() + config.counterValuesBufferLength()));

            controlMetaDataBuffer = createMetaDataBuffer(controlByteBuffer);
            ControlFileDescriptor.fillMetaData(
                controlMetaDataBuffer,
                config.conductorBufferLength(),
                config.broadcastBufferLength(),
                config.counterLabelsBufferLength(),
                config.counterValuesBufferLength());

            conductorCommands(
                    new ManyToOneRingBuffer(createCommandBuffer(controlByteBuffer, controlMetaDataBuffer)));

            conductorResponses(
                    new BroadcastTransmitter(createResponseBuffer(controlByteBuffer, controlMetaDataBuffer)));

            acceptorCommandQueue(
                    new OneToOneConcurrentArrayQueue<AcceptorCommand>(1024));

            acceptorResponseQueue(
                    new OneToOneConcurrentArrayQueue<ConductorResponse>(1024));

            connectorCommandQueue(
                    new OneToOneConcurrentArrayQueue<ConnectorCommand>(1024));

            connectorResponseQueue(
                    new OneToOneConcurrentArrayQueue<ConductorResponse>(1024));

            readerCommandQueue(
                    new OneToOneConcurrentArrayQueue<ReaderCommand>(1024));

            writerCommandQueue(
                    new OneToOneConcurrentArrayQueue<WriterCommand>(1024));

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
        unmap(controlByteBuffer);
    }

    private void concludeCounters()
    {
        if (countersManager() == null)
        {
            if (counterLabelsBuffer() == null)
            {
                counterLabelsBuffer(createCounterLabelsBuffer(controlByteBuffer, controlMetaDataBuffer));
            }

            if (counterValuesBuffer() == null)
            {
                counterValuesBuffer(createCounterValuesBuffer(controlByteBuffer, controlMetaDataBuffer));
            }

            countersManager(new CountersManager(counterLabelsBuffer(), counterValuesBuffer()));
        }

        if (null == counters)
        {
            counters = new Counters(countersManager);
        }
    }
}
