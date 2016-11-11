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
        "bind/client/initial/nukleus",
        "bind/client/initial/controller"
    })
    public void shouldBindClientInitial() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "bind/client/reply/nukleus",
        "bind/client/reply/controller"
    })
    public void shouldBindClientReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "bind/server/initial/nukleus",
        "bind/server/initial/controller"
    })
    public void shouldBindServerInitial() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "bind/server/reply/nukleus",
        "bind/server/reply/controller"
    })
    public void shouldBindServerReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "unbind/initial/nukleus",
        "unbind/initial/controller"
    })
    public void shouldUnbindInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "unbind/reply/nukleus",
        "unbind/reply/controller"
    })
    public void shouldUnbindReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
        "route/server/initial/nukleus",
        "route/server/initial/controller"
    })
    public void shouldRouteServerInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND_REPLY");
        k3po.finish();
    }

    @Specification({
        "route/server/reply/nukleus",
        "route/server/reply/controller"
    })
    public void shouldRouteServerReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "route/client/initial/nukleus",
        "route/client/initial/controller"
    })
    public void shouldRouteClientInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
        "route/client/reply/nukleus",
        "route/client/reply/controller"
    })
    public void shouldRouteClientReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "route.protocol/server/initial/nukleus",
        "route.protocol/server/initial/controller"
    })
    public void shouldRouteProtocolServerInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND_REPLY");
        k3po.finish();
    }

    @Specification({
        "route.protocol/server/reply/nukleus",
        "route.protocol/server/reply/controller"
    })
    public void shouldRouteProtocolServerReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "route.protocol/client/initial/nukleus",
        "route.protocol/client/initial/controller"
    })
    public void shouldRouteProtocolClientInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
        "route.protocol/client/reply/nukleus",
        "route.protocol/client/reply/controller"
    })
    public void shouldRouteProtocolClientReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute/client/initial/nukleus",
        "unroute/client/initial/controller"
    })
    public void shouldUnrouteClientInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute/client/reply/nukleus",
        "unroute/client/reply/controller"
    })
    public void shouldUnrouteClientReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute/server/initial/nukleus",
        "unroute/server/initial/controller"
    })
    public void shouldUnrouteServerInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute/server/reply/nukleus",
        "unroute/server/reply/controller"
    })
    public void shouldUnrouteServerReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute.protocol/client/initial/nukleus",
        "unroute.protocol/client/initial/controller"
    })
    public void shouldUnrouteProtocolClientInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute.protocol/client/reply/nukleus",
        "unroute.protocol/client/reply/controller"
    })
    public void shouldUnrouteProtocolClientReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute.protocol/server/initial/nukleus",
        "unroute.protocol/server/initial/controller"
    })
    public void shouldUnrouteProtocolServerInitial() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_INITIAL");
        k3po.finish();
    }

    @Test
    @Specification({
        "unroute.protocol/server/reply/nukleus",
        "unroute.protocol/server/reply/controller"
    })
    public void shouldUnrouteProtocolServerReply() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED_REPLY");
        k3po.finish();
    }
}
