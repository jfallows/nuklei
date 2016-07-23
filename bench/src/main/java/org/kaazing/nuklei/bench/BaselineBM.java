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
import static org.agrona.IoUtil.mapNewFile;
import static org.agrona.IoUtil.unmap;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

import java.io.File;
import java.util.Random;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
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

    @Setup(Level.Trial)
    public void init()
    {
        final int capacity = 1024 * 1024 * 16 + TRAILER_LENGTH;
        final int payload = 128;

        final File sourceFile = new File("target/benchmarks/baseline/source").getAbsoluteFile();
        final File targetFile = new File("target/benchmarks/baseline/target").getAbsoluteFile();

        this.source = new OneToOneRingBuffer(new UnsafeBuffer(mapNewFile(sourceFile, capacity)));
        this.target = new OneToOneRingBuffer(new UnsafeBuffer(mapNewFile(targetFile, capacity)));

        this.writeBuffer = new UnsafeBuffer(allocateDirect(payload).order(nativeOrder()));
        this.writeBuffer.setMemory(0, payload, (byte)new Random().nextInt(256));
    }

    @TearDown(Level.Trial)
    public void destroy()
    {
        unmap(source.buffer().byteBuffer());
        unmap(target.buffer().byteBuffer());
    }

    @Setup(Level.Iteration)
    public void reset()
    {
        source.buffer().setMemory(source.buffer().capacity() - TRAILER_LENGTH, TRAILER_LENGTH, (byte)0);
        source.buffer().putLongOrdered(0, 0L);
        target.buffer().setMemory(target.buffer().capacity() - TRAILER_LENGTH, TRAILER_LENGTH, (byte)0);
        target.buffer().putLongOrdered(0, 0L);
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void writer(Control control) throws Exception
    {
        while (!control.stopMeasurement &&
               !source.write(0x02, writeBuffer, 0, writeBuffer.capacity()))
        {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void copier(Control control) throws Exception
    {
        if (!control.stopMeasurement)
        {
            source.read((msgTypeId, buffer, offset, length) ->
            {
                while (!control.stopMeasurement &&
                        !target.write(msgTypeId, buffer, offset, length))
                {
                    Thread.yield();
                }
            });
        }
    }

    @Benchmark
    @Group("asymmetric")
    @GroupThreads(1)
    public void reader(Control control) throws Exception
    {
        while (!control.stopMeasurement &&
               target.read((msgTypeId, buffer, offset, length) -> {}) == 0)
        {
            Thread.yield();
        }
    }
}
