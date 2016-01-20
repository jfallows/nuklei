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
package org.kaazing.specification.nuklei.http.streams.rfc7230;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.specification.nuklei.common.NukleusRule;

public class ArchitectureIT
{
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule()
        .directory("target/nukleus-itests")
        .initialize("http", "source")
        .initialize("source", "http")
        .initialize("http", "destination")
        .initialize("destination", "http");

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/prepare.source.destination/controller",
        "nuklei/http/control/prepare.source.destination/nukleus",
        "nuklei/http/streams/rfc7230/architecture/outbound.must.send.version/source",
        "nuklei/http/streams/rfc7230/architecture/outbound.must.send.version/nukleus",
//      "http/rfc7230/architecture/outbound.must.send.version/response",
        "nuklei/http/streams/rfc7230/architecture/outbound.must.send.version/destination" })
    public void outboundMustSendVersion() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/bind.source.destination/controller",
        "nuklei/http/control/bind.source.destination/nukleus",
//      "http/rfc7230/architecture/inbound.must.send.version/request",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.send.version/source",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.send.version/nukleus",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.send.version/destination" })
    public void inboundMustSendVersion() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/bind.source.destination/controller",
        "nuklei/http/control/bind.source.destination/nukleus",
//      "http/rfc7230/architecture/response.must.be.505.on.invalid.version/request",
        "nuklei/http/streams/rfc7230/architecture/response.must.be.505.on.invalid.version/source",
        "nuklei/http/streams/rfc7230/architecture/response.must.be.505.on.invalid.version/nukleus" })
    public void inboundMustSend505OnInvalidVersion() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/bind.source.destination/controller",
        "nuklei/http/control/bind.source.destination/nukleus",
//      "http/rfc7230/architecture/inbound.must.reply.with.version.one.dot.one.when.received.higher.minor.version/request",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.reply.with.http.1.1.when.received.http.1.2+/source",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.reply.with.http.1.1.when.received.http.1.2+/nukleus",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.reply.with.http.1.1.when.received.http.1.2+/destination" })
    public void inboundMustReplyWithHttpOneDotOneWhenReceivedHttpOneDotTwoPlus() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/bind.source.destination/controller",
        "nuklei/http/control/bind.source.destination/nukleus",
//      "http/rfc7230/architecture/origin.server.should.send.505.on.major.version.not.equal.to.one/request",
        "nuklei/http/streams/rfc7230/architecture/origin.server.should.send.505.on.major.version.not.equal.to.one/source",
        "nuklei/http/streams/rfc7230/architecture/origin.server.should.send.505.on.major.version.not.equal.to.one/nukleus" })
    public void originServerShouldSend505OnMajorVersionNotEqualToOne() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/prepare.source.destination/controller",
        "nuklei/http/control/prepare.source.destination/nukleus",
        "nuklei/http/streams/rfc7230/architecture/client.must.send.host.identifier/source",
        "nuklei/http/streams/rfc7230/architecture/client.must.send.host.identifier/nukleus",
//      "http/rfc7230/architecture/client.must.send.host.identifier/response",
        "nuklei/http/streams/rfc7230/architecture/client.must.send.host.identifier/destination" })
    public void clientMustSendHostIdentifier() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/bind.source.destination/controller",
        "nuklei/http/control/bind.source.destination/nukleus",
//      "http/rfc7230/architecture/inbound.must.reject.requests.missing.host.identifier/request",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.reject.requests.missing.host.identifier/source",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.reject.requests.missing.host.identifier/nukleus" })
    public void inboundMustRejectRequestsMissingHostIdentifier() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/bind.source.destination/controller",
        "nuklei/http/control/bind.source.destination/nukleus",
//      "http/rfc7230/architecture/inbound.must.reject.requests.with.user.info.on.uri/request",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.reject.requests.with.user.info.on.uri/source",
        "nuklei/http/streams/rfc7230/architecture/inbound.must.reject.requests.with.user.info.on.uri/nukleus" })
    public void inboundMustRejectRequestWithUserInfoOnURI() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "nuklei/http/control/capture.source.destination/controller",
        "nuklei/http/control/capture.source.destination/nukleus",
        "nuklei/http/control/route.source.destination/controller",
        "nuklei/http/control/route.source.destination/nukleus",
        "nuklei/http/control/bind.source.destination/controller",
        "nuklei/http/control/bind.source.destination/nukleus",
//      "http/rfc7230/architecture/inbound.should.allow.requests.with.percent.chars.in.uri/request",
        "nuklei/http/streams/rfc7230/architecture/inbound.should.allow.requests.with.percent.chars.in.uri/source",
        "nuklei/http/streams/rfc7230/architecture/inbound.should.allow.requests.with.percent.chars.in.uri/nukleus",
        "nuklei/http/streams/rfc7230/architecture/inbound.should.allow.requests.with.percent.chars.in.uri/destination" })
    public void inboundShouldAllowRequestsWithPercentCharsInURI() throws Exception
    {
        k3po.finish();
    }
}
