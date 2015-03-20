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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

@FunctionalInterface
public interface AlignedMikro<T> extends StatefulMikro<T>
{
    default StatefulMikro<T> alignedBy(StorageSupplier<T> storage, AlignmentSupplier<T> alignment)
    {
        // indicies into the array of offset/limit for the associated replayBuffer
        int offsetIndex = 0;
        int limitIndex = 1;
        Objects.requireNonNull(storage);
        Map<MutableDirectBuffer, int[]> offsetAndLimitMap = new HashMap<MutableDirectBuffer, int[]>();
        return (state, header, typeId, buffer, offset, length) ->
        {
            MutableDirectBuffer replayBuffer = storage.supply(state);

            int[] offsetAndLimit = offsetAndLimitMap.get(replayBuffer);
            if (offsetAndLimit == null)
            {
                offsetAndLimit = new int[] { 0, 0 };
                offsetAndLimitMap.put(replayBuffer, offsetAndLimit);
            }

            // determine alignment boundary
            if (offsetAndLimit[offsetIndex] != offsetAndLimit[limitIndex])
            {
                int newReplayOffset = offsetAndLimit[offsetIndex] + length;
                if (newReplayOffset < offsetAndLimit[limitIndex])
                {
                    // retain partial frame for re-assembly
                    replayBuffer.putBytes(offsetAndLimit[offsetIndex], buffer, offset, length);
                    offsetAndLimit[offsetIndex] = newReplayOffset;

                    // no remaining data to process
                    return;
                }
                else
                {
                    // complete the re-assembled frame
                    replayBuffer.putBytes(
                            offsetAndLimit[offsetIndex], buffer, offset,
                            offsetAndLimit[limitIndex] - offsetAndLimit[offsetIndex]);

                    onMessage(state, header, typeId, replayBuffer, 0, offsetAndLimit[limitIndex]);

                    // update offset and length for remaining processing
                    offset += offsetAndLimit[limitIndex] - offsetAndLimit[offsetIndex];
                    length -= offsetAndLimit[limitIndex] - offsetAndLimit[offsetIndex];
                    offsetAndLimit[offsetIndex] = offsetAndLimit[limitIndex] = 0;

                    // no remaining data to process
                    if (length == 0)
                    {
                        return;
                    }
                }
            }

            int alignedLength = alignment.supply(state, header, typeId, buffer, offset, length);
            if (alignedLength == length)
            {
                // propagate aligned frame(s)
                onMessage(state, header, typeId, buffer, offset, length);
            }
            else if (alignedLength < length)
            {
                // propagate aligned frame(s)
                onMessage(state, header, typeId, buffer, offset, alignedLength);

                // retain partial frame for re-assembly
                offset += alignedLength;
                length -= alignedLength;
                replayBuffer.putBytes(offsetAndLimit[offsetIndex], buffer, offset, length);

                offsetAndLimit[offsetIndex] += length;
                offsetAndLimit[limitIndex] = alignment.supply(state, header, typeId, replayBuffer,
                        0, offsetAndLimit[offsetIndex]);
            }
            else
            {
                // retain knowledge of remaining length required for alignment
                offsetAndLimit[limitIndex] = alignedLength;

                // retain partial frame for re-assembly
                replayBuffer.putBytes(offsetAndLimit[offsetIndex], buffer, offset, length);
                offsetAndLimit[offsetIndex] += length;
            }
        };
    }

    @FunctionalInterface
    public interface StorageSupplier<T>
    {
        MutableDirectBuffer supply(T state);
    }

    @FunctionalInterface
    public interface AlignmentSupplier<T>
    {
        int supply(T state, Object header, int typeId, DirectBuffer buffer, int offset, int length);
    }
}
