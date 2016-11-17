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
package org.kaazing.nuklei.tcp.internal;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static org.kaazing.nuklei.tcp.internal.util.IpUtil.ipAddress;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.broadcast.BroadcastReceiver;
import org.agrona.concurrent.broadcast.CopyBroadcastReceiver;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.kaazing.nuklei.Controller;
import org.kaazing.nuklei.tcp.internal.types.Flyweight;
import org.kaazing.nuklei.tcp.internal.types.control.BindFW;
import org.kaazing.nuklei.tcp.internal.types.control.BoundFW;
import org.kaazing.nuklei.tcp.internal.types.control.ErrorFW;
import org.kaazing.nuklei.tcp.internal.types.control.RouteFW;
import org.kaazing.nuklei.tcp.internal.types.control.RoutedFW;
import org.kaazing.nuklei.tcp.internal.types.control.TcpRouteExFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnbindFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnboundFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnrouteFW;
import org.kaazing.nuklei.tcp.internal.types.control.UnroutedFW;


public final class TcpController implements Controller
{
    private static final int MAX_SEND_LENGTH = 1024; // TODO: Configuration and Context

    // TODO: thread-safe flyweights or command queue from public methods
    private final BindFW.Builder bindRW = new BindFW.Builder();
    private final UnbindFW.Builder unbindRW = new UnbindFW.Builder();
    private final RouteFW.Builder routeRW = new RouteFW.Builder();
    private final UnrouteFW.Builder unrouteRW = new UnrouteFW.Builder();

    private final TcpRouteExFW.Builder routeExRW = new TcpRouteExFW.Builder();

    private final ErrorFW errorRO = new ErrorFW();
    private final BoundFW boundRO = new BoundFW();
    private final UnboundFW unboundRO = new UnboundFW();
    private final RoutedFW routedRO = new RoutedFW();
    private final UnroutedFW unroutedRO = new UnroutedFW();

    private final Context context;
    private final RingBuffer conductorCommands;
    private final CopyBroadcastReceiver conductorResponses;
    private final AtomicBuffer atomicBuffer;
    private final Long2ObjectHashMap<CompletableFuture<?>> promisesByCorrelationId;

    public TcpController(Context context)
    {
        this.context = context;
        this.conductorCommands = context.conductorCommands();
        this.conductorResponses = new CopyBroadcastReceiver(new BroadcastReceiver(context.conductorResponseBuffer()));
        this.atomicBuffer = new UnsafeBuffer(allocateDirect(MAX_SEND_LENGTH).order(nativeOrder()));
        this.promisesByCorrelationId = new Long2ObjectHashMap<>();
    }

    @Override
    public int process()
    {
        int weight = 0;

        weight += conductorResponses.receive(this::handleResponse);

        return weight;
    }

    @Override
    public void close() throws Exception
    {
        context.close();
    }

    @Override
    public Class<TcpController> kind()
    {
        return TcpController.class;
    }

    @Override
    public String name()
    {
        return "tcp";
    }

    public CompletableFuture<Long> bind(
        final int kind)
    {
        final CompletableFuture<Long> promise = new CompletableFuture<>();
        final long correlationId = conductorCommands.nextCorrelationId();

        BindFW bindRO = bindRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                              .correlationId(correlationId)
                              .kind((byte)kind)
                              .build();

        if (!conductorCommands.write(bindRO.typeId(), bindRO.buffer(), bindRO.offset(), bindRO.length()))
        {
            commandSendFailed(promise);
        }
        else
        {
            commandSent(correlationId, promise);
        }

        return promise;
    }

    public CompletableFuture<Void> unbind(
        final long referenceId)
    {
        final CompletableFuture<Void> promise = new CompletableFuture<>();
        final long correlationId = conductorCommands.nextCorrelationId();

        UnbindFW unbindRO = unbindRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                    .correlationId(correlationId)
                                    .referenceId(referenceId)
                                    .build();

        if (!conductorCommands.write(unbindRO.typeId(), unbindRO.buffer(), unbindRO.offset(), unbindRO.length()))
        {
            commandSendFailed(promise);
        }
        else
        {
            commandSent(correlationId, promise);
        }

        return promise;
    }

    public CompletableFuture<Void> route(
        String source,
        long sourceRef,
        String target,
        long targetRef,
        InetSocketAddress address)
    {
        final CompletableFuture<Void> promise = new CompletableFuture<>();

        long correlationId = conductorCommands.nextCorrelationId();

        RouteFW routeRO = routeRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .correlationId(correlationId)
                                 .source(source)
                                 .sourceRef(sourceRef)
                                 .target(target)
                                 .targetRef(targetRef)
                                 .extension(b -> b.set(visitRouteEx(address)))
                                 .build();

        if (!conductorCommands.write(routeRO.typeId(), routeRO.buffer(), routeRO.offset(), routeRO.length()))
        {
            commandSendFailed(promise);
        }
        else
        {
            commandSent(correlationId, promise);
        }

        return promise;
    }

