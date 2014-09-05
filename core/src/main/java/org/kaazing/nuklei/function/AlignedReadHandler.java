package org.kaazing.nuklei.function;

import java.util.Objects;

import org.kaazing.nuklei.concurrent.AtomicBuffer;

@FunctionalInterface
public interface AlignedReadHandler<T> extends StatefulReadHandler<T> {

    default StatefulReadHandler<T> alignedBy(DataOffsetSupplier data, StorageSupplier<T> storage, AlignmentSupplier<T> alignment) {
        Objects.requireNonNull(storage);
        int replayOffset[] = { 0 };
        int replayLimit[] = { 0 };
        return (state, typeId, buffer, offset, length) -> {
            
            AtomicBuffer replayBuffer = storage.supply(state);
            int dataOffset = data.supplyAsInt(typeId);

            // determine alignment boundary
            if (replayOffset[0] != replayLimit[0]) {
                int newReplayOffset = replayOffset[0] + length - dataOffset;
                if (newReplayOffset < replayLimit[0]) {
                    // retain partial frame for re-assembly
                    replayBuffer.putBytes(replayOffset[0], buffer, offset + dataOffset, length - dataOffset);
                    replayOffset[0] = newReplayOffset;
                    // no remaining data to process
                    return;
                }
                else {
                    // complete the re-assembled frame
                    replayBuffer.putBytes(replayOffset[0], buffer, offset + dataOffset, replayLimit[0] - replayOffset[0]);
                    onMessage(state, typeId, replayBuffer, 0, replayLimit[0]);

                    // update offset and length for remaining processing
                    offset += replayLimit[0] - replayOffset[0] + dataOffset;
                    length -= replayLimit[0] - replayOffset[0] + dataOffset;
                    replayOffset[0] = replayLimit[0] = 0;
                    
                    // no remaining data to process
                    if (length == 0) {
                        return;
                    }
                }
            }

            int alignedLength = alignment.supply(state, typeId, buffer, offset, length);
            if (alignedLength == length) {
                // propagate aligned frame(s)
                onMessage(state, typeId, buffer, offset, length);
            }
            else if (alignedLength < length) {
                // propagate aligned frame(s)
                onMessage(state, typeId, buffer, offset, alignedLength);

                // retain partial frame for re-assembly
                replayBuffer.putBytes(replayOffset[0], buffer, offset, dataOffset);
                offset += alignedLength;
                length -= alignedLength;
                replayBuffer.putBytes(replayOffset[0] + dataOffset, buffer, offset, length);

                replayOffset[0] += length + dataOffset;
                replayLimit[0] = alignment.supply(state, typeId, replayBuffer, 0, replayOffset[0]);
            }
            else {
                // retain knowledge of remaining length required for alignment
                replayLimit[0] = alignedLength;

                // retain partial frame for re-assembly
                replayBuffer.putBytes(replayOffset[0], buffer, offset, length);
                replayOffset[0] += length;
            }
        };
    }

    @FunctionalInterface
    public interface StorageSupplier<T> {
        AtomicBuffer supply(T state);
    }

    @FunctionalInterface
    public interface DataOffsetSupplier {
        public int supplyAsInt(int typeId);
    }

    @FunctionalInterface
    public interface AlignmentSupplier<T> {
        public int supply(T state, int typeId, AtomicBuffer buffer, int offset, int length);
    }
}
