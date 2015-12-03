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
package org.kaazing.nuklei.specification.echo.streams;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class ConnectIT
{
    private final K3poRule k3po = new K3poRule();

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Before
    public void setupStreamFiles() throws Exception
    {
        int streamCapacity = 1024 * 1024;

        File source = new File("target/nukleus-itests/echo/streams/destination");
        createEmptyFile(source.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        File nukleus = new File("target/nukleus-itests/destination/streams/echo");
        createEmptyFile(nukleus.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);
    }

    @Test
    @Specification({
        "connect/establish.connection/nukleus",
        "connect/establish.connection/destination"
    })
    public void shouldEstablishConnection() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Test
    @Specification({
        "connect/echo.destination.data/nukleus",
        "connect/echo.destination.data/destination"
    })
    public void shouldEchoDestinationData() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Test
    @Specification({
        "connect/initiate.destination.close/nukleus",
        "connect/initiate.destination.close/destination" })
    public void shouldInitiateDestinationClose() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }

    @Test
    @Specification({
        "connect/initiate.nukleus.close/nukleus",
        "connect/initiate.nukleus.close/destination" })
    public void shouldInitiateNukleusClose() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }
}
