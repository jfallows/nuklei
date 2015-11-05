/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.nuklei.specification.tcp.stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class StreamIT
{
    private final K3poRule k3po = new K3poRule();

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Test
    @Specification({
        "accept.begin.ipv4.address.and.port/nukleus",
        "accept.begin.ipv4.address.and.port/destination"
    })
    public void shouldBeginAcceptedIPv4AddressAndPort() throws Exception
    {
        createStreamFile(new File("target/nukleus-itests/tcp/destination.accepts"), 1024 * 1024);

        k3po.start();
        k3po.notifyBarrier("BOUND");
        k3po.finish();
    }

    private static void createStreamFile(File location, int streamCapacity)
    {
        File absolute = location.getAbsoluteFile();
        int sourceLength = streamCapacity + RingBufferDescriptor.TRAILER_LENGTH;
        int destinationLength = streamCapacity + RingBufferDescriptor.TRAILER_LENGTH;
        createEmptyFile(absolute, sourceLength + destinationLength);
    }
}