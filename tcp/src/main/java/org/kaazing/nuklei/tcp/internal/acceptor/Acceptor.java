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
package org.kaazing.nuklei.tcp.internal.acceptor;

import static java.nio.channels.SelectionKey.OP_ACCEPT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.conductor.Conductor;
import org.kaazing.nuklei.tcp.internal.router.Router;

import org.agrona.LangUtil;
import org.agrona.nio.TransportPoller;

/**
 * The {@code Acceptor} nukleus accepts new socket connections and informs the {@code Router} nukleus.
 */
@Reaktive
public final class Acceptor extends TransportPoller implements Nukleus
{
    private final Map<InetSocketAddress, Route> routesByAddress;

    private Conductor conductor;
    private Router router;

    public Acceptor()
    {
        this.routesByAddress = new HashMap<>();
    }

    public void setConductor(
        Conductor conductor)
    {
        this.conductor = conductor;
    }

    public void setRouter(
        Router router)
    {
        this.router = router;
    }

    @Override
    public int process()
    {
        selectNow();
        return selectedKeySet.forEach(this::processAccept);
    }

    @Override
    public String name()
    {
        return "acceptor";
    }

    @Override
    public void close()
    {
        routesByAddress.values().forEach(this::unroute);
        super.close();
    }

    public void doRoute(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        try
        {
            final ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(address);
            serverChannel.configureBlocking(false);

            Route newRoute = new Route(sourceName, sourceRef, targetName, targetRef, replyName, address);
            serverChannel.register(selector, OP_ACCEPT, newRoute);
            newRoute.attach(serverChannel);

            routesByAddress.put(address, newRoute);

            conductor.onRoutedResponse(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
            conductor.onErrorResponse(correlationId);
        }
    }

    public void doUnroute(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        String replyName,
        InetSocketAddress address)
    {
        final Route route = routesByAddress.get(address);

        Route candidate = new Route(sourceName, sourceRef, targetName, targetRef, replyName, address);
        if (route != null && route.equals(candidate))
        {
            routesByAddress.remove(address);
            unroute(route);
            conductor.onUnroutedResponse(correlationId, sourceName, sourceRef, targetName, targetRef, replyName, address);
        }
        else
        {
            conductor.onErrorResponse(correlationId);
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

    private void unroute(
        Route route)
    {
        try
        {
            route.channel().close();
            selectNowWithoutProcessing();
        }
        catch (final Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    private int processAccept(
        SelectionKey selectionKey)
    {
        try
        {
            final Route route = (Route) selectionKey.attachment();
            final String sourceName = route.source();
            final long sourceRef = route.sourceRef();
            final String targetName = route.target();
            final long targetRef = route.targetRef();
            final String replyName = route.reply();
            final ServerSocketChannel serverChannel = route.channel();

            final SocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);

            final long targetId = System.identityHashCode(channel);

            final long replyRef = -sourceRef;
            final long replyId = System.identityHashCode(channel);

            router.onAccepted(sourceName, targetName, targetRef, targetId, replyName, replyRef, replyId, channel);
        }
        catch (Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        return 1;
    }
}
