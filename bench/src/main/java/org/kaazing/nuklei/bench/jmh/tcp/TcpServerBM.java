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
package org.kaazing.nuklei.bench.jmh.tcp;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.STREAMS_BUFFER_CAPACITY_PROPERTY_NAME;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.ControllerFactory;
import org.kaazing.nuklei.NukleusFactory;
import org.kaazing.nuklei.tcp.internal.TcpController;
import org.kaazing.nuklei.tcp.internal.TcpNukleus;
import org.kaazing.nuklei.tcp.internal.TcpStreams;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Control;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(3)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@OutputTimeUnit(SECONDS)
public class TcpServerBM
{
    private TcpNukleus nukleus;
    private TcpController controller;
    private TcpStreams streams;

    private ByteBuffer sendByteBuffer;

    @Setup
    public void create() throws Exception
    {
        final long streamsCapacity = 1024L * 1024L * 16L;

        final Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/nukleus-benchmarks");
        properties.setProperty(STREAMS_BUFFER_CAPACITY_PROPERTY_NAME, Long.toString(streamsCapacity));
        final Configuration config = new Configuration(properties);

        NukleusFactory nuklei = NukleusFactory.instantiate();
        ControllerFactory controllers = ControllerFactory.instantiate();

        this.nukleus = (TcpNukleus) nuklei.create("tcp", config);
        this.controller = controllers.create(TcpController.class, config);

        File target = new File("target/nukleus-benchmarks/target/streams/tcp");
        createEmptyFile(target.getAbsoluteFile(), streamsCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        final CompletableFuture<Long> bind = controller.bind();
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }
        long sourceRef = bind.get();

        final long targetRef = (long) (Math.random() * Long.MAX_VALUE);
        final CompletableFuture<Void> route =
                controller.route("any", sourceRef, "target", targetRef, new InetSocketAddress("localhost", 8080));
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }
        route.get();

        this.streams = controller.streams("any", "target");

        byte[] sendByteArray = "Hello, world".getBytes(StandardCharsets.UTF_8);
        this.sendByteBuffer = allocateDirect(sendByteArray.length).order(nativeOrder()).put(sendByteArray);
    }

    @TearDown
    public void close() throws Exception
    {
        this.nukleus.close();
        this.controller.close();
        this.streams.close();
    }

    @AuxCounters
    @State(Scope.Thread)
    public static class Counters
    {
        public int messages;

        @Setup(Level.Iteration)
        public void init()
        {
            messages = 0;
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void writer(Control control) throws Exception
    {
        if (control.startMeasurement && !control.stopMeasurement)
        {
            try (SocketChannel channel = SocketChannel.open())
            {
                channel.connect(new InetSocketAddress("localhost", 8080));
                while (control.startMeasurement && !control.stopMeasurement)
                {
                    sendByteBuffer.rewind();
                    channel.write(sendByteBuffer);
                }
            }
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void nukleus(Counters counters) throws Exception
    {
        counters.messages += this.nukleus.process();
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void reader(Control control) throws Exception
    {
        final MessageHandler handler = this::handleRead;
        while (!control.stopMeasurement &&
               streams.read(handler) == 0)
        {
            Thread.yield();
        }
    }

    private int handleRead(
        int msgTypeId,
        DirectBuffer buffer,
        int index,
        int length)
    {
        return 0;
    }
}
