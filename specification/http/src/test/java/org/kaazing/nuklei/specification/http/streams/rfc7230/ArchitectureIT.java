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
package org.kaazing.nuklei.specification.http.streams.rfc7230;

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

public class ArchitectureIT
{
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Before
    public void setupStreamFiles() throws Exception
    {
        int streamCapacity = 1024 * 1024;

        File sourceInitial = new File("target/nukleus-itests/http/streams/source");
        createEmptyFile(sourceInitial.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        File sourceReply = new File("target/nukleus-itests/source/streams/http");
        createEmptyFile(sourceReply.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        File destinationInitial = new File("target/nukleus-itests/http/streams/destination");
        createEmptyFile(destinationInitial.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);

        File destinationReply = new File("target/nukleus-itests/destination/streams/http");
        createEmptyFile(destinationReply.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);
    }

    @Test
    @Specification({
        "nuklei/specification/http/control/capture.source.destination/controller",
        "nuklei/specification/http/control/capture.source.destination/nukleus",
        "nuklei/specification/http/control/route.source.destination/controller",
        "nuklei/specification/http/control/route.source.destination/nukleus",
        "nuklei/specification/http/control/bind.source.destination/controller",
        "nuklei/specification/http/control/bind.source.destination/nukleus",
        "nuklei/specification/http/streams/rfc7230/inbound.must.send.version/source",
        "nuklei/specification/http/streams/rfc7230/inbound.must.send.version/nukleus",
        "nuklei/specification/http/streams/rfc7230/inbound.must.send.version/destination" })
    public void inboundMustSendVersion() throws Exception
    {
        k3po.finish();
    }
}
