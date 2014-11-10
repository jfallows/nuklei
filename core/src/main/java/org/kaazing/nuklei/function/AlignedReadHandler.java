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

import java.util.Objects;

import uk.co.real_logic.agrona.MutableDirectBuffer;

@FunctionalInterface
public interface AlignedReadHandler<T> extends StatefulReadHandler<T>
{

    default StatefulReadHandler<T> alignedBy(
        DataOffsetSupplier data, StorageSupplier<T> storage, AlignmentSupplier<T> alignment)
    {
        Objects.requireNonNull(storage);
        int replayOffset[] = { 0 };
        int replayLimit[] = { 0 };
        return (state, typeId, buffer, offset, length) ->
        {

            MutableDirectBuffer replayBuffer = storage.supply(state);
            int dataOffset = data.supplyAsInt(typeId);

            // determine alignment boundary
            if (replayOffset[0] != replayLimit[0])
            {
                int newReplayOffset = replayOffset[0] + length - dataOffset;
                if (newReplayOffset < replayLimit[0])
                {
                    // retain partial frame for re-assembly
                    replayBuffer.putBytes(replayOffset[0], buffer, offset + dataOffset, length - dataOffset);
                    replayOffset[0] = newReplayOffset;
                    // no remaining data to process
                    return;
                }
                else
                {
                    // complete the re-assembled frame
                    replayBuffer.putBytes(
                        replayOffset[0], buffer, offset + dataOffset, replayLimit[0] - replayOffset[0]);
                    onMessage(state, typeId, replayBuffer, 0, replayLimit[0]);

                    // update offset and length for remaining processing
                    offset += replayLimit[0] - replayOffset[0] + dataOffset;
                    length -= replayLimit[0] - replayOffset[0] + dataOffset;
                    replayOffset[0] = replayLimit[0] = 0;

                    // no remaining data to process
                    if (length == 0)
                    {
                        return;
                    }
                }
            }

            int alignedLength = alignment.supply(state, typeId, buffer, offset, length);
            if (alignedLength == length)
            {
                // propagate aligned frame(s)
                onMessage(state, typeId, buffer, offset, length);
            }
            else if (alignedLength < length)
            {
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
            else
            {
                // retain knowledge of remaining length required for alignment
                replayLimit[0] = alignedLength;

                // retain partial frame for re-assembly
                replayBuffer.putBytes(replayOffset[0], buffer, offset, length);
                replayOffset[0] += length;
            }
        };
    }

    @FunctionalInterface
    public interface StorageSupplier<T>
    {
        MutableDirectBuffer supply(T state);
    }

    @FunctionalInterface
    public interface DataOffsetSupplier
    {
        int supplyAsInt(int typeId);
    }

    @FunctionalInterface
    public interface AlignmentSupplier<T>
    {
        int supply(T state, int typeId, MutableDirectBuffer buffer, int offset, int length);
    }
}
