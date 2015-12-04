/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.http.internal.translator;

public class BindState
{
    private final long handlerRef;
    private final String source;
    private final long sourceRef;
    private final Object headers;
    private final String handler;

    public BindState(
        String source,
        long sourceRef,
        Object headers,
        String handler,
        long handlerRef)
    {
        this.source = source;
        this.sourceRef = sourceRef;
        this.headers = headers;
        this.handler = handler;
        this.handlerRef = handlerRef;
    }

    public String source()
    {
        return source;
    }

    public long sourceRef()
    {
        return this.sourceRef;
    }

    public Object headers()
    {
        return headers;
    }

    public String handler()
    {
        return handler;
    }

    public long handlerRef()
    {
        return this.handlerRef;
    }

    @Override
    public String toString()
    {
        return String.format("[source=\"%s\", sourceRef=%d, headers=%s, handlerRef=%d]",
                source, sourceRef, headers, handlerRef);
    }
}
