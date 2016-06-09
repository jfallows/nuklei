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
package org.kaazing.specification.nuklei.ws.control;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class ControlIT
{
    private final K3poRule k3po = new K3poRule();

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Test
    @Specification({
        "bind/nukleus",
        "bind/controller"
    })
    public void shouldBind() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "unbind/nukleus",
        "unbind/controller"
    })
    public void shouldUnbind() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTABLE");
        k3po.finish();
    }

    @Test
    @Specification({
        "prepare/nukleus",
        "prepare/controller"
    })
    public void shouldPrepare() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "unprepare/nukleus",
        "unprepare/controller"
    })
    public void shouldUnprepare() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTABLE");
        k3po.finish();
    }

    @Test
    @Specification({
        "route/nukleus",
        "route/controller"
    })
    public void shouldRoute() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTABLE");
        k3po.finish();
    }

    @Test
    @Specification({
        "route.protocol/nukleus",
        "route.protocol/controller"
    })
    public void shouldRouteProtocol() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTABLE");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute/nukleus",
        "unroute/controller"
    })
    public void shouldUnroute() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute.protocol/nukleus",
        "unroute.protocol/controller"
    })
    public void shouldUnrouteProtocol() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED");
        k3po.finish();
    }
}
