/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.http.internal.routable.stream;

import java.util.List;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.kaazing.nuklei.http.internal.routable.Route;
import org.kaazing.nuklei.http.internal.routable.Source;

public final class ClientReplyStreamFactory
{
    private final Source source;
    private final LongFunction<List<Route>> supplyRoutes;
    private final LongSupplier supplyTargetId;
    private final LongUnaryOperator correlateReply;

    public ClientReplyStreamFactory(
        Source source,
        LongFunction<List<Route>> supplyRoutes,
        LongSupplier supplyTargetId,
        LongUnaryOperator correlateReply)
    {
        this.source = source;
        this.supplyRoutes = supplyRoutes;
        this.supplyTargetId = supplyTargetId;
        this.correlateReply = correlateReply;
    }

    public MessageHandler newStream()
    {
        return new ClientReplyStream()::handleStream;
    }

    private final class ClientReplyStream
    {
        private ClientReplyStream()
        {
            // TODO Auto-generated constructor stub
        }

        private void handleStream(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            // TODO:
        }
    }
}
