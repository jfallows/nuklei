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
package org.kaazing.nuklei.bench.jmh.ws;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.STREAMS_BUFFER_CAPACITY_PROPERTY_NAME;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.NukleusFactory;
import org.kaazing.nuklei.ws.internal.WsController;
import org.kaazing.nuklei.ws.internal.WsNukleus;
import org.kaazing.nuklei.ws.internal.WsStreams;
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
public class WsServerBM
{
    private WsNukleus nukleus;
    private WsController controller;
    private WsStreams requestStreams;
    private WsStreams responseStreams;

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

        this.nukleus = (WsNukleus) factory.create("ws", config);
        this.controller = (WsController) factory.create("ws.controller", config);

        controller.capture("source");
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }

        controller.capture("destination");
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }

        File source = new File("target/nukleus-benchmarks/source/streams/ws").getAbsoluteFile();
        createEmptyFile(source, streamsCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        controller.route("source");
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }

        File destination = new File("target/nukleus-benchmarks/destination/streams/ws").getAbsoluteFile();
        createEmptyFile(destination, streamsCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        controller.route("destination");
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }

        CompletableFuture<Long> sourceRefFuture = controller.bind("destination", 0x1234L, "source", null);
        while (this.nukleus.process() != 0L || this.controller.process() != 0L)
        {
            // intentional
        }
        final long sourceRef = sourceRefFuture.get();

        this.requestStreams = controller.streams("source", "destination");
        this.responseStreams = controller.streams("destination", "source");

        // odd, positive, non-zero
        final Random random = new Random();
        this.streamId = (random.nextLong() & 0x3fffffffffffffffL) | 0x0000000000000001L;

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(":scheme", "http");
        headers.put(":method", "GET");
        headers.put(":path", "/");
        headers.put("host", "localhost:8080");
        headers.put("upgrade", "websocket");
        headers.put("sec-websocket-key", "dGhlIHNhbXBsZSBub25jZQ==");
        headers.put("sec-websocket-version", "13");

        this.requestStreams.httpBegin(streamId, sourceRef, headers);
        while (this.nukleus.process() != 0L)
        {
            // intentional
        }
        while (this.responseStreams.read((msgTypeId, buffer, offset, length) -> {}) != 0)
        {
            // intentional
        }

        byte[] charBytes = "Hello, world".getBytes(StandardCharsets.UTF_8);

        byte[] byteArray = new byte[18];
        byteArray[0] = (byte) 0x82; // fin, binary
        byteArray[1] = (byte) 0x8c; // masked
        byteArray[2] = (byte) 0x01; // masking key (4 bytes)
        byteArray[3] = (byte) 0x02;
        byteArray[4] = (byte) 0x03;
        byteArray[5] = (byte) 0x04;
        byteArray[6] = (byte) (charBytes[0] ^ byteArray[2]);
        byteArray[7] = (byte) (charBytes[1] ^ byteArray[3]);
        byteArray[8] = (byte) (charBytes[2] ^ byteArray[4]);
        byteArray[9] = (byte) (charBytes[3] ^ byteArray[5]);
        byteArray[10] = (byte) (charBytes[4] ^ byteArray[2]);
        byteArray[11] = (byte) (charBytes[5] ^ byteArray[3]);
        byteArray[12] = (byte) (charBytes[6] ^ byteArray[4]);
        byteArray[13] = (byte) (charBytes[7] ^ byteArray[5]);
        byteArray[14] = (byte) (charBytes[8] ^ byteArray[2]);
        byteArray[15] = (byte) (charBytes[9] ^ byteArray[3]);
        byteArray[16] = (byte) (charBytes[10] ^ byteArray[4]);
        byteArray[17] = (byte) (charBytes[11] ^ byteArray[5]);

        this.sendBuffer = new UnsafeBuffer(byteArray);
    }

    @TearDown
    public void destroy() throws Exception
    {
        this.nukleus.close();
        this.controller.close();
        this.requestStreams.close();
        this.responseStreams.close();
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
               !requestStreams.httpData(streamId, sendBuffer, 0, sendBuffer.capacity()))
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
               requestStreams.read((msgTypeId, buffer, offset, length) -> {}) == 0)
        {
            Thread.yield();
        }
    }
}
