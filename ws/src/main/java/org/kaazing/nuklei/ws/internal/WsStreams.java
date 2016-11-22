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
package org.kaazing.nuklei.ws.internal;

import java.nio.file.Path;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.kaazing.nuklei.ws.internal.layouts.StreamsLayout;

public final class WsStreams
{
    private final StreamsLayout layout;
    private final RingBuffer throttle;
    private final RingBuffer streams;

    WsStreams(
        int streamsCapacity,
        int throttleCapacity,
        Path path,
        boolean readonly)
    {
        this.layout = new StreamsLayout.Builder()
                .streamsCapacity(streamsCapacity)
                .throttleCapacity(throttleCapacity)
                .path(path)
                .readonly(readonly)
                .build();

        this.streams = this.layout.streamsBuffer();
        this.throttle = this.layout.throttleBuffer();
    }

    public void close()
    {
        layout.close();
    }

    public int readStreams(
        MessageHandler handler)
    {
        return streams.read(handler);
    }

    public int readThrottle(
        MessageHandler handler)
    {
        return throttle.read(handler);
    }

    public boolean writeStreams(
        int msgTypeId,
        DirectBuffer srcBuffer,
        int srcIndex,
        int length)
    {
        return streams.write(msgTypeId, srcBuffer, srcIndex, length);
    }

    public boolean writeThrottle(
        int msgTypeId,
        DirectBuffer srcBuffer,
        int srcIndex,
        int length)
    {
        return throttle.write(msgTypeId, srcBuffer, srcIndex, length);
    }
}
