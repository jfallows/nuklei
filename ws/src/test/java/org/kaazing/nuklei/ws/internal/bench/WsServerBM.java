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
package org.kaazing.nuklei.ws.internal.bench;

import static java.nio.ByteBuffer.allocateDirect;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.kaazing.nuklei.Configuration.DIRECTORY_PROPERTY_NAME;
import static org.kaazing.nuklei.Configuration.STREAMS_BUFFER_CAPACITY_PROPERTY_NAME;
import static org.kaazing.nuklei.ws.internal.router.RouteKind.SERVER_INITIAL;
import static org.kaazing.nuklei.ws.internal.router.RouteKind.SERVER_REPLY;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.reaktor.internal.Reaktor;
import org.kaazing.nuklei.ws.internal.WsController;
import org.kaazing.nuklei.ws.internal.WsStreams;
import org.kaazing.nuklei.ws.internal.types.Flyweight;
import org.kaazing.nuklei.ws.internal.types.HttpHeaderFW;
import org.kaazing.nuklei.ws.internal.types.ListFW;
import org.kaazing.nuklei.ws.internal.types.OctetsFW;
import org.kaazing.nuklei.ws.internal.types.stream.BeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.DataFW;
import org.kaazing.nuklei.ws.internal.types.stream.HttpBeginExFW;
import org.kaazing.nuklei.ws.internal.types.stream.WindowFW;
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
public class WsServerBM
{
    private final Configuration configuration;
    private final Reaktor reaktor;

    {
        Properties properties = new Properties();
        properties.setProperty(DIRECTORY_PROPERTY_NAME, "target/nukleus-benchmarks");
        properties.setProperty(STREAMS_BUFFER_CAPACITY_PROPERTY_NAME, Long.toString(1024L * 1024L * 16L));

        configuration = new Configuration(properties);
        reaktor = Reaktor.launch(configuration, n -> "ws".equals(n), WsController.class::isAssignableFrom);
    }

    private final BeginFW beginRO = new BeginFW();
    private final DataFW dataRO = new DataFW();

    private final BeginFW.Builder beginRW = new BeginFW.Builder();
    private final DataFW.Builder dataRW = new DataFW.Builder();
    private final WindowFW.Builder windowRW = new WindowFW.Builder();

    private final HttpBeginExFW.Builder httpBeginExRW = new HttpBeginExFW.Builder();

    private WsStreams initialStreams;
    private WsStreams replyStreams;

    private MutableDirectBuffer throttleBuffer;

    private long initialRef;
    private long replyRef;

    private long targetRef;
    private long sourceId;
    private DataFW data;

    private MessageHandler replyHandler;

