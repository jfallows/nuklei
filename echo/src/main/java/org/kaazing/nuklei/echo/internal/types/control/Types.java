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
package org.kaazing.nuklei.echo.internal.types.control;

public final class Types
{
    public static final int TYPE_ID_ERROR_RESPONSE = 0x40000000;

    public static final int TYPE_ID_CAPTURE_COMMAND = 0x00000001;
    public static final int TYPE_ID_CAPTURED_RESPONSE = 0x40000001;
    public static final int TYPE_ID_UNCAPTURE_COMMAND = 0x00000002;
    public static final int TYPE_ID_UNCAPTURED_RESPONSE = 0x40000002;
    public static final int TYPE_ID_ROUTE_COMMAND = 0x00000003;
    public static final int TYPE_ID_ROUTED_RESPONSE = 0x40000003;
    public static final int TYPE_ID_UNROUTE_COMMAND = 0x00000004;
    public static final int TYPE_ID_UNROUTED_RESPONSE = 0x40000004;

    public static final int TYPE_ID_BIND_COMMAND = 0x00000011;
    public static final int TYPE_ID_BOUND_RESPONSE = 0x40000011;
    public static final int TYPE_ID_UNBIND_COMMAND = 0x00000012;
    public static final int TYPE_ID_UNBOUND_RESPONSE = 0x40000012;
    public static final int TYPE_ID_PREPARE_COMMAND = 0x00000013;
    public static final int TYPE_ID_PREPARED_RESPONSE = 0x40000013;
    public static final int TYPE_ID_UNPREPARE_COMMAND = 0x00000014;
    public static final int TYPE_ID_UNPREPARED_RESPONSE = 0x40000014;

    public static final int TYPE_ID_CONNECT_COMMAND = 0x00000021;
    public static final int TYPE_ID_CONNECTED_RESPONSE = 0x40000021;

    private Types()
    {
        // no instances
    }
}
