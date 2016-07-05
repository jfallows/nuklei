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
package org.kaazing.nuklei.http.internal.routable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

import uk.co.real_logic.agrona.concurrent.MessageHandler;

public final class Route
{
    private final HandlerFactory handlerFactory;
    private final Target replyTo;
    private final LongSupplier newTargetId;
    private final List<Option> options;

    public Route(
        HandlerFactory handlerFactory,
        Target replyTo,
        LongSupplier newTargetId)
    {
        this.handlerFactory = handlerFactory;
        this.replyTo = replyTo;
        this.newTargetId = newTargetId;
        this.options = new LinkedList<>();
    }

    public MessageHandler newStream(
        Source source,
        long streamId)
    {
        return handlerFactory.newHandler(this, source, streamId);
    }

    public Target replyTo()
    {
        return replyTo;
    }

    public long newTargetId()
    {
        return newTargetId.getAsLong();
    }

    public boolean add(
        Map<String, String> headers,
        Target target,
        long targetRef,
        String reply)
    {
        return options.add(new Option(headers, target, targetRef, reply));
    }

    public Option resolve(
        Map<String, String> headers)
    {
        return options.stream().filter(o -> headers.entrySet().containsAll(o.headers.entrySet())).findFirst().orElse(null);
    }

    public boolean removeIf(
        Map<String, String> headers,
        Target target,
        long targetRef,
        String reply)
    {
        return options.removeIf(o -> o.equalTo(headers, target, targetRef, reply));
    }

    public static final class Option
    {
        private final Map<String, String> headers;
        private final Target target;
        private final long targetRef;
        private final String reply;

        Option(
            Map<String, String> headers,
            Target target,
            long targetRef,
            String reply)
        {
            this.headers = headers;
            this.target = target;
            this.targetRef = targetRef;
            this.reply = reply;
        }

        public Map<String, String> headers()
        {
            return headers;
        }

        public Target target()
        {
            return target;
        }

        public long targetRef()
        {
            return targetRef;
        }

        public String reply()
        {
            return reply;
        }

        boolean equalTo(
            Map<String, String> headers,
            Target target,
            long targetRef,
            String reply)
        {
            return this.targetRef == targetRef &&
                    Objects.equals(this.target, target) &&
                    Objects.equals(this.reply, reply) &&
                    Objects.equals(this.headers, headers);
        }
    }
}