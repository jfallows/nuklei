/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
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
package org.kaazing.nuklei.function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_LONG;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.kaazing.nuklei.function.AlignedMikro.AlignmentSupplier;
import org.kaazing.nuklei.function.AlignedMikro.Storage;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

@SuppressWarnings("unchecked")
public class AlignedMikroTest
{

    private final AlignmentSupplier<MutableDirectBuffer> alignment = (state, header, typeId, buffer, offset, length) -> {
        return buffer.getInt(offset);
    };

    @Test
    public void shouldNotReassembleFrames()
    {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);

        final AlignedMikro<MutableDirectBuffer> handler = mock(AlignedMikro.class);
        AlignedMikro<MutableDirectBuffer> wrapHandler = new AlignedMikro<MutableDirectBuffer>()
        {
            @Override
            public void onMessage(
                    MutableDirectBuffer state, Object header, int typeId, DirectBuffer buffer, int offset, int length)
            {
                handler.onMessage(state, null, typeId, buffer, offset, length);
            }
        };
        StatefulMikro<MutableDirectBuffer> mikro = wrapHandler.alignedBy((t) -> new Storage(t), alignment);
        mikro.onMessage(storageBuffer, null, 0, readBuffer, SIZE_OF_LONG, 32);

        verify(handler).onMessage(storageBuffer, null, 0, readBuffer, SIZE_OF_LONG, 32);
    }

    @Test
    public void shouldNotDeliverPartialFrames()
    {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(24));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(24));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);

        final AlignedMikro<MutableDirectBuffer> handler = mock(AlignedMikro.class);
        AlignedMikro<MutableDirectBuffer> wrapHandler = new AlignedMikro<MutableDirectBuffer>()
        {
            @Override
            public void onMessage(
                    MutableDirectBuffer state, Object header, int typeId, DirectBuffer buffer, int offset, int length)
            {
                handler.onMessage(state, null, typeId, buffer, offset, length);
            }
        };
        StatefulMikro<MutableDirectBuffer> mikro = wrapHandler.alignedBy((t) -> new Storage(t), alignment);
        mikro.onMessage(storageBuffer, null, 0, readBuffer, SIZE_OF_LONG, 16);

        verify(handler, never()).onMessage(any(), any(), anyInt(), any(), anyInt(), anyInt());
    }

    @Test
    public void shouldDeliverAlignedFrames()
    {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(64));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);

        final AlignedMikro<MutableDirectBuffer> handler = mock(AlignedMikro.class);
        AlignedMikro<MutableDirectBuffer> wrapHandler = new AlignedMikro<MutableDirectBuffer>()
        {
            @Override
            public void onMessage(
                    MutableDirectBuffer state, Object header, int typeId, DirectBuffer buffer, int offset, int length)
            {
                handler.onMessage(state, null, typeId, buffer, offset, length);
            }
        };
        StatefulMikro<MutableDirectBuffer> mikro = wrapHandler.alignedBy((t) -> new Storage(t), alignment);
        mikro.onMessage(storageBuffer, null, 0, readBuffer, SIZE_OF_LONG, 40);

        verify(handler).onMessage(storageBuffer, null, 0, readBuffer, SIZE_OF_LONG, 32);
    }

    @Test
    public void shouldDeliverReassembledFrames()
    {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(48));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        readBuffer.putLong(16, 1L);

        final AlignedMikro<MutableDirectBuffer> handler = mock(AlignedMikro.class);
        final Storage storage = new Storage();
        AlignedMikro<MutableDirectBuffer> wrapHandler = new AlignedMikro<MutableDirectBuffer>()
        {
            @Override
            public void onMessage(
                    MutableDirectBuffer state, Object header, int typeId, DirectBuffer buffer, int offset, int length)
            {
                handler.onMessage(state, null, typeId, buffer, offset, length);
            }
        };
        StatefulMikro<MutableDirectBuffer> mikro = wrapHandler.alignedBy((t) -> storage.buffer(t), alignment);
        mikro.onMessage(storageBuffer, null, 0, readBuffer, SIZE_OF_LONG, 8);
        mikro.onMessage(storageBuffer, null, 0, readBuffer, 24, 24);

        verify(handler).onMessage(storageBuffer, null, 0, storageBuffer, 0, 32);
    }

    @Test
    public void shouldDeliverAlignedAndReassembledFrames()
    {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(80));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        readBuffer.putInt(SIZE_OF_LONG + 32, 32);
        readBuffer.putLong(64, 1L);

        final AlignedMikro<MutableDirectBuffer> handler = mock(AlignedMikro.class);
        final Storage storage = new Storage();
        AlignedMikro<MutableDirectBuffer> wrapHandler = new AlignedMikro<MutableDirectBuffer>()
        {
            @Override
            public void onMessage(
                    MutableDirectBuffer state, Object header, int typeId, DirectBuffer buffer, int offset, int length)
            {
                handler.onMessage(state, null, typeId, buffer, offset, length);
            }
        };
        StatefulMikro<MutableDirectBuffer> mikro = wrapHandler.alignedBy((t) -> storage.buffer(t), alignment);
        mikro.onMessage(storageBuffer, null, 0, readBuffer, SIZE_OF_LONG, 56);
        mikro.onMessage(storageBuffer, null, 0, readBuffer, 72, 8);

        verify(handler).onMessage(storageBuffer, null, 0, readBuffer, SIZE_OF_LONG, 32);
        verify(handler).onMessage(storageBuffer, null, 0, storageBuffer, 0, 32);
    }
}
