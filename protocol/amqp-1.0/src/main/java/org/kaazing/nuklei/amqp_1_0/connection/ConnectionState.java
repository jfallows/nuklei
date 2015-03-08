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
package org.kaazing.nuklei.amqp_1_0.connection;

/*
 * See AMQP 1.0 specification, section 2.4.6 "Connection States"
 */
public enum ConnectionState
{
    START, HEADER_RECEIVED, HEADER_SENT, HEADER_EXCHANGED, OPEN_PIPE, OPEN_CLOSE_PIPE,
    OPEN_RECEIVED, OPEN_SENT, CLOSE_PIPE, OPENED, CLOSE_RECEIVED, CLOSE_SENT, DISCARDING,
    END
}