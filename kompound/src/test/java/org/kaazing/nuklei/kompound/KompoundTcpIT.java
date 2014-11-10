/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
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

package org.kaazing.nuklei.kompound;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.nuklei.kompound.cmd.StartCmd;
import org.kaazing.nuklei.kompound.cmd.StopCmd;
import org.kaazing.nuklei.net.TcpManagerHeadersDecoder;
import org.kaazing.nuklei.net.TcpManagerTypeId;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class KompoundTcpIT
{
    public static final String URI = "tcp://localhost:9876";

    @Rule
    public RobotRule robot = new RobotRule().setScriptRoot("org/kaazing/robot/scripts/nuklei/kompound");

    final AtomicBoolean attached = new AtomicBoolean(false);

    private Kompound kompound;

    @After
    public void cleanUp() throws Exception
    {
        if (null != kompound)
        {
            kompound.close();
        }
    }

    @Test(timeout = 1000)
    public void shouldStartUpAndShutdownCorrectly() throws Exception
    {
        final AtomicBoolean started = new AtomicBoolean(false);
        final AtomicBoolean stopped = new AtomicBoolean(false);

        final Kompound.Builder builder = new Kompound.Builder()
            .service(
                URI,
                (header, typeId, buffer, offset, length) ->
                {
                    if (header instanceof StartCmd)
                    {
                        started.lazySet(true);
                    }
                    else if (header instanceof StopCmd)
                    {
                        stopped.lazySet(true);
                    }
                    else if (TcpManagerTypeId.ATTACH_COMPLETED == typeId)
                    {
                        attached.lazySet(true);
                    }
                });

        kompound = Kompound.startUp(builder);
        waitToBeAttached();

        kompound.close();
        kompound = null;

        assertTrue(started.get());
        assertTrue(stopped.get());
    }

    @Robotic(script = "ConnectAndWrite")
    @Test(timeout = 1000)
    public void shouldAllowConnectionAndSendOfDataFromClient() throws Exception
    {
        final String message = "hello world";
        final byte[] data = new byte[message.length()];

        final Kompound.Builder builder = new Kompound.Builder()
            .service(
                URI,
                (header, typeId, buffer, offset, length) ->
                {
                    switch (typeId)
                    {
                        case TcpManagerTypeId.ATTACH_COMPLETED:
                            attached.lazySet(true);
                            break;

                        case TcpManagerTypeId.RECEIVED_DATA:
                            buffer.getBytes(offset, data);
                            break;
                    }
                });

        kompound = Kompound.startUp(builder);
        waitToBeAttached();
        robot.join();

        assertThat(data, is(message.getBytes()));
    }

    @Robotic(script = "ConnectWriteRead")
    @Test(timeout = 1000)
    public void shouldConnectWriteReadFromClient() throws Exception
    {
        final Kompound.Builder builder = new Kompound.Builder()
            .service(
                URI,
                (header, typeId, buffer, offset, length) ->
                {
                    switch (typeId)
                    {
                        case TcpManagerTypeId.ATTACH_COMPLETED:
                            attached.lazySet(true);
                            break;

                        case TcpManagerTypeId.RECEIVED_DATA:
                            final TcpManagerHeadersDecoder decoder = (TcpManagerHeadersDecoder) header;
                            final MutableDirectBuffer echoBuffer = new UnsafeBuffer(new byte[decoder.length() + length]);

                            echoBuffer.putBytes(decoder.length(), buffer, offset, length);
                            decoder.respond(echoBuffer, decoder.length(), length);
                            break;
                    }
                });

        kompound = Kompound.startUp(builder);
        waitToBeAttached();
        robot.join();
    }

    private void waitToBeAttached() throws Exception
    {
        while (!attached.get())
        {
            Thread.sleep(10);
        }
    }
}
