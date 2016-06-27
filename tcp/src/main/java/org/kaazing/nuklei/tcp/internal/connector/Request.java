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
package org.kaazing.nuklei.tcp.internal.connector;

import java.nio.channels.SocketChannel;

public class Request
{
    private final String sourceName;
    private final long sourceRef;
    private final long sourceId;
    private final String targetName;
    private final long targetRef;
    private final long targetId;
    private final String replyName;
    private final long replyRef;
    private final long replyId;
    private final SocketChannel channel;
    private final Runnable onsuccess;
    private final Runnable onfailure;

    public Request(
        String sourceName,
        long sourceRef,
        long sourceId,
        String targetName,
        long targetRef,
        long targetId,
        String replyName,
        long replyRef,
        long replyId,
        SocketChannel channel,
        Runnable onsuccess,
        Runnable onfailure)
    {
        this.sourceName = sourceName;
        this.sourceRef = sourceRef;
        this.sourceId = sourceId;
        this.targetName = targetName;
        this.targetRef = targetRef;
        this.targetId = targetId;
        this.replyName = replyName;
        this.replyRef = replyRef;
        this.replyId = replyId;
        this.channel = channel;
        this.onsuccess = onsuccess;
        this.onfailure = onfailure;
    }

    public String source()
    {
        return sourceName;
    }

    public long sourceRef()
    {
        return sourceRef;
    }

    public long sourceId()
    {
        return sourceId;
    }

    public String target()
    {
        return targetName;
    }

    public long targetRef()
    {
        return targetRef;
    }

    public long targetId()
    {
        return targetId;
    }

    public String reply()
    {
        return replyName;
    }

    public long replyRef()
    {
        return replyRef;
    }

    public long replyId()
    {
        return replyId;
    }

    public SocketChannel channel()
    {
        return channel;
    }

    public void fail()
    {
        onfailure.run();
    }

    public void succeed()
    {
        onsuccess.run();
    }

    @Override
    public String toString()
    {
        return String.format("[source=%s, sourceRef=%d, sourceId=%d, replyName=%s, replyRef=%d, replyId=%d, channel=%s]",
                sourceName, sourceRef, sourceId, replyName, replyRef, replyId, channel);
    }
}
