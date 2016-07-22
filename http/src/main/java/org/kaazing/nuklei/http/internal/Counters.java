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
    private final AtomicCounter streamsBound;
    private final AtomicCounter streamsPrepared;
    private final AtomicCounter streamsAccepted;
    private final AtomicCounter streamsConnected;

    Counters(CountersManager countersManager)
    {
        streamsBound = countersManager.newCounter("streamsBound");
        streamsPrepared = countersManager.newCounter("streamsPrepared");
        streamsAccepted = countersManager.newCounter("streamsAccepted");
        streamsConnected = countersManager.newCounter("streamsConnected");
    }

    @Override
    public void close() throws Exception
    {
        streamsBound.close();
        streamsPrepared.close();
        streamsAccepted.close();
        streamsConnected.close();
    }

    public AtomicCounter streamsBound()
    {
        return streamsBound;
    }

    public AtomicCounter streamsPrepared()
    {
        return streamsPrepared;
    }

    public AtomicCounter streamsAccepted()
    {
        return streamsAccepted;
    }

    public AtomicCounter streamsConnected()
    {
        return streamsConnected;
    }
}
