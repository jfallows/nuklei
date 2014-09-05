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

import static org.kaazing.nuklei.BitUtil.SIZE_OF_LONG;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.function.AlignedReadHandler;
import org.kaazing.nuklei.function.StatefulReadHandler;
import org.kaazing.nuklei.function.AlignedReadHandler.AlignmentSupplier;

@SuppressWarnings("unchecked")
public class AlignedReadHandlerTest {

    private final AlignmentSupplier<AtomicBuffer> alignment = (state, typeId, buffer, offset, length) -> {
        return SIZE_OF_LONG + buffer.getInt(offset + SIZE_OF_LONG);
    };
    
    @Test
    public void shouldNotReassembleFrames() {
        AtomicBuffer storageBuffer = new AtomicBuffer(ByteBuffer.allocate(40));
        AtomicBuffer readBuffer = new AtomicBuffer(ByteBuffer.allocate(40));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);

        final AlignedReadHandler<AtomicBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<AtomicBuffer> wrapHandler = new AlignedReadHandler<AtomicBuffer>() {
            @Override
            public void onMessage(AtomicBuffer state, int typeId, AtomicBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<AtomicBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 40);

        verify(handler).onMessage(storageBuffer, 0, readBuffer, 0, 40);
    }

    @Test
    public void shouldNotDeliverPartialFrames() {
        AtomicBuffer storageBuffer = new AtomicBuffer(ByteBuffer.allocate(24));
        AtomicBuffer readBuffer = new AtomicBuffer(ByteBuffer.allocate(24));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        
        final AlignedReadHandler<AtomicBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<AtomicBuffer> wrapHandler = new AlignedReadHandler<AtomicBuffer>() {
            @Override
            public void onMessage(AtomicBuffer state, int typeId, AtomicBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<AtomicBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 24);

        verify(handler, never()).onMessage(any(), anyInt(), any(), anyInt(), anyInt());
    }

    @Test
    public void shouldDeliverAlignedFrames() {
        AtomicBuffer storageBuffer = new AtomicBuffer(ByteBuffer.allocate(40));
        AtomicBuffer readBuffer = new AtomicBuffer(ByteBuffer.allocate(64));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        
        final AlignedReadHandler<AtomicBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<AtomicBuffer> wrapHandler = new AlignedReadHandler<AtomicBuffer>() {
            @Override
            public void onMessage(AtomicBuffer state, int typeId, AtomicBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<AtomicBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 48);

        verify(handler).onMessage(storageBuffer, 0, readBuffer, 0, 40);
    }

    @Test
    public void shouldDeliverReassembledFrames() {
        AtomicBuffer storageBuffer = new AtomicBuffer(ByteBuffer.allocate(40));
        AtomicBuffer readBuffer = new AtomicBuffer(ByteBuffer.allocate(48));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        readBuffer.putLong(16, 1L);
        
        final AlignedReadHandler<AtomicBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<AtomicBuffer> wrapHandler = new AlignedReadHandler<AtomicBuffer>() {
            @Override
            public void onMessage(AtomicBuffer state, int typeId, AtomicBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<AtomicBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 16);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 16, 32);

        verify(handler).onMessage(storageBuffer, 0, storageBuffer, 0, 40);
    }

    @Test
    public void shouldDeliverAlignedAndReassembledFrames() {
        AtomicBuffer storageBuffer = new AtomicBuffer(ByteBuffer.allocate(40));
        AtomicBuffer readBuffer = new AtomicBuffer(ByteBuffer.allocate(80));
        readBuffer.putLong(0, 1L);
        readBuffer.putInt(SIZE_OF_LONG, 32);
        readBuffer.putInt(SIZE_OF_LONG + 32, 32);
        readBuffer.putLong(64, 1L);

        final AlignedReadHandler<AtomicBuffer> handler = mock(AlignedReadHandler.class);
        AlignedReadHandler<AtomicBuffer> wrapHandler = new AlignedReadHandler<AtomicBuffer>() {
            @Override
            public void onMessage(AtomicBuffer state, int typeId, AtomicBuffer buffer, int offset, int length) {
                handler.onMessage(state, typeId, buffer, offset, length);
            }
        };
        StatefulReadHandler<AtomicBuffer> readHandler = wrapHandler.alignedBy((typeId) -> SIZE_OF_LONG, (t) -> t, alignment);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 0, 64);
        readHandler.onMessage(storageBuffer, 0, readBuffer, 64, 16);
        
        verify(handler).onMessage(storageBuffer, 0, readBuffer, 0, 40);
        verify(handler).onMessage(storageBuffer, 0, storageBuffer, 0, 40);
    }
}
