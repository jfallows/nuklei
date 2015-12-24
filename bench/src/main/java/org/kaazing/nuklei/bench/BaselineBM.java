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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    @Setup
    public void create() throws Exception
    {
        int capacity = 1024 * 1024 * 16 + RingBufferDescriptor.TRAILER_LENGTH;

        UnsafeBuffer source = new UnsafeBuffer(new byte[capacity]);
        this.sourceCapture = new ManyToOneRingBuffer(source);
        this.sourceRoute = new ManyToOneRingBuffer(source);

        UnsafeBuffer destination = new UnsafeBuffer(new byte[capacity]);
        this.destinationCapture = new ManyToOneRingBuffer(destination);
        this.destinationRoute = new ManyToOneRingBuffer(destination);

        byte[] byteArray = "Hello, world".getBytes(StandardCharsets.UTF_8);
        UnsafeBuffer writeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(byteArray.length).order(ByteOrder.nativeOrder()));
        writeBuffer.putBytes(0, byteArray);
        this.writeBuffer = writeBuffer;

        this.copyBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(byteArray.length).order(ByteOrder.nativeOrder()));
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
        while (!control.stopMeasurement && !sourceRoute.write(0x02, writeBuffer, 0, writeBuffer.capacity()))
        {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void copier(Counters counters) throws Exception
    {
        counters.messages += sourceCapture.read((msgTypeId, buffer, offset, length) ->
        {
            MutableDirectBuffer copyBuffer = this.copyBuffer;
            copyBuffer.putBytes(0, buffer, offset, length);
            while (!destinationRoute.write(0x01, copyBuffer, 0, copyBuffer.capacity()))
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
        while (!control.stopMeasurement && destinationCapture.read((msgTypeId, buffer, offset, length) -> {}) == 0)
        {
            Thread.yield();
        }
    }
}
