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
package org.kaazing.nuklei.tcp.internal.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.router.Router;

import org.agrona.LangUtil;
import org.agrona.nio.TransportPoller;

/**
 * The {@code Connector} nukleus accepts new socket connections and informs the {@code Router} nukleus.
 */
@Reaktive
public final class Connector extends TransportPoller implements Nukleus
{
    private Router router;

    public void setRouter(
        Router router)
    {
        this.router = router;
    }

    @Override
    public int process()
    {
        selectNow();
        return selectedKeySet.forEach(this::processConnect);
    }

    @Override
    public String name()
    {
        return "connector";
    }

    public void doConnect(
        String sourceName,
        long sourceRef,
        long sourceId,
        String targetName,
        long targetRef,
        String replyName,
        long replyRef,
        long replyId,
        SocketChannel channel,
        InetSocketAddress address,
        Runnable onsuccess,
        Runnable onfailure)
    {
        try
        {
            final long targetId = System.identityHashCode(channel);

            final Request request = new Request(sourceName, sourceRef, sourceId,
                    targetName, targetRef, targetId,
                    replyName, replyRef, replyId,
                    channel, onsuccess, onfailure);

            if (channel.connect(address))
            {
                processConnectFinished(request);
            }
            else
            {
                channel.register(selector, SelectionKey.OP_CONNECT, request);
            }
        }
        catch (IOException ex)
        {
            onfailure.run();
            LangUtil.rethrowUnchecked(ex);
        }
    }

    private void selectNow()
    {
        try
        {
            selector.selectNow();
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    private int processConnect(
        SelectionKey selectionKey)
    {
        final Request request = (Request) selectionKey.attachment();

        try
        {
            final SocketChannel channel = request.channel();

            channel.finishConnect();

            processConnectFinished(request);
        }
        catch (Exception ex)
        {
            request.fail();
            LangUtil.rethrowUnchecked(ex);
        }

        return 1;
    }

    private void processConnectFinished(
        Request request)
    {
        final String sourceName = request.source();
        final long sourceRef = request.sourceRef();
        final long sourceId = request.sourceId();

        final String targetName = request.target();

        final String replyName = request.reply();
        final long replyRef = request.replyRef();
        final long replyId = request.replyId();

        final SocketChannel channel = request.channel();

        request.succeed();

        router.onConnected(targetName, sourceName, sourceRef, sourceId, replyName, replyRef, replyId, channel);
    }
}
