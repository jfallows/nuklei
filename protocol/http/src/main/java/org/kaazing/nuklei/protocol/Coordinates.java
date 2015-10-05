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
package org.kaazing.nuklei.protocol;

/**
 * Offset and Length tuple for variable length fields.
 */
public class Coordinates
{
    private int offset;
    private int length;

    public Coordinates()
    {
        reset();
    }

    public void reset()
    {
        offset = 0;
        length = 0;
    }

    public void offset(final int offset)
    {
        this.offset = offset;
    }

    public int offset()
    {
        return offset;
    }

    public void length(final int length)
    {
        this.length = length;
    }

    public int length()
    {
        return length;
    }
}
