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

import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.util.Collections.emptyList;
import static org.agrona.CloseHelper.quietClose;
import static org.kaazing.nuklei.tcp.internal.acceptor.Route.descriptionMatches;
import static org.kaazing.nuklei.tcp.internal.acceptor.Route.sourceMatches;
import static org.kaazing.nuklei.tcp.internal.acceptor.Route.sourceRefMatches;
import static org.kaazing.nuklei.tcp.internal.acceptor.Route.targetMatches;
import static org.kaazing.nuklei.tcp.internal.acceptor.Route.targetRefMatches;
import static org.kaazing.nuklei.tcp.internal.util.IpUtil.describe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.agrona.CloseHelper;
import org.agrona.LangUtil;
import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.nio.TransportPoller;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.conductor.Conductor;
import org.kaazing.nuklei.tcp.internal.router.Router;

/**
 * The {@code Acceptor} nukleus accepts new socket connections and informs the {@code Router} nukleus.
 */
@Reaktive
public final class Acceptor extends TransportPoller implements Nukleus
{
    private static final List<Route> EMPTY_ROUTES = emptyList();

    private final Context context;

    private Conductor conductor;
    private Router router;

    public Acceptor(
        Context context)
    {
        this.context = context;
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
        for (SelectionKey key : selector.keys())
        {
            quietClose(key.channel());
        }
        super.close();
    }

    public void doRoute(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        InetSocketAddress address)
    {
        try
        {
            final SelectionKey key = findOrRegisterKey(address);
            final List<Route> routes = attachment(key);

            final String description = describe(address);
            final Predicate<SocketChannel> condition = ch -> hasLocalAddress(ch, address);
            Route newRoute = new Route(sourceName, sourceRef, targetName, targetRef, description, condition);
            routes.add(newRoute);

            conductor.onRoutedResponse(correlationId);
        }
        catch (Exception ex)
        {
            conductor.onErrorResponse(correlationId);
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public void doUnroute(
        long correlationId,
        String sourceName,
        long sourceRef,
        String targetName,
        long targetRef,
        InetSocketAddress address)
    {
        final SelectionKey key = findRegisteredKey(address);
        final List<Route> routes = attachment(key);

        final Predicate<Route> filter =
                sourceMatches(sourceName)
                 .and(sourceRefMatches(sourceRef))
                 .and(targetMatches(targetName))
                 .and(targetRefMatches(targetRef))
                 .and(descriptionMatches(describe(address)));

        if (routes.removeIf(filter))
        {
            if (routes.isEmpty())
            {
                CloseHelper.quietClose(key.channel());
                selectNowWithoutProcessing();
            }

            conductor.onUnroutedResponse(correlationId);
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

    private int processAccept(
        SelectionKey selectionKey)
    {
        try
        {
            final ServerSocketChannel serverChannel = channel(selectionKey);

            final SocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);

            final List<Route> routes = attachment(selectionKey);

            routes.stream()
                  .filter(r -> r.condition().test(channel))
                  .findFirst()
                  .ifPresent(r -> handleAccepted(r, channel));
        }
        catch (Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        return 1;
    }

    private void handleAccepted(
        final Route route,
        final SocketChannel channel)
    {
        final String sourceName = route.source();
        final String targetName = route.target();
        final long targetRef = route.targetRef();

        final AtomicCounter streamsSourced = context.counters().streamsSourced();
        final long targetId = streamsSourced.increment();
        final long correlationId = System.identityHashCode(channel);

        router.onAccepted(sourceName, targetName, targetRef, targetId, correlationId, channel);
    }

    private SelectionKey findRegisteredKey(
        InetSocketAddress localAddress)
    {
        return findSelectionKey(localAddress, a -> null);
    }

    private SelectionKey findOrRegisterKey(
        InetSocketAddress address)
    {
        return findSelectionKey(address, this::registerKey);
    }

    private SelectionKey findSelectionKey(
        InetSocketAddress localAddress,
        Function<InetSocketAddress, SelectionKey> mappingFunction)
    {
        final Optional<SelectionKey> optional =
                selector.keys()
                        .stream()
                        .filter(k -> hasLocalAddress(channel(k), localAddress))
                        .findFirst();

        return optional.orElse(mappingFunction.apply(localAddress));
    }

    private SelectionKey registerKey(
        InetSocketAddress localAddress)
    {
        try
        {
            final ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(localAddress);
            serverChannel.setOption(SO_REUSEADDR, true);
            serverChannel.configureBlocking(false);

            return serverChannel.register(selector, OP_ACCEPT, new ArrayList<Route>());
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        // unreachable
        return null;
    }

    private boolean hasLocalAddress(
        NetworkChannel channel,
        InetSocketAddress address)
    {
        try
        {
            return channel.getLocalAddress().equals(address);
        }
        catch (IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        // unreachable
        return false;
    }

    private static ServerSocketChannel channel(
        SelectionKey selectionKey)
    {
        return (ServerSocketChannel) selectionKey.channel();
    }

    @SuppressWarnings("unchecked")
    private static List<Route> attachment(
        SelectionKey key)
    {
        return key != null ? (List<Route>) key.attachment() : EMPTY_ROUTES;
    }
}
