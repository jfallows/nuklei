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
package org.kaazing.nuklei.tcp.internal;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.kaazing.nuklei.tcp.internal.layouts.StreamsLayout;

public final class TcpReadableStreams
{
    private final StreamsLayout routeStreams;
    private final RingBuffer routeBuffer;
    private final RingBuffer throttleBuffer;

    TcpReadableStreams(
        Context context,
        String source,
        String target)
    {
        this.routeStreams = new StreamsLayout.Builder().streamsCapacity(context.streamsBufferCapacity())
                                                       .throttleCapacity(context.throttleBufferCapacity())
                                                       .path(context.routeStreamsPath().apply(source, target))
                                                       .readonly(true)
                                                       .build();
        this.routeBuffer = this.routeStreams.streamsBuffer();
        this.throttleBuffer = this.routeStreams.throttleBuffer();
    }

    public void close()
    {
        routeStreams.close();
    }

    public int read(
        MessageHandler handler)
    {
        return routeBuffer.read(handler);
    }

    public boolean write(
        int msgTypeId,
        DirectBuffer srcBuffer,
        int srcIndex,
        int length)
    {
        return throttleBuffer.write(msgTypeId, srcBuffer, srcIndex, length);
    }

    public long consumerPosition()
    {
        return routeBuffer.consumerPosition();
    }
}
