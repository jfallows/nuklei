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
package org.kaazing.nuklei.http.internal;

import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.concurrent.status.CountersManager;

public final class Counters implements AutoCloseable
{
    private final AtomicCounter targetsBound;
    private final AtomicCounter streamsTargeted;

    Counters(CountersManager countersManager)
    {
        targetsBound = countersManager.newCounter("targetsBound");
        streamsTargeted = countersManager.newCounter("streamsTargeted");
    }

    @Override
    public void close() throws Exception
    {
        targetsBound.close();
        streamsTargeted.close();
    }

    public AtomicCounter targetsBound()
    {
        return targetsBound;
    }

    public AtomicCounter streamsTargeted()
    {
        return streamsTargeted;
    }
}
