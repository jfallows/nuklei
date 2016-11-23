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
package org.kaazing.nuklei.http.internal.streams.server.rfc7230;

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

public class ConnectionManagementIT
{
    private final K3poRule k3po = new K3poRule()
            .addScriptRoot("control", "org/kaazing/specification/nuklei/http/control")
            .addScriptRoot("streams", "org/kaazing/specification/nuklei/http/streams/rfc7230/connection.management");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("http")
        .directory("target/nukleus-itests")
        .commandBufferCapacity(1024)
        .responseBufferCapacity(1024)
        .counterValuesBufferCapacity(1024)
        .streams("http", "source")
        .streams("rejectTarget", "http#source")
        .streams("target", "http#source")
        .streams("http", "replySource")
        .streams("replyTarget", "http#replySource");

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Test
    @Specification({
        "${control}/bind/server/initial/controller",
        "${control}/bind/server/reply/controller",
        "${control}/route/server/initial/controller",
        "${control}/route/server/reply/controller",
        "${streams}/response.status.101.with.upgrade/server/source",
        "${streams}/response.status.101.with.upgrade/server/target" })
    public void shouldSwitchProtocolAfterUpgrade() throws Exception
    {
        k3po.finish();
    }
}
