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

public class ConnectionManagementIT
{
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule()
            .directory("target/nukleus-itests")
            .streams("http", "source")
            .streams("target", "http#source")
            .streams("http", "target")
            .streams("source", "http#target");

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Test
    @Specification({
        "nuklei/http/control/bind/controller",
        "nuklei/http/control/bind/nukleus",
        "nuklei/http/control/route/controller",
        "nuklei/http/control/route/nukleus",
        "nuklei/http/streams/rfc7230/connection.management/payload.bytes.passthrough.verbatim.after.101.upgrade/source",
        "nuklei/http/streams/rfc7230/connection.management/payload.bytes.passthrough.verbatim.after.101.upgrade/nukleus",
        "nuklei/http/streams/rfc7230/connection.management/payload.bytes.passthrough.verbatim.after.101.upgrade/target" })
    public void shouldPassthroughPayloadBytesAfter101Upgrade() throws Exception
    {
        k3po.finish();
    }
}
