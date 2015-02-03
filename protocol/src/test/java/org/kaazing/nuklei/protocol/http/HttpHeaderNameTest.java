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

package org.kaazing.nuklei.protocol.http;

import org.junit.Test;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import static org.junit.Assert.assertEquals;

public class HttpHeaderNameTest
{

    @Test
    public void matchMixedCaseHeader() throws Exception
    {
        final String request = "GET /path\r\nHosT: www.example.com\r\nCoNtEnt-lEngth: 123\r\n\r\n";
        MutableDirectBuffer buf = new UnsafeBuffer(request.getBytes("UTF-8"));

        HttpHeaderName host = HttpHeaderName.get(buf, request.indexOf("HosT"));
        assertEquals(HttpHeaderName.HOST, host);

        HttpHeaderName contentLength = HttpHeaderName.get(buf, request.indexOf("CoNtEnt-lEngth"));
        assertEquals(HttpHeaderName.CONTENT_LENGTH, contentLength);
    }

}
