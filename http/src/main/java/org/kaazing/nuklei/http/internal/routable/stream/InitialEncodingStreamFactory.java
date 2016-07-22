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

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.nuklei.http.internal.routable.Route;
import org.kaazing.nuklei.http.internal.routable.Source;
import org.kaazing.nuklei.http.internal.routable.Target;
import org.kaazing.nuklei.http.internal.router.Router;
import org.kaazing.nuklei.http.internal.types.OctetsFW;
import org.kaazing.nuklei.http.internal.types.stream.BeginFW;
import org.kaazing.nuklei.http.internal.types.stream.DataFW;
import org.kaazing.nuklei.http.internal.types.stream.EndFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpBeginFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpDataFW;
import org.kaazing.nuklei.http.internal.types.stream.HttpEndFW;
import org.kaazing.nuklei.http.internal.types.stream.ResetFW;
import org.kaazing.nuklei.http.internal.types.stream.WindowFW;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;

public final class InitialEncodingStreamFactory
{
    private final HttpBeginFW httpBeginRO = new HttpBeginFW();
    private final HttpDataFW httpDataRO = new HttpDataFW();
    private final HttpEndFW httpEndRO = new HttpEndFW();

    private final WindowFW windowRO = new WindowFW();
    private final ResetFW resetRO = new ResetFW();

    private final Router router;
    private final Source source;

    public InitialEncodingStreamFactory(
        Router router,
        Source source)
    {
        this.router = router;
        this.source = source;
    }

    public MessageHandler newStream(
        Route route,
        long streamId)
    {
        return new InitialEncodingStream(route)::handleStream;
    }

    private final class InitialEncodingStream
    {
        private final Route route;

        private Target target;
        private long targetId;

        private long httpSourceId;

        private InitialEncodingStream(
            Route route)
        {
            this.route = route;
        }

        private void handleStream(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length)
        {
            switch (msgTypeId)
            {
            case BeginFW.TYPE_ID:
                httpBeginRO.wrap(buffer, index, index + length);
                onBegin(httpBeginRO);
                break;
            case DataFW.TYPE_ID:
                httpDataRO.wrap(buffer, index, index + length);
                onData(httpDataRO);
                break;
            case EndFW.TYPE_ID:
                httpEndRO.wrap(buffer, index, index + length);
                onEnd(httpEndRO);
                break;
            default:
                // ignore
                break;
            }
        }

        private void onBegin(
            HttpBeginFW httpBegin)
        {
            final Map<String, String> httpHeaders = new HashMap<>();
            httpBegin.headers().forEach(h -> httpHeaders.put(h.name().asString(), h.value().asString()));

            final Route.Option resolved = route.resolve(httpHeaders);
            final long targetRef = resolved.targetRef();

            this.target = resolved.target();

            // TODO: replace with connection pool (start)
            this.targetId = route.newTargetId();
            final long httpSourceId = httpBegin.streamId();
            final long httpSourceRef = httpBegin.routableRef();
            final long httpReplyRef = httpBegin.replyRef();
            final long httpReplyId = httpBegin.replyId();
            final long replyRef = ~targetRef;
            final long replyId = ~targetId;
            target.doBegin(targetRef, targetId, replyRef, replyId);
            final String replyName = resolved.reply();
            router.doRegisterReplyDecoder(target.name(), replyRef, replyId, replyName,
                    httpReplyRef, httpReplyId, httpSourceRef, httpSourceId);
            // TODO: replace with connection pool (end)

            String[] start = new String[2];
            StringBuilder headers = new StringBuilder();
            httpBegin.headers().forEach(header ->
            {
                String name = header.name().asString();
                String value = header.value().asString();

                if (name.charAt(0) == ':')
                {
                    if (":method".equals(name))
                    {
                        start[0] = value;
                    }
                    else if (":path".equals(name))
                    {
                        start[1] = value;
                    }
                }
                else
                {
                    Pattern pattern = Pattern.compile("^\\p{Lower}|-\\p{Lower}");
                    Matcher matcher = pattern.matcher(name);
                    StringBuffer sb = new StringBuffer();
                    while (matcher.find())
                    {
                        matcher.appendReplacement(sb, matcher.group().toUpperCase());
                    }
                    matcher.appendTail(sb);
                    String canonicalName = sb.toString();

                    headers.append(canonicalName).append(": ").append(value).append("\r\n");
                }
            });

            // TODO: validate method and path
            String method = start[0];
            String requestURI = start[1];

            StringBuilder request = new StringBuilder();
            request.append(method).append(" ").append(requestURI).append(" HTTP/1.1\r\n");
            request.append(headers).append("\r\n");

            DirectBuffer payload = new UnsafeBuffer(request.toString().getBytes(US_ASCII));

            target.doData(targetId, payload, 0, payload.capacity());
            target.addThrottle(targetId, this::handleThrottle);

            this.httpSourceId = httpBegin.streamId();
        }

        private void onData(
            HttpDataFW httpData)
        {
            final OctetsFW payload = httpData.payload();

            target.doData(this.targetId, payload);
        }

        private void onEnd(
            HttpEndFW httpEnd)
        {
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
