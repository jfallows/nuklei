/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY ERROR_TYPE_ID, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal;

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

import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastBufferDescriptor;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class ControlIT
{
    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/nuklei/specification/tcp/control");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("tcp")
            .setDirectory("target/nukleus-itests")
            .setConductorBufferLength(1024 + RingBufferDescriptor.TRAILER_LENGTH)
            .setBroadcastBufferLength(1024 + BroadcastBufferDescriptor.TRAILER_LENGTH)
            .setCounterValuesBufferLength(1024);

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout).around(nukleus);

    @Test
    @Specification({
        "bind.address.and.port/controller"
    })
    public void shouldBindAddressAndPort() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "unbind.address.and.port/controller",
    })
    public void shouldUnbindAddressAndPort() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "prepare.address.and.port/controller",
    })
    public void shouldPrepareAddressAndPort() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "unprepare.address.and.port/controller",
    })
    public void shouldUnprepareAddressAndPort() throws Exception
    {
        k3po.finish();
    }

}