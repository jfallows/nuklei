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
package org.kaazing.nuklei.amqp_1_0.link;

import java.util.function.Consumer;

import org.kaazing.nuklei.amqp_1_0.codec.transport.Attach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Detach;
import org.kaazing.nuklei.amqp_1_0.codec.transport.Transfer;
import org.kaazing.nuklei.amqp_1_0.function.FrameConsumer;

/*
 * See AMQP 1.0 specification, section 2.6 "Links"
 */
public class LinkHooks<L>
{

    public Consumer<Link<L>> whenInitialized = (s) ->
    {
    };
    public Consumer<Link<L>> whenError = (s) ->
    {
    };

    public FrameConsumer<Link<L>, Attach> whenAttachReceived = (s, f, m) ->
    {
    };
    public FrameConsumer<Link<L>, Attach> whenAttachSent = (s, f, m) ->
    {
    };
    public FrameConsumer<Link<L>, Transfer> whenTransferReceived = (s, f, m) ->
    {
    };
    public FrameConsumer<Link<L>, Transfer> whenTransferSent = (s, f, m) ->
    {
    };
    public FrameConsumer<Link<L>, Detach> whenDetachReceived = (s, f, m) ->
    {
    };
    public FrameConsumer<Link<L>, Detach> whenDetachSent = (s, f, m) ->
    {
    };

}