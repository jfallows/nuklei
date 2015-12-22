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
package org.kaazing.specification.nuklei.common;

import static uk.co.real_logic.agrona.IoUtil.createEmptyFile;

import java.io.File;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor;

public final class NukleusRule implements TestRule
{
    private static final int STREAMS_BUFFER_CAPACITY_DEFAULT = 1024 * 1024;

    private File directory;
    private int streamsBufferCapacity;

    public NukleusRule()
    {
        this.streamsBufferCapacity = STREAMS_BUFFER_CAPACITY_DEFAULT;
    }

    public NukleusRule setDirectory(String directory)
    {
        this.directory = new File("./" + directory);
        return this;
    }

    public NukleusRule setStreamsBufferCapacity(int streamsBufferCapacity)
    {
        this.streamsBufferCapacity = streamsBufferCapacity;
        return this;
    }

    public NukleusRule initialize(
        String reader,
        String writer)
    {
        File streams = new File(directory, String.format("%s/streams/%s", reader, writer));
        createEmptyFile(streams.getAbsoluteFile(), streamsBufferCapacity + RingBufferDescriptor.TRAILER_LENGTH);
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description)
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                base.evaluate();
            }
        };
    }
}