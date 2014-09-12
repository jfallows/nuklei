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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.nuklei.net.TcpManagerEvents;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

import java.util.concurrent.atomic.AtomicBoolean;

public class KompoundIT
{
    @Rule
    public RobotRule robot = new RobotRule().setScriptRoot("org/kaazing/robot/scripts/nuklei/kompound");

    private Kompound kompound;

    @After
    public void cleanUp() throws Exception
    {
        if (null != kompound)
        {
            kompound.close();
        }
    }

    @Robotic(script = "connect")
    @Test(timeout = 1000)
    @Ignore
    public void shouldSpinUpCorrectlyAndConnect() throws Exception {
        final AtomicBoolean done = new AtomicBoolean(false);
        final Kompound.Builder builder = new Kompound.Builder()
            .service(
                "tcp://localhost:9876",
                (typeId, buffer, offset, length) ->
                {
                    System.out.println("typeId " + typeId);
                    if (TcpManagerEvents.EOF_TYPE_ID == typeId)
                    {
                        done.lazySet(true);
                    }
                    return 0;
                });

        kompound = Kompound.startUp(builder);

        while (!done.get())
        {
            Thread.sleep(10);
        }
        System.out.println("done");

        robot.join();
    }
}
