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
package org.kaazing.nuklei.ws.internal.streams;

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

/**
 * RFC-6455, section 5.2 "Base Framing Protocol"
 */
public class BaseFramingIT
{
    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/nuklei/ws");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("ws")
        .directory("target/nukleus-itests")
        .commandBufferCapacity(1024)
        .responseBufferCapacity(1024)
        .counterValuesBufferCapacity(1024)
        .initialize("source", "ws")
        .initialize("destination", "ws");

    @Rule
    public final TestRule chain = outerRule(nukleus).around(k3po).around(timeout);

    @Test
    @Specification({
        "control/capture.source.destination/controller",
        "control/route.source.destination/controller",
        "control/bind.source.destination/controller",
        "streams/framing/echo.binary.payload.length.0/source.accept",
        "streams/framing/echo.binary.payload.length.0/destination.accept" })
    public void shouldEchoBinaryFrameWithPayloadLength0() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.binary.payload.length.125/handshake.request.and.frame",
        "streams/framing/echo.binary.payload.length.125/handshake.response.and.frame" })
    public void shouldEchoBinaryFrameWithPayloadLength125() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.binary.payload.length.126/handshake.request.and.frame",
        "streams/framing/echo.binary.payload.length.126/handshake.response.and.frame" })
    public void shouldEchoBinaryFrameWithPayloadLength126() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.binary.payload.length.127/handshake.request.and.frame",
        "streams/framing/echo.binary.payload.length.127/handshake.response.and.frame" })
    public void shouldEchoBinaryFrameWithPayloadLength127() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.binary.payload.length.128/handshake.request.and.frame",
        "streams/framing/echo.binary.payload.length.128/handshake.response.and.frame" })
    public void shouldEchoBinaryFrameWithPayloadLength128() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.binary.payload.length.65535/handshake.request.and.frame",
        "streams/framing/echo.binary.payload.length.65535/handshake.response.and.frame" })
    public void shouldEchoBinaryFrameWithPayloadLength65535() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.binary.payload.length.65536/handshake.request.and.frame",
        "streams/framing/echo.binary.payload.length.65536/handshake.response.and.frame" })
    public void shouldEchoBinaryFrameWithPayloadLength65536() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.text.payload.length.0/handshake.request.and.frame",
        "streams/framing/echo.text.payload.length.0/handshake.response.and.frame" })
    public void shouldEchoTextFrameWithPayloadLength0() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.text.payload.length.125/handshake.request.and.frame",
        "streams/framing/echo.text.payload.length.125/handshake.response.and.frame" })
    public void shouldEchoTextFrameWithPayloadLength125() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.text.payload.length.126/handshake.request.and.frame",
        "streams/framing/echo.text.payload.length.126/handshake.response.and.frame" })
    public void shouldEchoTextFrameWithPayloadLength126() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.text.payload.length.127/handshake.request.and.frame",
        "streams/framing/echo.text.payload.length.127/handshake.response.and.frame" })
    public void shouldEchoTextFrameWithPayloadLength127() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.text.payload.length.128/handshake.request.and.frame",
        "streams/framing/echo.text.payload.length.128/handshake.response.and.frame" })
    public void shouldEchoTextFrameWithPayloadLength128() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.text.payload.length.65535/handshake.request.and.frame",
        "streams/framing/echo.text.payload.length.65535/handshake.response.and.frame" })
    public void shouldEchoTextFrameWithPayloadLength65535() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "streams/framing/echo.text.payload.length.65536/handshake.request.and.frame",
        "streams/framing/echo.text.payload.length.65536/handshake.response.and.frame" })
    public void shouldEchoTextFrameWithPayloadLength65536() throws Exception
    {
        k3po.finish();
    }
}
