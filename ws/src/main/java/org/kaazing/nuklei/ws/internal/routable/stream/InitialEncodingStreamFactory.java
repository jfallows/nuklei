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
package org.kaazing.nuklei.ws.internal.routable.stream;

import org.kaazing.nuklei.ws.internal.routable.Route;
import org.kaazing.nuklei.ws.internal.routable.Source;
import org.kaazing.nuklei.ws.internal.router.Router;
import org.kaazing.nuklei.ws.internal.types.stream.WsBeginFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsDataFW;
import org.kaazing.nuklei.ws.internal.types.stream.WsEndFW;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;

public final class InitialEncodingStreamFactory
{
    private final WsBeginFW wsBeginRO = new WsBeginFW();
    private final WsDataFW wsDataRO = new WsDataFW();
    private final WsEndFW wsEndRO = new WsEndFW();

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
            case WsBeginFW.TYPE_ID:
                wsBeginRO.wrap(buffer, index, index + length);
                onWsBegin(wsBeginRO);
                break;
            case WsDataFW.TYPE_ID:
                wsDataRO.wrap(buffer, index, index + length);
                onData(wsDataRO);
                break;
            case WsEndFW.TYPE_ID:
                wsEndRO.wrap(buffer, index, index + length);
                onEnd(wsEndRO);
                break;
            default:
                // ignore
                break;
            }
        }

        private void onWsBegin(
            WsBeginFW wsBeginRO)
        {
            // TODO Auto-generated method stub
        }

        private void onData(
            WsDataFW wsDataRO)
        {
            // TODO Auto-generated method stub
        }

        private void onEnd(
            WsEndFW wsEndRO)
        {
            // TODO Auto-generated method stub
        }
    }
}
