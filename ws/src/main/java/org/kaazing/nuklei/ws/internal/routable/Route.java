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
package org.kaazing.nuklei.ws.internal.routable;

import java.util.Objects;
import java.util.function.Predicate;

public class Route
{
    private final String source;
    private final long sourceRef;
    private final Target target;
    private final long targetRef;
    private final String protocol;

    public Route(
        String source,
        long sourceRef,
        Target target,
        long targetRef,
        String protocol)
    {
        this.source = source;
        this.sourceRef = sourceRef;
        this.target = target;
        this.targetRef = targetRef;
        this.protocol = protocol;
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

    public String protocol()
    {
        return protocol;
    }

    @Override
    public int hashCode()
    {
        int result = source.hashCode();
        result = 31 * result + Long.hashCode(sourceRef);
        result = 31 * result + target.hashCode();
        result = 31 * result + Long.hashCode(targetRef);

        if (protocol != null)
        {
            result = 31 * result + protocol.hashCode();
        }

        return result;
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
                Objects.equals(this.protocol, that.protocol);
    }

    @Override
    public String toString()
    {
        return String.format("[source=\"%s\", sourceRef=%d, target=\"%s\", targetRef=%d, protocol=%s]",
                source, sourceRef, target.name(), targetRef, protocol);
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
        return r -> target.equals(r.target.name());
    }

    public static Predicate<Route> targetRefMatches(
        long targetRef)
    {
        return r -> targetRef == r.targetRef;
    }

    public static Predicate<Route> protocolMatches(
        String protocol)
    {
        return r -> protocol == null || protocol.equals(r.protocol);
    }
}
