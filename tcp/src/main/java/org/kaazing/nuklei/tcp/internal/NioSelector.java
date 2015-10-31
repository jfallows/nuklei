/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
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

package org.kaazing.nuklei.tcp.internal;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Set;
import java.util.function.IntSupplier;

import org.kaazing.nuklei.Nukleus;

import uk.co.real_logic.agrona.nio.TransportPoller;

public final class NioSelector extends TransportPoller implements Nukleus
{
    private static final int DISPATCH_READ = 0;
    private static final int DISPATCH_WRITE = 1;
    private static final int DISPATCH_CONNECT = 2;
    private static final int DISPATCH_ACCEPT = 3;

    NioSelector(Context context)
    {
    }

    public void register(
        final SelectableChannel channel,
        final int ops,
        final IntSupplier handler) throws Exception
    {
        SelectionKey key = channel.keyFor(selector);
        Dispatcher dispatcher;

        if (key == null)
        {
            dispatcher = new Dispatcher();
            key = channel.register(selector, ops, dispatcher);
        }
        else
        {
            dispatcher = (Dispatcher) key.attachment();
            key.interestOps(key.interestOps() | ops);
        }

        if ((ops & OP_CONNECT) != 0)
        {
            dispatcher.register(DISPATCH_CONNECT, handler);
        }

        if ((ops & OP_ACCEPT) != 0)
        {
            dispatcher.register(DISPATCH_ACCEPT, handler);
        }

        if ((ops & OP_READ) != 0)
        {
            dispatcher.register(DISPATCH_READ, handler);
        }

        if ((ops & OP_WRITE) != 0)
        {
            dispatcher.register(DISPATCH_WRITE, handler);
        }

    }

    public void cancel(
        final SelectableChannel channel,
        final int ops)
    {
        final SelectionKey key = channel.keyFor(selector);

        if (key != null)
        {
            final int newInterestOps = key.interestOps() & ~ops;

            if (newInterestOps == 0)
            {
                key.cancel();
            }
            else
            {
                key.interestOps(newInterestOps);
            }
        }
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        selector.selectNow();
        weight += processKeys();

        return weight;
    }

    @Override
    public String name()
    {
        return "selector";
    }

    private int processKeys()
    {
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        selectedKeys.forEach((selectedKey) ->
        {
            int readyOps = selectedKey.readyOps();
            Dispatcher dispatcher = (Dispatcher) selectedKey.attachment();

            if ((readyOps & OP_ACCEPT) != 0)
            {
                dispatcher.dispatch(DISPATCH_ACCEPT);
            }

            if ((readyOps & OP_CONNECT) != 0)
            {
                dispatcher.dispatch(DISPATCH_CONNECT);
            }

            if ((readyOps & OP_READ) != 0)
            {
                dispatcher.dispatch(DISPATCH_READ);
            }

            if ((readyOps & OP_WRITE) != 0)
            {
                dispatcher.dispatch(DISPATCH_WRITE);
            }
        });

        return selectedKeys.size();
    }

    private static class Dispatcher
    {
        private IntSupplier[] handlers = new IntSupplier[4];

        public void register(final int index, final IntSupplier handler)
        {
            handlers[index] = handler;
        }

        public int dispatch(final int index)
        {
            return handlers[index].getAsInt();
        }
    }
}
