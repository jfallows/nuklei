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
package org.kaazing.nuklei.http.internal.routable.stream;

import static java.lang.Character.toUpperCase;
import static java.nio.charset.StandardCharsets.US_ASCII;

import org.kaazing.nuklei.http.internal.routable.Source;
import org.kaazing.nuklei.http.internal.routable.Target;
import org.kaazing.nuklei.http.internal.types.OctetsFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpEndFW;
import org.kaazing.nuklei.http.internal.types.stream.ResetFW;
import org.kaazing.nuklei.http.internal.types.stream.WindowFW;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class ReplyEncodingStreamFactory
{
    private final HttpBeginFW httpBeginRO = new HttpBeginFW();
    private final HttpDataFW httpDataRO = new HttpDataFW();
    private final HttpEndFW httpEndRO = new HttpEndFW();

    private final WindowFW windowRO = new WindowFW();
    private final ResetFW resetRO = new ResetFW();

    private final Source source;

    public ReplyEncodingStreamFactory(
        Source source)
    {
        this.source = source;
    }

    public MessageHandler newStream(
        Target target,
        long targetRef,
        long targetId,
        long replyRef,
        long replyId)
    {
        return new ReplyEncodingStream(target, targetRef, targetId, replyRef, replyId)::handleStream;
    }

    private final class ReplyEncodingStream
    {
        private final Target target;
        private final long targetId;
        private final long targetRef;
        private final long replyRef;
        private final long replyId;

        private long httpSourceId;

        private ReplyEncodingStream(
            final Target target,
            final long targetRef,
            final long targetId,
            final long replyRef,
            final long replyId)
        {
            this.target = target;
            this.targetRef = targetRef;
            this.targetId = targetId;
            this.replyRef = replyRef;
            this.replyId = replyId;
        }

        private void handleStream(
            int msgTypeId,
            DirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case HttpBeginFW.TYPE_ID:
                onHttpBegin(buffer, index, length);
                break;
            case HttpDataFW.TYPE_ID:
                onHttpData(buffer, index, length);
                break;
            case HttpEndFW.TYPE_ID:
                onHttpEnd(buffer, index, length);
                break;
            default:
                // ignore
                break;
            }
        }

        private void onHttpBegin(
            DirectBuffer buffer,
            int index,
            int length)
        {
            httpBeginRO.wrap(buffer, index, index + length);

            // TODO: replace with connection pool (start)
            target.doBegin(targetRef, targetId, replyRef, replyId);
            // TODO: replace with connection pool (end)

            final long streamId = httpBeginRO.streamId();

            // default status (and reason)
            String[] status = new String[] { "200", "OK" };

            StringBuilder headers = new StringBuilder();
            httpBeginRO.headers().forEach(header ->
            {
                String name = header.name().asString();
                String value = header.value().asString();

                if (":status".equals(name))
                {
                    status[0] = value;
                    if ("101".equals(status[0]))
                    {
                        status[1] = "Switching Protocols";
                    }
                }
                else
                {
                    headers.append(toUpperCase(name.charAt(0))).append(name.substring(1))
                           .append(": ").append(value).append("\r\n");
                }
            });

            String payloadChars =
                    new StringBuilder().append("HTTP/1.1 ").append(status[0]).append(" ").append(status[1]).append("\r\n")
                                       .append(headers).append("\r\n").toString();

            final DirectBuffer payload = new UnsafeBuffer(payloadChars.getBytes(US_ASCII));

            target.doData(this.targetId, payload, 0, payload.capacity());
            target.addThrottle(targetId, this::handleThrottle);

            this.httpSourceId = streamId;
        }

        private void onHttpData(
            DirectBuffer buffer,
            int index,
            int length)
        {
            httpDataRO.wrap(buffer, index, index + length);

            final OctetsFW payload = httpDataRO.payload();

            target.doData(this.targetId, payload);
        }

        private void onHttpEnd(
            DirectBuffer buffer,
            int index,
            int length)
        {
            httpEndRO.wrap(buffer, index, index + length);

            target.removeThrottle(targetId);
            source.removeStream(httpSourceId);
        }

        private void handleThrottle(
            int msgTypeId,
            DirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case WindowFW.TYPE_ID:
                processWindow(buffer, index, length);
                break;
            case ResetFW.TYPE_ID:
                processReset(buffer, index, length);
                break;
            default:
                // ignore
                break;
            }
        }

        private void processWindow(
            DirectBuffer buffer,
            int index,
            int length)
        {
            windowRO.wrap(buffer, index, length);

            // TODO: factor in variability in chunk header length
            final int update = windowRO.update();

            source.doWindow(httpSourceId, update);
        }

        private void processReset(
            DirectBuffer buffer,
            int index,
            int length)
        {
            resetRO.wrap(buffer, index, length);

            source.doReset(httpSourceId);
        }
    }
}
