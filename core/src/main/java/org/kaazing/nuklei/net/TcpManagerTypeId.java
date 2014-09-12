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

package org.kaazing.nuklei.net;

/**
 * Events and commands for the TCP Manager
 */
public class TcpManagerTypeId
{
    /** events */
    public static final int NEW_CONNECTION = 1;
    public static final int RECEIVED_DATA = 2;
    public static final int ATTACH_COMPLETED = 3;
    public static final int DETACH_COMPLETED = 4;
    public static final int EOF = 5;

    /** commands */
    public static final int SEND_DATA = 6;
    public static final int CLOSE_CONNECTION = 7;
}
