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
package org.kaazing.nuklei.echo.internal.streams;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;
import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.nuklei.test.NukleusRule;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public class AcceptIT
{
    private final K3poRule k3po = new K3poRule()
        .setScriptRoot("org/kaazing/nuklei/specification/echo");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("echo")
        .setDirectory("target/nukleus-itests")
        .setCommandBufferCapacity(1024)
        .setResponseBufferCapacity(1024)
        .setCounterValuesBufferCapacity(1024);

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout).around(nukleus);

    @Before
    public void setupStreamFiles() throws Exception
    {
        int streamCapacity = 1024 * 1024;

        File source = new File("target/nukleus-itests/source/streams/echo");
        createEmptyFile(source.getAbsoluteFile(), streamCapacity + RingBufferDescriptor.TRAILER_LENGTH);
    }

    @Test
    @Specification({
        "control/bind.source/controller",
        "streams/accept/establish.connection/source"
    })
    public void shouldEstablishConnection() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "control/bind.source/controller",
        "streams/accept/echo.source.data/source" })
    public void shouldEchoSourceData() throws Exception
    {
        k3po.finish();
    }

    @Ignore("TODO: nukleus CLOSE command")
    @Test
    @Specification({
        "control/bind.source/controller",
        "streams/accept/initiate.nukleus.close/source" })
    public void shouldInitiateNukleusClose() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "control/bind.source/controller",
        "streams/accept/initiate.source.close/source" })
    public void shouldInitiateSourceClose() throws Exception
    {
        k3po.finish();
    }
}