    public CompletableFuture<Void> unroute(
        String source,
        long sourceRef,
        String target,
        long targetRef,
        InetSocketAddress address)
    {
        final CompletableFuture<Void> promise = new CompletableFuture<>();

        long correlationId = conductorCommands.nextCorrelationId();

        UnrouteFW unrouteRO = unrouteRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .correlationId(correlationId)
                                 .source(source)
                                 .sourceRef(sourceRef)
                                 .target(target)
                                 .targetRef(targetRef)
                                 .extension(b -> b.set(visitRouteEx(address)))
                                 .build();

        if (!conductorCommands.write(unrouteRO.typeId(), unrouteRO.buffer(), unrouteRO.offset(), unrouteRO.length()))
        {
            commandSendFailed(promise);
        }
        else
        {
            commandSent(correlationId, promise);
        }

        return promise;
    }

    public TcpReadableStreams streams(
        String source,
        String target)
    {
        return new TcpReadableStreams(context, source, target);
    }

    private Flyweight.Builder.Visitor visitRouteEx(
        InetSocketAddress address)
    {
        return (buffer, offset, limit) ->
            routeExRW.wrap(buffer, offset, limit)
                     .address(a -> ipAddress(address, a::ipv4Address, a::ipv6Address))
                     .port(address.getPort())
                     .build()
                     .length();
    }

    private int handleResponse(
        int msgTypeId,
        DirectBuffer buffer,
        int index,
        int length)
    {
        switch (msgTypeId)
        {
        case ErrorFW.TYPE_ID:
            handleErrorResponse(buffer, index, length);
            break;
        case BoundFW.TYPE_ID:
            handleBoundResponse(buffer, index, length);
            break;
        case UnboundFW.TYPE_ID:
            handleUnboundResponse(buffer, index, length);
            break;
        case RoutedFW.TYPE_ID:
            handleRoutedResponse(buffer, index, length);
            break;
        case UnroutedFW.TYPE_ID:
            handleUnroutedResponse(buffer, index, length);
            break;
        default:
            break;
        }

        return 1;
    }

    private void handleErrorResponse(
        DirectBuffer buffer,
        int index,
        int length)
    {
        errorRO.wrap(buffer, index, length);
        long correlationId = errorRO.correlationId();

        CompletableFuture<?> promise = promisesByCorrelationId.remove(correlationId);
        if (promise != null)
        {
            commandFailed(promise, "command failed");
        }
    }

    private void handleRoutedResponse(
        DirectBuffer buffer,
        int index,
        int length)
    {
        routedRO.wrap(buffer, index, length);
        long correlationId = routedRO.correlationId();

        CompletableFuture<?> promise = promisesByCorrelationId.remove(correlationId);
        if (promise != null)
        {
            commandSucceeded(promise);
        }
    }

    private void handleUnroutedResponse(
        DirectBuffer buffer,
        int index,
        int length)
    {
        unroutedRO.wrap(buffer, index, length);
        long correlationId = unroutedRO.correlationId();

        CompletableFuture<?> promise = promisesByCorrelationId.remove(correlationId);
        if (promise != null)
        {
            commandSucceeded(promise);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleBoundResponse(
        DirectBuffer buffer,
        int index,
        int length)
    {
        boundRO.wrap(buffer, index, length);
        long correlationId = boundRO.correlationId();

        CompletableFuture<Long> promise = (CompletableFuture<Long>)promisesByCorrelationId.remove(correlationId);
        if (promise != null)
        {
            commandSucceeded(promise, boundRO.referenceId());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleUnboundResponse(
        DirectBuffer buffer,
        int index,
        int length)
    {
        unboundRO.wrap(buffer, index, length);
        long correlationId = unboundRO.correlationId();

        CompletableFuture<Void> promise = (CompletableFuture<Void>)promisesByCorrelationId.remove(correlationId);
        if (promise != null)
        {
            commandSucceeded(promise);
        }
    }

    private void commandSent(
        final long correlationId,
        final CompletableFuture<?> promise)
    {
        promisesByCorrelationId.put(correlationId, promise);
    }

    private <T> boolean commandSucceeded(
        final CompletableFuture<T> promise)
    {
        return commandSucceeded(promise, null);
    }

    private <T> boolean commandSucceeded(
        final CompletableFuture<T> promise,
        final T value)
    {
        return promise.complete(value);
    }

    private boolean commandSendFailed(
        final CompletableFuture<?> promise)
    {
        return commandFailed(promise, "unable to offer command");
    }

    private boolean commandFailed(
        final CompletableFuture<?> promise,
        final String message)
    {
        return promise.completeExceptionally(new IllegalStateException(message).fillInStackTrace());
    }
}
