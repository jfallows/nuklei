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
package org.kaazing.nuklei.tcp.internal.reader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.IntSupplier;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.reader.stream.StreamFactory;
import org.kaazing.nuklei.tcp.internal.router.Router;

import uk.co.real_logic.agrona.CloseHelper;
import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.nio.TransportPoller;

@Reaktive
public final class Source extends TransportPoller implements Nukleus
{
    private final StreamFactory streamFactory;

    public Source(
        Router router,
        int bufferSize)
    {
        this.streamFactory = new StreamFactory(bufferSize);
    }

    @Override
    public String name()
    {
        return "source";
    }

    @Override
    public int process()
    {
        int weight = 0;

        try
        {
            selector.selectNow();
            weight += selectedKeySet.forEach(this::processKey);
        }
        catch (Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        return weight;
    }

    public void doBegin(
        Target target,
        long targetRef,
        long targetId,
        long replyRef,
        long replyId,
        SocketChannel channel)
    {
        try
        {
            final InetSocketAddress localAddress = (InetSocketAddress) channel.getLocalAddress();
            final InetSocketAddress remoteAddress = (InetSocketAddress) channel.getRemoteAddress();

            target.doTcpBegin(targetRef, targetId, replyRef, replyId, localAddress, remoteAddress);

            final SelectionKey key = channel.register(selector, 0);
            final IntSupplier attachment = streamFactory.newStream(target, targetId, key, channel);

            key.attach(attachment);
        }
        catch (IOException ex)
        {
            CloseHelper.quietClose(channel);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    private int processKey(
        SelectionKey selectionKey)
    {
        final IntSupplier attachment = (IntSupplier) selectionKey.attachment();
        return attachment.getAsInt();
    }
}
