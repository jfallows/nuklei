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
package org.kaazing.nuklei.http.internal.conductor;

import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_BIND_COMMAND;
import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_CAPTURE_COMMAND;
import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_PREPARE_COMMAND;
import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_ROUTE_COMMAND;
import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_UNBIND_COMMAND;
import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_UNCAPTURE_COMMAND;
import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_UNPREPARE_COMMAND;
import static org.kaazing.nuklei.http.internal.types.control.Types.TYPE_ID_UNROUTE_COMMAND;

import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.http.internal.Context;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.broadcast.BroadcastTransmitter;
import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBuffer;

public final class Conductor implements Nukleus, Consumer<ConductorResponse>
{
    private static final int SEND_BUFFER_CAPACITY = 1024; // TODO: Configuration and Context

    private final OneToOneConcurrentArrayQueue<ConductorResponse> translatorResponses;

    private final RingBuffer conductorCommands;
    private final BroadcastTransmitter conductorResponses;
    private final AtomicBuffer sendBuffer;

    public Conductor(Context context)
    {
        this.translatorResponses = context.translatorResponseQueue();
        this.conductorCommands = context.conductorCommands();
        this.conductorResponses = context.conductorResponses();
        this.sendBuffer = new UnsafeBuffer(new byte[SEND_BUFFER_CAPACITY]);
    }

    @Override
    public int process() throws Exception
    {
        int weight = 0;

        weight += conductorCommands.read(this::handleCommand);
        weight += translatorResponses.drain(this);

        return weight;
    }

    @Override
    public String name()
    {
        return "conductor";
    }

    @Override
    public void accept(ConductorResponse response)
    {
        response.execute(this);
    }

    public void onErrorResponse(long correlationId)
    {
        // TODO
    }

    public void onCapturedResponse(long correlationId)
    {
        // TODO
    }

    public void onUncapturedResponse(long correlationId)
    {
        // TODO
    }

    public void onRoutedResponse(long correlationId)
    {
        // TODO
    }

    public void onUnroutedResponse(long correlationId)
    {
        // TODO
    }

    public void onBoundResponse(
        long correlationId,
        long referenceId)
    {
        // TODO
    }

    public void onUnboundResponse(
        long correlationId,
        String source,
        long sourceRef)
    {
        // TODO
    }

    public void onPreparedResponse(
        long correlationId,
        long referenceId)
    {
        // TODO
    }

    public void onUnpreparedResponse(
        long correlationId,
        String destination,
        long destinationRef)
    {
        // TODO
    }

    private void handleCommand(int msgTypeId, DirectBuffer buffer, int index, int length)
    {
        switch (msgTypeId)
        {
        case TYPE_ID_CAPTURE_COMMAND:
            handleCaptureCommand(buffer, index, length);
            break;
        case TYPE_ID_UNCAPTURE_COMMAND:
            handleUncaptureCommand(buffer, index, length);
            break;
        case TYPE_ID_ROUTE_COMMAND:
            handleRouteCommand(buffer, index, length);
            break;
        case TYPE_ID_UNROUTE_COMMAND:
            handleUnrouteCommand(buffer, index, length);
            break;
        case TYPE_ID_BIND_COMMAND:
            handleBindCommand(buffer, index, length);
            break;
        case TYPE_ID_UNBIND_COMMAND:
            handleUnbindCommand(buffer, index, length);
            break;
        case TYPE_ID_PREPARE_COMMAND:
            handlePrepareCommand(buffer, index, length);
            break;
        case TYPE_ID_UNPREPARE_COMMAND:
            handleUnprepareCommand(buffer, index, length);
            break;
        default:
            // ignore unrecognized commands (forwards compatible)
            break;
        }
    }

    private void handleCaptureCommand(DirectBuffer buffer, int index, int length)
    {
        // TODO
    }

    private void handleUncaptureCommand(DirectBuffer buffer, int index, int length)
    {
        // TODO
    }

    private void handleRouteCommand(DirectBuffer buffer, int index, int length)
    {
        // TODO
    }

    private void handleUnrouteCommand(DirectBuffer buffer, int index, int length)
    {
        // TODO
    }

    private void handleBindCommand(DirectBuffer buffer, int index, int length)
    {
        // TODO
    }

    private void handleUnbindCommand(DirectBuffer buffer, int index, int length)
    {
        // TODO
    }

    private void handlePrepareCommand(DirectBuffer buffer, int index, int length)
    {
        // TODO
    }

    private void handleUnprepareCommand(DirectBuffer buffer, int index, int length)
    {
        // TODO
    }
}
