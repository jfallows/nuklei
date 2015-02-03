package org.kaazing.nuklei.function;

import java.util.Objects;

import uk.co.real_logic.agrona.MutableDirectBuffer;

@FunctionalInterface
public interface StatefulMikro<T>
{
    void onMessage(
        final T state, final Object header, final int typeId,
        final MutableDirectBuffer buffer, final int offset, final int length);

    default Mikro statefulBy(StateSupplier<T> stateful)
    {
        Objects.requireNonNull(stateful);
        return (header, typeId, buffer, offset, length) ->
        {
            T state = stateful.supply(header, typeId);
            onMessage(state, header, typeId, buffer, offset, length);
        };
    }

    @FunctionalInterface
    public interface StateSupplier<T>
    {
        T supply(Object header, int typeId);
    }
}
