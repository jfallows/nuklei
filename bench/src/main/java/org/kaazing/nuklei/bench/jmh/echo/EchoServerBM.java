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
import static org.kaazing.nuklei.Configuration.STREAMS_CAPACITY_PROPERTY_NAME;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.echo.internal.Context;
import org.kaazing.nuklei.echo.internal.EchoNukleus;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

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
    public void create() throws Exception
    {
        final Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/nukleus-benchmarks");
        properties.setProperty(STREAMS_CAPACITY_PROPERTY_NAME, Long.toString(1024 * 1024 * 16));
        final Configuration config = new Configuration(properties);
        final Context context = new Context();
        context.controlFile(new File(config.directory(), "echo/control"))
               .conclude(config);

        this.nukleus = new EchoNukleus(context);
        this.controller = new EchoController(context);

        long bindCorrelationId = controller.bind("source", 1234L);
        while (this.nukleus.process() != 0L)
        {
            // intentional
        }
        long bindRef = controller.supplyResponse(bindCorrelationId);

        this.streams = new EchoStreams(context, "source.accepts");

        long streamId = (long) Math.random() * Long.MAX_VALUE;
        this.streams.begin(bindRef, streamId);
        this.streams.drain();

        this.sendBuffer = new UnsafeBuffer("Hello, world".getBytes(StandardCharsets.UTF_8));
    }

    @TearDown
    public void close() throws Exception
    {
        this.nukleus.close();
    }

    @Benchmark
    @Group("process")
    @GroupThreads(1)
    public void nukleus() throws Exception
    {
        this.nukleus.process();
    }

    @Benchmark
    @Group("process")
    @GroupThreads(1)
    public void handler() throws Exception
    {
        this.streams.data(streamId, sendBuffer, 0, sendBuffer.capacity());
        this.streams.drain();
    }
}
