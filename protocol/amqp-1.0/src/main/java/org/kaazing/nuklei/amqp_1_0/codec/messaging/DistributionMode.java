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
package org.kaazing.nuklei.amqp_1_0.codec.messaging;

import static java.nio.charset.StandardCharsets.US_ASCII;

import org.kaazing.nuklei.function.DirectBufferAccessor;
import org.kaazing.nuklei.function.MutableDirectBufferMutator;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

/*
 * See AMQP 1.0 specification, section 3.5.7 "Standard Distribution Mode"
 */
public enum DistributionMode
{
    MOVE, COPY;

    public static final DirectBufferAccessor<DistributionMode> READ = (DirectBuffer buffer, int offset, int size) ->
    {
        switch (buffer.getByte(offset))
        {
        case 'm':
            // TODO: verify "move" matches entirely
            return DistributionMode.MOVE;
        case 'c':
            // TODO: verify "copy" matches entirely
            return DistributionMode.COPY;
        default:
            return null;
        }
    };

    private static final byte[] MOVE_BYTES = "move".getBytes(US_ASCII);
    private static final byte[] COPY_BYTES = "copy".getBytes(US_ASCII);

    private static final int MOVE_LENGTH = MOVE_BYTES.length;
    private static final int COPY_LENGTH = COPY_BYTES.length;

    public static final MutableDirectBufferMutator<DistributionMode> WRITE = new MutableDirectBufferMutator<DistributionMode>()
    {

        @Override
        public int mutate(org.kaazing.nuklei.function.MutableDirectBufferMutator.Mutation mutation,
                          MutableDirectBuffer buffer,
                          DistributionMode policy)
        {
            switch (policy)
            {
            case MOVE:
                int moveOffset = mutation.maxOffset(MOVE_LENGTH);
                buffer.putBytes(moveOffset, MOVE_BYTES);
                return MOVE_LENGTH;
            case COPY:
                int copyOffset = mutation.maxOffset(COPY_LENGTH);
                buffer.putBytes(copyOffset, COPY_BYTES);
                return COPY_LENGTH;
            default:
                throw new IllegalArgumentException();
            }
        }
    };

}
