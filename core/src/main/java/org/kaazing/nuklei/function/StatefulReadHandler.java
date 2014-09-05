package org.kaazing.nuklei.function;

import java.util.Objects;

import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.RingBufferReader.ReadHandler;

@FunctionalInterface
public interface StatefulReadHandler<T> {

    /**
     * Message read from a ring buffer with connection state.
     *
     * @param state of the message stream
     * @param typeId of the message
     * @param buffer of the message
     * @param offset within the buffer where the message starts
     * @param length of the message in bytes
     */
    void onMessage(final T state, final int typeId, final AtomicBuffer buffer, final int offset, final int length);

    default ReadHandler statefulBy(StateSupplier<T> stateful) {
        Objects.requireNonNull(stateful);
        return (typeId, buffer, offset, length) -> {
            T state = stateful.supply(typeId, buffer, offset, length);
            onMessage(state, typeId, buffer, offset, length);
        };
    }

    @FunctionalInterface
    public interface StateSupplier<T> {
        
        T supply(int typeId, AtomicBuffer buffer, int offset, int length);
    }
}
