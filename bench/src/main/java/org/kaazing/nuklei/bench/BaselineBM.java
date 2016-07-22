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
package org.kaazing.nuklei.bench;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.agrona.IoUtil.createEmptyFile;
import static org.agrona.IoUtil.mapExistingFile;
import static org.agrona.IoUtil.unmap;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

import java.io.File;
import java.util.Random;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
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


@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(3)
@Warmup(iterations = 10, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@OutputTimeUnit(SECONDS)
public class BaselineBM
{
    private OneToOneRingBuffer source;
    private OneToOneRingBuffer target;

    private MutableDirectBuffer writeBuffer;
    private MutableDirectBuffer copyBuffer;

    @Setup(Level.Trial)
    public void init()
    {
        final int capacity = 1024 * 1024 * 16 + TRAILER_LENGTH;
        final int payload = 256;

        final File sourceFile = new File("target/benchmarks/baseline/source").getAbsoluteFile();
        final File targetFile = new File("target/benchmarks/baseline/target").getAbsoluteFile();

        createEmptyFile(sourceFile, capacity);
        createEmptyFile(targetFile, capacity);

        this.source = new OneToOneRingBuffer(new UnsafeBuffer(mapExistingFile(sourceFile, "source")));
        this.target = new OneToOneRingBuffer(new UnsafeBuffer(mapExistingFile(targetFile, "target")));

        this.writeBuffer = new UnsafeBuffer(allocateDirect(payload).order(nativeOrder()));
        this.writeBuffer.setMemory(0, payload, (byte)new Random().nextInt(256));

        this.copyBuffer = new UnsafeBuffer(allocateDirect(source.maxMsgLength()).order(nativeOrder()));
    }

    @TearDown(Level.Trial)
    public void destroy()
    {
        unmap(source.buffer().byteBuffer());
        unmap(target.buffer().byteBuffer());
    }

    @TearDown(Level.Iteration)
    public void reset()
    {
        source.buffer().setMemory(source.buffer().capacity() - TRAILER_LENGTH, TRAILER_LENGTH, (byte)0);
        source.buffer().putLongOrdered(0, 0L);

        target.buffer().setMemory(target.buffer().capacity() - TRAILER_LENGTH, TRAILER_LENGTH, (byte)0);
        target.buffer().putLongOrdered(0, 0L);
    }

    @AuxCounters
    @State(Scope.Thread)
    public static class Counters
    {
        public long writerAt;
        public long readerAt;

        @Setup(Level.Iteration)
        public void init()
        {
            writerAt = 0;
            readerAt = 0;
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void writer(Control control, Counters counters) throws Exception
    {
        while (!control.stopMeasurement &&
               !source.write(0x02, writeBuffer, 0, writeBuffer.capacity()))
        {
            Thread.yield();
        }

        counters.writerAt = source.producerPosition();
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void copier(Control control, Counters counters) throws Exception
    {
        if (!control.stopMeasurement)
        {
            source.read((msgTypeId, buffer, offset, length) ->
            {
                final MutableDirectBuffer copyBuffer = this.copyBuffer;
                copyBuffer.putBytes(0, buffer, offset, length);
                while (!control.stopMeasurement &&
                        !target.write(msgTypeId, copyBuffer, 0, length))
                {
                    Thread.yield();
                }
            });
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void reader(Control control, Counters counters) throws Exception
    {
        while (!control.stopMeasurement &&
               target.read((msgTypeId, buffer, offset, length) -> {}) == 0)
        {
            Thread.yield();
        }

        counters.readerAt = target.consumerPosition();
    }
}
