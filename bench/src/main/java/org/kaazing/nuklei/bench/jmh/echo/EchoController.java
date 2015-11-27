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
package org.kaazing.nuklei.bench.jmh.echo;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_BOUND_RESPONSE;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_CAPTURED_RESPONSE;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_CONNECTED_RESPONSE;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_ERROR_RESPONSE;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_PREPARED_RESPONSE;
import static org.kaazing.nuklei.echo.internal.types.control.Types.TYPE_ID_ROUTED_RESPONSE;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.kaazing.nuklei.echo.internal.Context;
import org.kaazing.nuklei.echo.internal.types.control.BindFW;
import org.kaazing.nuklei.echo.internal.types.control.BoundFW;
import org.kaazing.nuklei.echo.internal.types.control.CaptureFW;
import org.kaazing.nuklei.echo.internal.types.control.CapturedFW;
import org.kaazing.nuklei.echo.internal.types.control.ConnectFW;
import org.kaazing.nuklei.echo.internal.types.control.ConnectedFW;
import org.kaazing.nuklei.echo.internal.types.control.ErrorFW;
import org.kaazing.nuklei.echo.internal.types.control.PrepareFW;
import org.kaazing.nuklei.echo.internal.types.control.PreparedFW;
import org.kaazing.nuklei.echo.internal.types.control.RouteFW;
import org.kaazing.nuklei.echo.internal.types.control.RoutedFW;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastReceiver;
import uk.co.real_logic.agrona.concurrent.broadcast.CopyBroadcastReceiver;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class EchoController
{
    private static final int MAX_SEND_LENGTH = 1024; // TODO: Configuration and Context

    private final CaptureFW.Builder captureRW = new CaptureFW.Builder();
    private final RouteFW.Builder routeRW = new RouteFW.Builder();
    private final BindFW.Builder bindRW = new BindFW.Builder();
    private final PrepareFW.Builder prepareRW = new PrepareFW.Builder();
    private final ConnectFW.Builder connectRW = new ConnectFW.Builder();

    private final CapturedFW capturedRO = new CapturedFW();
    private final RoutedFW routedRO = new RoutedFW();
    private final BoundFW boundRO = new BoundFW();
    private final PreparedFW preparedRO = new PreparedFW();
    private final ConnectedFW connectedRO = new ConnectedFW();
    private final ErrorFW errorRO = new ErrorFW();

    private final RingBuffer conductorCommands;
    private final CopyBroadcastReceiver conductorResponsesCopy;
    private final AtomicBuffer atomicBuffer;

    EchoController(Context context)
    {
        this.conductorCommands = context.conductorCommands();
        this.conductorResponsesCopy = new CopyBroadcastReceiver(new BroadcastReceiver(context.conductorResponseBuffer()));
        this.atomicBuffer = new UnsafeBuffer(allocateDirect(MAX_SEND_LENGTH).order(nativeOrder()));
    }

    public long capture(
        String source)
    {
        long correlationId = conductorCommands.nextCorrelationId();

        CaptureFW captureRO = captureRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                       .correlationId(correlationId)
                                       .source(source)
                                       .build();

        if (!conductorCommands.write(captureRO.typeId(), captureRO.buffer(), captureRO.offset(), captureRO.length()))
        {
            throw new IllegalStateException("unable to offer command");
        }

        return correlationId;
    }

    public long route(
            String destination)
    {
        long correlationId = conductorCommands.nextCorrelationId();

        RouteFW routeRO = routeRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                                 .correlationId(correlationId)
                                 .destination(destination)
                                 .build();

        if (!conductorCommands.write(routeRO.typeId(), routeRO.buffer(), routeRO.offset(), routeRO.length()))
        {
            throw new IllegalStateException("unable to offer command");
        }

        return correlationId;
    }

    public long bind(
        String source,
        long sourceRef)
    {
        long correlationId = conductorCommands.nextCorrelationId();

        bindRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
              .correlationId(correlationId)
              .binding()
                  .source(source)
                  .sourceRef(sourceRef);
        BindFW bindRO = bindRW.build();

        if (!conductorCommands.write(bindRO.typeId(), bindRO.buffer(), bindRO.offset(), bindRO.length()))
        {
            throw new IllegalStateException("unable to offer command");
        }

        return correlationId;
    }

    public long prepare(
        String destination,
        long destinationRef)
    {
        long correlationId = conductorCommands.nextCorrelationId();

        prepareRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                 .correlationId(correlationId)
                 .preparation()
                     .destination(destination)
                     .destinationRef(destinationRef);
        PrepareFW prepareRO = prepareRW.build();

        if (!conductorCommands.write(prepareRO.typeId(), prepareRO.buffer(), prepareRO.offset(), prepareRO.length()))
        {
            throw new IllegalStateException("unable to offer command");
        }

        return correlationId;
    }

    public long connect(
        long referenceId)
    {
        long correlationId = conductorCommands.nextCorrelationId();

        connectRW.wrap(atomicBuffer, 0, atomicBuffer.capacity())
                 .correlationId(correlationId)
                 .referenceId(referenceId);
        ConnectFW connectRO = connectRW.build();

        if (!conductorCommands.write(connectRO.typeId(), connectRO.buffer(), connectRO.offset(), connectRO.length()))
        {
            throw new IllegalStateException("unable to offer command");
        }

        return correlationId;
    }

    public long supplyResponse(
        final long correlationId)
    {
        AtomicLong result = new AtomicLong();
        for(final AtomicBoolean complete = new AtomicBoolean(); !complete.get();)
        {
            final MessageHandler handler = (msgTypeId, buffer, index, length) -> {
                switch (msgTypeId)
                {
                case TYPE_ID_CAPTURED_RESPONSE:
                    capturedRO.wrap(buffer, index, length);
                    if (capturedRO.correlationId() == correlationId)
                    {
                        result.set(0L);
                        complete.set(true);
                    }
                    break;
                case TYPE_ID_ROUTED_RESPONSE:
                    routedRO.wrap(buffer, index, length);
                    if (routedRO.correlationId() == correlationId)
                    {
                        result.set(0L);
                        complete.set(true);
                    }
                    break;
                case TYPE_ID_BOUND_RESPONSE:
                    boundRO.wrap(buffer, index, length);
                    if (boundRO.correlationId() == correlationId)
                    {
                        result.set(boundRO.referenceId());
                        complete.set(true);
                    }
                    break;
                case TYPE_ID_PREPARED_RESPONSE:
                    preparedRO.wrap(buffer, index, length);
                    if (preparedRO.correlationId() == correlationId)
                    {
                        result.set(preparedRO.referenceId());
                        complete.set(true);
                    }
                    break;
                case TYPE_ID_CONNECTED_RESPONSE:
                    connectedRO.wrap(buffer, index, length);
                    if (connectedRO.correlationId() == correlationId)
                    {
                        result.set(connectedRO.connectionId());
                        complete.set(true);
                    }
                    break;
                case TYPE_ID_ERROR_RESPONSE:
                    errorRO.wrap(buffer, index, length);
                    if (errorRO.correlationId() == correlationId)
                    {
                        throw new RuntimeException("command failed");
                    }
                }
            };

            while (conductorResponsesCopy.receive(handler) == 0)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException ex)
                {
                    LangUtil.rethrowUnchecked(ex);
                }
            }
        }

        return result.get();
    }
}
