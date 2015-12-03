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
package org.kaazing.nuklei.specification.http.control;

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
        "bind.source/nukleus",
        "bind.source/controller"
    })
    public void shouldBindSource() throws Exception
    {
        k3po.finish();
    }
/*
    @Test
    @Specification({
        "unbind.source/controller",
        "unbind.source/nukleus"
    })
    public void shouldUnbindSource() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "prepare.destination/controller",
        "prepare.destination/nukleus"
    })
    public void shouldPrepareDestination() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "unprepare.destination/controller",
        "unprepare.destination/nukleus"
    })
    public void shouldUnprepareDestination() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "connect.destination/controller",
        "connect.destination/nukleus"
    })
    public void shouldConnectDestination() throws Exception
    {
        k3po.finish();
    }
*/
}
