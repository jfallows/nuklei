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

import org.junit.Test;
import org.kaazing.nuklei.function.AlignedReadHandler.AlignmentSupplier;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_LONG;

@SuppressWarnings("unchecked")
public class AlignedReadHandlerTest {

    private final AlignmentSupplier<MutableDirectBuffer> alignment = (state, typeId, buffer, offset, length) -> {
        return SIZE_OF_LONG + buffer.getInt(offset + SIZE_OF_LONG);
    };
    
    @Test
    public void shouldNotReassembleFrames() {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);

        final AlignedReadHandler<MutableDirectBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<MutableDirectBuffer> wrapHandler = new AlignedReadHandler<MutableDirectBuffer>() {
            @Override
            public void onMessage(MutableDirectBuffer state, int typeId, MutableDirectBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<MutableDirectBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 40);

        verify(handler).onMessage(storageBuffer, 0, readBuffer, 0, 40);
    }

    @Test
    public void shouldNotDeliverPartialFrames() {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(24));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(24));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        
        final AlignedReadHandler<MutableDirectBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<MutableDirectBuffer> wrapHandler = new AlignedReadHandler<MutableDirectBuffer>() {
            @Override
            public void onMessage(MutableDirectBuffer state, int typeId, MutableDirectBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<MutableDirectBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 24);

        verify(handler, never()).onMessage(any(), anyInt(), any(), anyInt(), anyInt());
    }

    @Test
    public void shouldDeliverAlignedFrames() {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(64));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        
        final AlignedReadHandler<MutableDirectBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<MutableDirectBuffer> wrapHandler = new AlignedReadHandler<MutableDirectBuffer>() {
            @Override
            public void onMessage(MutableDirectBuffer state, int typeId, MutableDirectBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<MutableDirectBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 48);

        verify(handler).onMessage(storageBuffer, 0, readBuffer, 0, 40);
    }

    @Test
    public void shouldDeliverReassembledFrames() {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(48));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        readBuffer.putLong(16, 1L);
        
        final AlignedReadHandler<MutableDirectBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<MutableDirectBuffer> wrapHandler = new AlignedReadHandler<MutableDirectBuffer>() {
            @Override
            public void onMessage(MutableDirectBuffer state, int typeId, MutableDirectBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<MutableDirectBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 16);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 16, 32);

        verify(handler).onMessage(storageBuffer, 0, storageBuffer, 0, 40);
    }

    @Test
    public void shouldDeliverAlignedAndReassembledFrames() {
        MutableDirectBuffer storageBuffer = new UnsafeBuffer(ByteBuffer.allocate(40));
        MutableDirectBuffer readBuffer = new UnsafeBuffer(ByteBuffer.allocate(80));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        readBuffer.putInt(SIZE_OF_LONG + 32, 32);
        readBuffer.putLong(64, 1L);

        final AlignedReadHandler<MutableDirectBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<MutableDirectBuffer> wrapHandler = new AlignedReadHandler<MutableDirectBuffer>() {
            @Override
            public void onMessage(MutableDirectBuffer state, int typeId, MutableDirectBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<MutableDirectBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 64);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 64, 16);
        
        verify(handler).onMessage(storageBuffer, 0, readBuffer, 0, 40);
        verify(handler).onMessage(storageBuffer, 0, storageBuffer, 0, 40);
    }
}
