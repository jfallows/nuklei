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
package org.kaazing.nuklei.tcp.internal.acceptor;

import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.function.Predicate;

public class Route
{
    private final String source;
    private final long sourceRef;
    private final String target;
    private final long targetRef;
    private final String description;
    private final Predicate<SocketChannel> condition;

    public Route(
        String source,
        long sourceRef,
        String target,
        long targetRef,
        String description,
        Predicate<SocketChannel> condition)
    {
        this.source = source;
        this.sourceRef = sourceRef;
        this.target = target;
        this.targetRef = targetRef;
        this.description = description;
        this.condition = condition;
    }

    public String source()
    {
        return source;
    }

    public long sourceRef()
    {
        return sourceRef;
    }

    public String target()
    {
        return target;
    }

    public long targetRef()
    {
        return this.targetRef;
    }

    public String description()
    {
        return description;
    }

    public Predicate<SocketChannel> condition()
    {
        return condition;
    }

    @Override
    public int hashCode()
    {
        return description.hashCode();
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
                Objects.equals(this.description, that.description);
    }

    @Override
    public String toString()
    {
        return String.format("[source=\"%s\", sourceRef=%d, target=\"%s\", targetRef=%d, description=%s]",
                source, sourceRef, target, targetRef, description);
    }

    public static Predicate<Route> sourceMatches(
        String source)
    {
        Objects.requireNonNull(source);
        return r -> source.equals(r.source);
    }

    public static Predicate<Route> sourceRefMatches(
        long sourceRef)
    {
        return r -> sourceRef == r.sourceRef;
    }

    public static Predicate<Route> targetMatches(
        String target)
    {
        Objects.requireNonNull(target);
        return r -> target.equals(r.target);
    }

    public static Predicate<Route> targetRefMatches(
        long targetRef)
    {
        return r -> targetRef == r.targetRef;
    }

    public static Predicate<Route> descriptionMatches(
        String description)
    {
        Objects.requireNonNull(description);
        return r -> description.equals(r.description);
    }
}
