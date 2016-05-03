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
package org.kaazing.nuklei.http.internal.streams.rfc7230;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.nuklei.test.NukleusRule;

public class MessageFormatIT
{
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("http")
            .directory("target/nukleus-itests")
            .commandBufferCapacity(1024)
            .responseBufferCapacity(1024)
            .counterValuesBufferCapacity(1024)
            .initialize("destination", "http")
            .initialize("source", "http");

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/bind.source.destination/controller",
//      "http/rfc7230/message.format/inbound.should.process.request.with.content.length/request",
        "nuklei/http/streams/rfc7230/message.format/inbound.should.process.request.with.content.length/source",
        "nuklei/http/streams/rfc7230/message.format/inbound.should.process.request.with.content.length/destination" })
    public void inboundShouldProcessRequestWithContentLength() throws Exception
    {
//      k3po.property("transport", "nuklei://bidirectional/http/streams/source#sourceRef");
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/prepare.source.destination/controller",
        "nuklei/http/streams/rfc7230/message.format/outbound.should.accept.headers/source",
//      "http/rfc7230/message.format/outbound.should.accept.headers/response",
        "nuklei/http/streams/rfc7230/message.format/outbound.should.accept.headers/destination" })
    public void ouboundShouldAcceptHeaders() throws Exception
    {
        k3po.finish();
    }
}
