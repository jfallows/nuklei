/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
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
package org.kaazing.nuklei.amqp_1_0.session;

import java.util.function.Consumer;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Begin;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Disposition;
import org.kaazing.nuklei.amqp_1_0.codec.transport.End;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Flow;
import org.kaazing.nuklei.amqp_1_0.function.FrameConsumer;

public class SessionHooks<S, L>
{

    public Consumer<Session<S, L>> whenInitialized = (s) ->
    {
    };
    public Consumer<Session<S, L>> whenError = (s) ->
    {
    };

    public FrameConsumer<Session<S, L>, Begin> whenBeginReceived = (s, f, m) ->
    {
    };
    public FrameConsumer<Session<S, L>, Begin> whenBeginSent = (s, f, m) ->
    {
    };
    public FrameConsumer<Session<S, L>, Flow> whenFlowReceived = (s, f, m) ->
    {
    };
    public FrameConsumer<Session<S, L>, Flow> whenFlowSent = (s, f, m) ->
    {
    };
    public FrameConsumer<Session<S, L>, Disposition> whenDispositionReceived = (s, f, m) ->
    {
    };
    public FrameConsumer<Session<S, L>, Disposition> whenDispositionSent = (s, f, m) ->
    {
    };
    public FrameConsumer<Session<S, L>, End> whenEndReceived = (s, f, m) ->
    {
    };
    public FrameConsumer<Session<S, L>, End> whenEndSent = (s, f, m) ->
    {
    };
    public FrameConsumer<Session<S, L>, End> whenEndSentWithError = (s, f, m) ->
    {
    };

}