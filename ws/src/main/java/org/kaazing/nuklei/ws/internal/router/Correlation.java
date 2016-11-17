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
package org.kaazing.nuklei.ws.internal.router;

import java.util.Objects;

public class Correlation
{
    private final long id;
    private final String hash;

    public Correlation(
        long id,
        String hash)
    {
        this.id = id;
        this.hash = hash;
    }

    public long id()
    {
        return id;
    }

    public String hash()
    {
        return hash;
    }

    @Override
    public int hashCode()
    {
        int result = hash.hashCode();
        result = 31 * result + Long.hashCode(id);

        return result;
    }

    @Override
    public boolean equals(
        Object obj)
    {
        if (!(obj instanceof Correlation))
        {
            return false;
        }

        Correlation that = (Correlation) obj;
        return this.id == that.id &&
                Objects.equals(this.hash, that.hash);
    }

    @Override
    public String toString()
    {
        return String.format("[id=%d, hash=\"%s\"]", id, hash);
    }
}
