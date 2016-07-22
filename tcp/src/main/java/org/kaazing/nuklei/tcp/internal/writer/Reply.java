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
package org.kaazing.nuklei.tcp.internal.writer;

import java.nio.channels.SocketChannel;

import org.agrona.collections.Long2ObjectHashMap;

public final class Reply
{
    private final Long2ObjectHashMap<Path> registrations;

    public Reply()
    {
        this.registrations = new Long2ObjectHashMap<>();
    }

    public void register(
        long replyId,
        Target target,
        SocketChannel channel)
    {
        registrations.put(replyId, new Path(target, channel));
    }

    public Path consume(
        long replyId)
    {
        return registrations.remove(replyId);
    }

    public final class Path
    {
        private final Target target;
        private final SocketChannel channel;

        private Path(
            Target target,
            SocketChannel channel)
        {
            this.target = target;
            this.channel = channel;
        }

        public Target target()
        {
            return target;
        }

        public SocketChannel channel()
        {
            return channel;
        }
    }
}
