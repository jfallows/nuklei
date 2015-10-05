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
package org.kaazing.nuklei.protocol.ws.codec;

/**
 * This variant of WsProtocolException is needed so we can send the correct close status code of 1007 if we get a Text
 * frame containing invalid UTF-8 (see RFC-6455 section 7.4.1).
 */
public class InvalidUTF8ProtocolException extends ProtocolException
{
    private static final long serialVersionUID = 4905969734515008256L;

    public InvalidUTF8ProtocolException(String message)
    {
        super(message);
    }

    public InvalidUTF8ProtocolException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
