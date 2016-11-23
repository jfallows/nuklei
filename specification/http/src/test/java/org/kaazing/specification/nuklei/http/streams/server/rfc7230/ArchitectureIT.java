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
package org.kaazing.specification.nuklei.http.streams.server.rfc7230;

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
    private final K3poRule k3po = new K3poRule()
            .addScriptRoot("streams", "org/kaazing/specification/nuklei/http/streams/rfc7230/architecture")
            .addScriptRoot("http", "org/kaazing/specification/http/rfc7230/architecture");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule()
        .directory("target/nukleus-itests")
        .streams("http", "source")
        .streams("rejectTarget", "http#source")
        .streams("target", "http#source")
        .streams("http", "replySource")
        .streams("replyTarget", "http#replySource");

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Test
    @Specification({
//      "${http}/request.and.response/request",
        "${streams}/request.and.response/server/source",
        "${streams}/request.and.response/server/nukleus",
        "${streams}/request.and.response/server/target" })
    public void shouldCorrelateRequestAndResponse() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
//      "${http}/request.header.host.missing/request",
        "${streams}/request.header.host.missing/server/source",
        "${streams}/request.header.host.missing/server/nukleus" })
    public void shouldRejectRequestWhenHostHeaderMissing() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
//      "${http}/request.version.http.1.2+/request",
        "${streams}/request.version.http.1.2+/server/source",
        "${streams}/request.version.http.1.2+/server/nukleus",
        "${streams}/request.version.http.1.2+/server/target" })
    public void shouldRespondVersionHttp11WhenRequestVersionHttp12plus() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
//      "${http}/request.version.not.invalid/request",
        "${streams}/request.version.invalid/server/source",
        "${streams}/request.version.invalid/server/nukleus" })
    public void shouldRejectRequestWhenVersionInvalid() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
//      "${http}/request.version.not.http.1.x/request",
        "${streams}/request.version.not.http.1.x/server/source",
        "${streams}/request.version.not.http.1.x/server/nukleus" })
    public void shouldRejectRequestWhenVersionNotHttp1x() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
//      "${http}/request.uri.with.user.info/request",
        "${streams}/request.uri.with.user.info/server/source",
        "${streams}/request.uri.with.user.info/server/nukleus" })
    public void shouldRejectRequestWithUserInfo() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
//      "${http}/request.uri.with.percent.chars/request",
        "${streams}/request.uri.with.percent.chars/server/source",
        "${streams}/request.uri.with.percent.chars/server/nukleus",
        "${streams}/request.uri.with.percent.chars/server/target" })
    public void shouldAcceptRequestWithPercentChars() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }
}
