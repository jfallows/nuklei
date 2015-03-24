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
 * See AMQP 1.0 specification, section 3.5.6 "Terminus Expiry Policy"
 */
public enum TerminusExpiryPolicy
{
    LINK_DETACH, SESSION_END, CONNECTION_CLOSE, NEVER;

    public static final DirectBufferAccessor<TerminusExpiryPolicy> READ = (DirectBuffer buffer, int offset, int size) ->
    {
        switch (buffer.getByte(offset))
        {
        case 'l':
            // TODO: verify "link-detach" matches entirely
            return TerminusExpiryPolicy.LINK_DETACH;
        case 's':
            // TODO: verify "session-end" matches entirely
            return TerminusExpiryPolicy.SESSION_END;
        case 'c':
            // TODO: verify "connection-close" matches entirely
            return TerminusExpiryPolicy.CONNECTION_CLOSE;
        case 'n':
            // TODO: verify "never" matches entirely
            return TerminusExpiryPolicy.NEVER;
        default:
            return null;
        }
    };

    private static final byte[] LINK_DETACH_BYTES = "link-detach".getBytes(US_ASCII);
    private static final byte[] SESSION_END_BYTES = "session-end".getBytes(US_ASCII);
    private static final byte[] CONNECTION_CLOSE_BYTES = "connection-close".getBytes(US_ASCII);
    private static final byte[] NEVER_BYTES = "never".getBytes(US_ASCII);

    private static final int LINK_DETACH_LENGTH = LINK_DETACH_BYTES.length;
    private static final int SESSION_END_LENGTH = SESSION_END_BYTES.length;
    private static final int CONNECTION_CLOSE_LENGTH = CONNECTION_CLOSE_BYTES.length;
    private static final int NEVER_LENGTH = NEVER_BYTES.length;

    public static final MutableDirectBufferMutator<TerminusExpiryPolicy> WRITE =
            new MutableDirectBufferMutator<TerminusExpiryPolicy>()
    {

        @Override
        public int mutate(org.kaazing.nuklei.function.MutableDirectBufferMutator.Mutation mutation,
                          MutableDirectBuffer buffer,
                          TerminusExpiryPolicy policy)
        {
            switch (policy)
            {
            case LINK_DETACH:
                buffer.putBytes(mutation.maxOffset(LINK_DETACH_LENGTH), LINK_DETACH_BYTES);
                return LINK_DETACH_LENGTH;
            case SESSION_END:
                buffer.putBytes(mutation.maxOffset(SESSION_END_LENGTH), SESSION_END_BYTES);
                return SESSION_END_LENGTH;
            case CONNECTION_CLOSE:
                buffer.putBytes(mutation.maxOffset(CONNECTION_CLOSE_LENGTH), CONNECTION_CLOSE_BYTES);
                return CONNECTION_CLOSE_LENGTH;
            case NEVER:
                buffer.putBytes(mutation.maxOffset(NEVER_LENGTH), NEVER_BYTES);
                return NEVER_LENGTH;
            default:
                throw new IllegalArgumentException();
            }
        }
    };

}
