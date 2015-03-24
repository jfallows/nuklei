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

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

@FunctionalInterface
public interface AlignedMikro<T> extends StatefulMikro<T>
{
    default StatefulMikro<T> alignedBy(StorageSupplier<T> storage, AlignmentSupplier<T> alignment)
    {
        Objects.requireNonNull(storage);
        return (state, header, typeId, buffer, offset, length) ->
        {
            Storage replayStorage = storage.supply(state);
            MutableDirectBuffer replayBuffer = replayStorage.buffer();
            int replayLimit = replayStorage.limit();
            int replayOffset = replayStorage.offset();

            // determine alignment boundary
            if (replayOffset != replayLimit)
            {
                int newReplayOffset = replayOffset + length;
                if (newReplayOffset < replayLimit)
                {
                    // retain partial frame for re-assembly
                    replayBuffer.putBytes(replayOffset, buffer, offset, length);
                    replayStorage.offset(newReplayOffset);

                    // no remaining data to process
                    return;
                }
                else
                {
                    // complete the re-assembled frame
                    replayBuffer.putBytes(
                            replayOffset, buffer, offset,
                            replayLimit - replayOffset);

                    onMessage(state, header, typeId, replayBuffer, 0, replayLimit);

                    // update offset and length for remaining processing
                    offset += replayLimit - replayOffset;
                    length -= replayLimit - replayOffset;

                    replayLimit = replayOffset = 0;
                    replayStorage.offset(replayOffset);
                    replayStorage.limit(replayLimit);

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
                replayBuffer.putBytes(replayOffset, buffer, offset, length);

                replayOffset += length;
                replayLimit = alignment.supply(state, header, typeId, replayBuffer,
                        0, replayOffset);
                replayStorage.offset(replayOffset);
                replayStorage.limit(replayLimit);
            }
            else
            {
                // retain knowledge of remaining length required for alignment
                replayStorage.limit(alignedLength);

                // retain partial frame for re-assembly
                replayBuffer.putBytes(replayOffset, buffer, offset, length);
                replayStorage.offset(replayOffset + length);
            }
        };
    }

    @FunctionalInterface
    public interface StorageSupplier<T>
    {
        Storage supply(T state);
    }

    @FunctionalInterface
    public interface AlignmentSupplier<T>
    {
        int supply(T state, Object header, int typeId, DirectBuffer buffer, int offset, int length);
    }

    public static class Storage
    {
        private MutableDirectBuffer buffer;
        private int limit;
        private int offset;

        public Storage()
        {
            this.limit = 0;
            this.offset = 0;
        }

        public Storage(MutableDirectBuffer buffer)
        {
            this.buffer = buffer;
            this.limit = 0;
            this.offset = 0;
        }

        public MutableDirectBuffer buffer()
        {
            return buffer;
        }
        public Storage buffer(MutableDirectBuffer buffer)
        {
            this.buffer = buffer;
            return this;
        }
        public int limit()
        {
            return limit;
        }
        public void limit(int limit)
        {
            this.limit = limit;
        }
        public int offset()
        {
            return offset;
        }
        public void offset(int offset)
        {
            this.offset = offset;
        }
    }
}
