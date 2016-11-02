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
package org.kaazing.nuklei.tcp.internal.bench;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.agrona.IoUtil.createEmptyFile;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.STREAMS_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.tcp.internal.router.RouteKind.SERVER_INITIAL;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.reaktor.internal.Reaktor;
import org.kaazing.nuklei.tcp.internal.TcpController;
import org.kaazing.nuklei.tcp.internal.TcpReadableStreams;
import org.kaazing.nuklei.tcp.internal.types.OctetsFW;
import org.kaazing.nuklei.tcp.internal.types.stream.BeginFW;
import org.kaazing.nuklei.tcp.internal.types.stream.DataFW;
import org.kaazing.nuklei.tcp.internal.types.stream.WindowFW;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(3)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@OutputTimeUnit(SECONDS)
public class TcpServerBM
{
    private static final Configuration CONFIGURATION;
    private static final Reaktor REAKTOR;

    static
    {
        Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/nukleus-benchmarks");
        properties.setProperty(STREAMS_BUFFER_CAPACITY_PROPERTY_NAME, Long.toString(1024L * 1024L * 16L));

        CONFIGURATION = new Configuration(properties);
        REAKTOR = Reaktor.launch(CONFIGURATION, n -> "tcp".equals(n), TcpController.class::isAssignableFrom);
    }

    private final BeginFW beginRO = new BeginFW();
    private final DataFW dataRO = new DataFW();
    private final WindowFW.Builder windowRW = new WindowFW.Builder();

    private TcpReadableStreams streams;
    private ByteBuffer sendByteBuffer;
    private AtomicBuffer throttleBuffer;
    private final Random random = new Random();
    private final long targetRef = random.nextLong();
    private long sourceRef;

    @Setup(Level.Trial)
    public void reinit() throws Exception
    {
        byte[] sendByteArray = "Hello, world".getBytes(StandardCharsets.UTF_8);
        this.sendByteBuffer = allocateDirect(sendByteArray.length).order(nativeOrder()).put(sendByteArray);

        this.throttleBuffer = new UnsafeBuffer(allocateDirect(SIZE_OF_LONG + SIZE_OF_INT));

        File target = new File("target/nukleus-benchmarks/tcp/streams/target");
        createEmptyFile(target.getAbsoluteFile(), CONFIGURATION.streamsBufferCapacity() + RingBufferDescriptor.TRAILER_LENGTH);

        TcpController controller = REAKTOR.controller(TcpController.class);
        this.sourceRef = controller.bind(SERVER_INITIAL.kind()).get();
        controller.route("any", sourceRef, "target", targetRef, new InetSocketAddress("localhost", 8080)).get();

        this.streams = controller.streams("any", "target");
    }

    @TearDown(Level.Trial)
    public void reset() throws Exception
    {
        this.streams.close();
        this.streams = null;

        TcpController controller = REAKTOR.controller(TcpController.class);
        controller.unroute("any", sourceRef, "target", targetRef, new InetSocketAddress("localhost", 8080)).get();
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void writer(
        final Control control) throws Exception
    {
        if (control.startMeasurement && !control.stopMeasurement)
        {
            try (SocketChannel channel = SocketChannel.open())
            {
                channel.connect(new InetSocketAddress("127.0.0.1", 8080));
                channel.configureBlocking(false);
                while (control.startMeasurement && !control.stopMeasurement)
                {
                    sendByteBuffer.rewind();
                    if (channel.write(sendByteBuffer) == 0)
                    {
                        Thread.yield();
                    }
                }
            }
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void reader(
        final Control control) throws Exception
    {
        final MessageHandler handler = this::handleRead;
        while (!control.stopMeasurement &&
               streams.read(handler) == 0)
        {
            Thread.yield();
        }
    }

    private void handleRead(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        if (msgTypeId == BeginFW.TYPE_ID)
        {
            beginRO.wrap(buffer, index, index + length);
            final long streamId = beginRO.streamId();
            doWindow(streamId, 8192);
        }
        else if (msgTypeId == DataFW.TYPE_ID)
        {
            dataRO.wrap(buffer, index, index + length);
            final long streamId = dataRO.streamId();
            final OctetsFW payload = dataRO.payload();

            final int update = payload.length();
            doWindow(streamId, update);
        }
    }

    private void doWindow(
        final long streamId,
        final int update)
    {
        final WindowFW window = windowRW.wrap(throttleBuffer, 0, throttleBuffer.capacity())
                .streamId(streamId)
                .update(update)
                .build();

        streams.write(window.typeId(), window.buffer(), window.offset(), window.length());
    }

    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder()
                .include(TcpServerBM.class.getSimpleName())
                .forks(0)
                .build();

        new Runner(opt).run();
    }
}
