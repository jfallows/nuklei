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
package org.kaazing.nuklei.bench.jmh.echo;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.STREAMS_BUFFER_CAPACITY_PROPERTY_NAME;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.NukleusFactory;
import org.kaazing.nuklei.echo.internal.EchoController;
import org.kaazing.nuklei.echo.internal.EchoNukleus;
import org.kaazing.nuklei.echo.internal.EchoStreams;
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

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(3)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@OutputTimeUnit(SECONDS)
public class EchoServerBM
{
    private EchoNukleus nukleus;
    private EchoController controller;
    private EchoStreams streams;

    private MutableDirectBuffer sendBuffer;
    private long streamId;

    @Setup
    public void init() throws Exception
    {
        int streamsCapacity = 1024 * 1024 * 16;

        final Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/nukleus-benchmarks");
        properties.setProperty(STREAMS_BUFFER_CAPACITY_PROPERTY_NAME, Long.toString(streamsCapacity));
        final Configuration config = new Configuration(properties);

        NukleusFactory factory = NukleusFactory.instantiate();

        this.nukleus = (EchoNukleus) factory.create("echo", config);
        this.controller = (EchoController) factory.create("echo.controller", config);

        controller.capture("source");
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }

        File source = new File("target/nukleus-benchmarks/source/streams/echo");
        createEmptyFile(source.getAbsoluteFile(), streamsCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        controller.route("source");
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }

        CompletableFuture<Long> sourceRefFuture = controller.bind("source");
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }
        final long sourceRef = sourceRefFuture.get();

        this.streams = controller.streams("source");

        // odd, positive, non-zero
        final Random random = new Random();
        this.streamId = (random.nextLong() & 0x3fffffffffffffffL) | 0x0000000000000001L;

        this.streams.begin(streamId, sourceRef);
        while (this.nukleus.process() != 0L)
        {
            // intentional
        }
        while (this.streams.read((msgTypeId, buffer, offset, length) -> {}) != 0)
        {
            // intentional
        }

        byte[] byteArray = "Hello, world".getBytes(StandardCharsets.UTF_8);
        this.sendBuffer = new UnsafeBuffer(byteArray);
    }

    @TearDown
    public void destroy() throws Exception
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
        while (!control.stopMeasurement &&
               !streams.data(streamId, sendBuffer, 0, sendBuffer.capacity()))
        {
            Thread.yield();
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
        while (!control.stopMeasurement &&
               streams.read((msgTypeId, buffer, offset, length) -> {}) == 0)
        {
            Thread.yield();
        }
    }
}
