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
package org.kaazing.nuklei.bench;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static uk.co.real_logic.agrona.IoUtil.mapExistingFile;
import static uk.co.real_logic.agrona.IoUtil.mapNewFile;
import static uk.co.real_logic.agrona.IoUtil.unmap;

import java.io.File;
import java.nio.charset.StandardCharsets;

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
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(3)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@OutputTimeUnit(SECONDS)
public class BaselineBM
{
    private RingBuffer sourceCapture;
    private RingBuffer sourceRoute;
    private RingBuffer destinationCapture;
    private RingBuffer destinationRoute;

    private DirectBuffer writeBuffer;
    private MutableDirectBuffer copyBuffer;

    @Setup(Level.Trial)
    public void init()
    {
        int capacity = 1024 * 1024 * 16 + RingBufferDescriptor.TRAILER_LENGTH;

        File sourceToNukleus = new File("target/nukleus-benchmarks/nukleus/streams/source").getAbsoluteFile();
        File nukleusToDestination = new File("target/nukleus-benchmarks/destination/streams/nukleus").getAbsoluteFile();

        this.sourceCapture = new ManyToOneRingBuffer(new UnsafeBuffer(mapNewFile(sourceToNukleus, capacity)));
        this.sourceRoute = new ManyToOneRingBuffer(new UnsafeBuffer(mapExistingFile(sourceToNukleus, "source")));

        this.destinationCapture = new ManyToOneRingBuffer(new UnsafeBuffer(mapNewFile(nukleusToDestination, capacity)));
        this.destinationRoute = new ManyToOneRingBuffer(new UnsafeBuffer(mapExistingFile(nukleusToDestination, "destination")));

        byte[] byteArray = "Hello, world".getBytes(StandardCharsets.UTF_8);
        UnsafeBuffer writeBuffer = new UnsafeBuffer(allocateDirect(byteArray.length).order(nativeOrder()));
        writeBuffer.putBytes(0, byteArray);
        this.writeBuffer = writeBuffer;

        this.copyBuffer = new UnsafeBuffer(allocateDirect(sourceCapture.maxMsgLength()).order(nativeOrder()));
    }

    @TearDown(Level.Trial)
    public void destroy()
    {
        unmap(sourceCapture.buffer().byteBuffer());
        unmap(sourceRoute.buffer().byteBuffer());
        unmap(destinationCapture.buffer().byteBuffer());
        unmap(destinationRoute.buffer().byteBuffer());
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
               !sourceRoute.write(0x02, writeBuffer, 0, writeBuffer.capacity()))
        {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void copier(Control control, Counters counters) throws Exception
    {
        counters.messages += sourceCapture.read((msgTypeId, buffer, offset, length) ->
        {
            MutableDirectBuffer copyBuffer = this.copyBuffer;
            copyBuffer.putBytes(0, buffer, offset, length);
            while (!control.stopMeasurement &&
                   !destinationRoute.write(msgTypeId, copyBuffer, 0, length))
            {
                Thread.yield();
            }
        });
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void reader(Control control) throws Exception
    {
        while (!control.stopMeasurement &&
               destinationCapture.read((msgTypeId, buffer, offset, length) -> {}) == 0)
        {
            Thread.yield();
        }
    }
}
