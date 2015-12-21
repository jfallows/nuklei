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
package org.kaazing.specification.nuklei.http.control;

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
        "capture.source.destination/nukleus",
        "capture.source.destination/controller"
    })
    public void shouldCaptureSourceAndDestination() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "route.source.destination/nukleus",
        "route.source.destination/controller"
    })
    public void shouldRouteSourceAndDestination() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("CAPTURED");
        k3po.finish();
    }

    @Test
    @Specification({
        "bind.source.destination/nukleus",
        "bind.source.destination/controller"
    })
    public void shouldBindSourceToDestination() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED");
        k3po.finish();
    }

    @Test
    @Specification({
        "unbind.source.destination/controller",
        "unbind.source.destination/nukleus"
    })
    public void shouldUnbindSourceDestination() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("BOUND");
        k3po.finish();
    }

    @Test
    @Specification({
        "prepare.source.destination/controller",
        "prepare.source.destination/nukleus"
    })
    public void shouldPrepareDestination() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("ROUTED");
        k3po.finish();
    }

    @Test
    @Specification({
        "unprepare.source.destination/controller",
        "unprepare.source.destination/nukleus"
    })
    public void shouldUnprepareSourceDestination() throws Exception
    {
        k3po.start();
        k3po.notifyBarrier("PREPARED");
        k3po.finish();
    }
}
