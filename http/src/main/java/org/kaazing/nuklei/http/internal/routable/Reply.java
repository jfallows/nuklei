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

import java.util.function.Function;

import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.MessageHandler;

public final class Reply
{
    private final Long2ObjectHashMap<Function<Source, MessageHandler>> registrations;

    public Reply()
    {
        this.registrations = new Long2ObjectHashMap<>();
    }

    public MessageHandler newStream(
        Source source,
        long sourceId)
    {
        final Function<Source, MessageHandler> function = registrations.remove(sourceId);
        return function != null ? function.apply(source) : null;
    }

    public void register(
        long replyId,
        Function<Source, MessageHandler> function)
    {
        registrations.put(replyId, function);
    }
}