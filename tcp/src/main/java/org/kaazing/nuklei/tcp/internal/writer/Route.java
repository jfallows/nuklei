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

import java.net.InetSocketAddress;
import java.util.Objects;

public class Route
{
    private final String source;
    private final long sourceRef;
    private final Target target;
    private final long targetRef;
    private final String reply;
    private final InetSocketAddress address;

    public Route(
        String source,
        long sourceRef,
        Target target,
        long targetRef,
        String reply,
        InetSocketAddress address)
    {
        this.source = source;
        this.sourceRef = sourceRef;
        this.target = target;
        this.targetRef = targetRef;
        this.reply = reply;
        this.address = address;
    }

    public String source()
    {
        return source;
    }

    public long sourceRef()
    {
        return sourceRef;
    }

    public Target target()
    {
        return target;
    }

    public long targetRef()
    {
        return this.targetRef;
    }

    public String reply()
    {
        return reply;
    }

    public InetSocketAddress address()
    {
        return address;
    }

    @Override
    public int hashCode()
    {
        return address.hashCode();
    }

    @Override
    public boolean equals(
        Object obj)
    {
        if (!(obj instanceof Route))
        {
            return false;
        }

        Route that = (Route) obj;
        return this.sourceRef == that.sourceRef &&
                this.targetRef == that.targetRef &&
                Objects.equals(this.source, that.source) &&
                Objects.equals(this.target, that.target) &&
                Objects.equals(this.reply, that.reply) &&
                Objects.equals(this.address, that.address);
    }

    @Override
    public String toString()
    {
        return String.format("[source=\"%s\", sourceRef=%d, target=\"%s\", targetRef=%d, reply=\"%s\", address=%s]",
                source, sourceRef, target, targetRef, reply, address);
    }
}