    @Setup(Level.Trial)
    public void reinit() throws Exception
    {
        final Random random = new Random();
        final WsController controller = reaktor.controller(WsController.class);

        this.initialRef = controller.bind(SERVER_INITIAL.kind()).get();
        this.replyRef = controller.bind(SERVER_REPLY.kind()).get();
        this.targetRef = random.nextLong();
        this.replyHandler = this::processBegin;

        controller.route("source", initialRef, "ws", replyRef, null).get();
        controller.route("ws", replyRef, "target", targetRef, null).get();

        this.initialStreams = controller.streams("source");
        this.replyStreams = controller.streams("ws", "target");

        this.sourceId = random.nextLong();

        final Consumer<ListFW.Builder<HttpHeaderFW.Builder, HttpHeaderFW>> headers = hs ->
        {
            hs.item(h -> h.name(":scheme").value("http"));
            hs.item(h -> h.name(":method").value("GET"));
            hs.item(h -> h.name(":path").value("/"));
            hs.item(h -> h.name("host").value("localhost:8080"));
            hs.item(h -> h.name("upgrade").value("websocket"));
            hs.item(h -> h.name("sec-websocket-key").value("dGhlIHNhbXBsZSBub25jZQ=="));
            hs.item(h -> h.name("sec-websocket-version").value("13"));

//            hs.item(h -> h.name("sec-websocket-protocol").value(protocol));
        };

        final AtomicBuffer writeBuffer = new UnsafeBuffer(new byte[256]);

        BeginFW begin = beginRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .streamId(sourceId)
                .referenceId(initialRef)
                .correlationId(random.nextLong())
                .extension(e -> e.set(visitHttpBeginEx(headers)))
                .build();

        this.initialStreams.writeStreams(begin.typeId(), begin.buffer(), begin.offset(), begin.length());

        byte[] charBytes = "Hello, world".getBytes(StandardCharsets.UTF_8);

        byte[] sendArray = new byte[18];
        sendArray[0] = (byte) 0x82; // fin, binary
        sendArray[1] = (byte) 0x8c; // masked, length 12
        sendArray[2] = (byte) 0x01; // masking key (4 bytes)
        sendArray[3] = (byte) 0x02;
        sendArray[4] = (byte) 0x03;
        sendArray[5] = (byte) 0x04;
        sendArray[6] = (byte) (charBytes[0] ^ sendArray[2]);
        sendArray[7] = (byte) (charBytes[1] ^ sendArray[3]);
        sendArray[8] = (byte) (charBytes[2] ^ sendArray[4]);
        sendArray[9] = (byte) (charBytes[3] ^ sendArray[5]);
        sendArray[10] = (byte) (charBytes[4] ^ sendArray[2]);
        sendArray[11] = (byte) (charBytes[5] ^ sendArray[3]);
        sendArray[12] = (byte) (charBytes[6] ^ sendArray[4]);
        sendArray[13] = (byte) (charBytes[7] ^ sendArray[5]);
        sendArray[14] = (byte) (charBytes[8] ^ sendArray[2]);
        sendArray[15] = (byte) (charBytes[9] ^ sendArray[3]);
        sendArray[16] = (byte) (charBytes[10] ^ sendArray[4]);
        sendArray[17] = (byte) (charBytes[11] ^ sendArray[5]);

        this.data = dataRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                          .streamId(sourceId)
                          .payload(p -> p.set(sendArray))
                          .extension(e -> e.reset())
                          .build();

        this.throttleBuffer = new UnsafeBuffer(allocateDirect(SIZE_OF_LONG + SIZE_OF_INT));
    }

    @TearDown(Level.Trial)
    public void reset() throws Exception
    {
        WsController controller = reaktor.controller(WsController.class);

        controller.unroute("source", initialRef, "ws", replyRef, null).get();
        controller.unroute("ws", replyRef, "target", targetRef, null).get();

        controller.unbind(initialRef).get();
        controller.unbind(replyRef).get();

        this.initialStreams.close();
        this.initialStreams = null;

        this.replyStreams.close();
        this.replyStreams = null;
    }

    @Benchmark
    @Group("throughput")
    @GroupThreads(1)
    public void writer(Control control) throws Exception
    {
        while (!control.stopMeasurement &&
               !initialStreams.writeStreams(data.typeId(), data.buffer(), 0, data.limit()))
        {
            Thread.yield();
        }

        while (!control.stopMeasurement &&
                initialStreams.readThrottle((t, b, o, l) -> {}) == 0)
        {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("throughput")
    @GroupThreads(1)
    public void reader(Control control) throws Exception
    {
        while (!control.stopMeasurement &&
               replyStreams.readStreams(this::handleReply) == 0)
        {
            Thread.yield();
        }
    }

    private void handleReply(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        replyHandler.onMessage(msgTypeId, buffer, index, length);
    }

    private void processBegin(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        beginRO.wrap(buffer, index, index + length);
        final long streamId = beginRO.streamId();
        doWindow(streamId, 8192);

        this.replyHandler = this::processData;
    }

    private void processData(
        int msgTypeId,
        MutableDirectBuffer buffer,
        int index,
        int length)
    {
        dataRO.wrap(buffer, index, index + length);
        final long streamId = dataRO.streamId();
        final OctetsFW payload = dataRO.payload();

        final int update = payload.length();
        doWindow(streamId, update);
    }

    private void doWindow(
        final long streamId,
        final int update)
    {
        final WindowFW window = windowRW.wrap(throttleBuffer, 0, throttleBuffer.capacity())
                .streamId(streamId)
                .update(update)
                .build();

        replyStreams.writeThrottle(window.typeId(), window.buffer(), window.offset(), window.length());
    }

    private Flyweight.Builder.Visitor visitHttpBeginEx(
        Consumer<ListFW.Builder<HttpHeaderFW.Builder, HttpHeaderFW>> headers)
    {
        return (buffer, offset, limit) ->
            httpBeginExRW.wrap(buffer, offset, limit)
                         .headers(headers)
                         .build()
                         .length();
    }

    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder()
                .include(WsServerBM.class.getSimpleName())
                .forks(0)
                .build();

        new Runner(opt).run();
    }
}
