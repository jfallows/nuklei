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
package org.kaazing.nuklei.bench.jmh.tcp;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.STREAMS_CAPACITY_PROPERTY_NAME;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.NukleusFactory;
import org.kaazing.nuklei.tcp.internal.TcpController;
import org.kaazing.nuklei.tcp.internal.TcpNukleus;
import org.kaazing.nuklei.tcp.internal.TcpStreams;
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
import org.openjdk.jmh.infra.Control;

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

    private byte[] sendByteArray;

    @Setup
    public void create() throws Exception
    {
        final Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/nukleus-benchmarks");
        properties.setProperty(STREAMS_CAPACITY_PROPERTY_NAME, Long.toString(1024 * 1024 * 16));
        final Configuration config = new Configuration(properties);

        NukleusFactory factory = NukleusFactory.instantiate();

        this.nukleus = (TcpNukleus) factory.create("tcp", config);
        this.controller = (TcpController) factory.create("tcp.controller", config);

        controller.capture("handler");
        while (this.nukleus.process() != 0L)
        {
            // intentional
        }

        int streamCapacity = 1024 * 1024;
        File source = new File("target/nukleus-benchmarks/handler/streams/tcp");
        createEmptyFile(source.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        controller.route("handler");
        while (this.nukleus.process() != 0L)
        {
            // intentional
        }

        controller.bind("handler", new InetSocketAddress("localhost", 8080));
        while (this.nukleus.process() != 0L)
        {
            // intentional
        }

        this.streams = controller.streams("handler");

        this.sendByteArray = "Hello, world".getBytes(StandardCharsets.UTF_8);
    }

    @TearDown
    public void close() throws Exception
    {
        this.nukleus.close();
    }

    @Benchmark
    @Group("process")
    @GroupThreads(1)
    public void socket(Control control) throws Exception
    {
        if (control.startMeasurement && !control.stopMeasurement)
        {
            try (Socket socket = new Socket("localhost", 8080))
            {
                OutputStream output = socket.getOutputStream();
                while (control.startMeasurement && !control.stopMeasurement)
                {
                    output.write(sendByteArray);
                }
            }
        }
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
    public void handler(Control control) throws Exception
    {
        while (control.startMeasurement && !control.stopMeasurement)
        {
            while (this.streams.read((msgTypeId, buffer, offset, length) -> {}) != 0)
            {
                // intentional
            }
        }
    }
}
