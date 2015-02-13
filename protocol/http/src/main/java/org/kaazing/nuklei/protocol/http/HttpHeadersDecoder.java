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

import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.function.Mikro;
import org.kaazing.nuklei.protocol.Coordinates;
import org.kaazing.nuklei.protocol.ExpandableBuffer;
import org.kaazing.nuklei.protocol.ProtocolUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.function.Supplier;

/**
 */
public class HttpHeadersDecoder extends Flyweight implements Mikro
{
    private static final int INITIAL_REASSEMBLY_BUFFER_CAPACITY = 256;

    private static final byte[] SPACE = { ' ' };
    private static final byte[] CRLF = { '\r', '\n' };
    private static final byte[] SPACE_OR_HTAB = { ' ', '\t' };

    enum State
    {
        METHOD, PATH, VERSION, HEADERS, BODY
    }

    private final EnumMap<HttpHeaderName, Coordinates> standardHeaders;
    private final ExpandableBuffer reassemblyBuffer;
    private int cursor;
    private int limit;
    private State state;
    private Supplier<Coordinates> coordinatesSupplier;
    final DirectBuffer tmpHeaderBuffer = new UnsafeBuffer(new byte[0]);
    final ArrayList<Coordinates> unmatchedHeaderList = new ArrayList<>();

    public HttpHeadersDecoder(final Supplier<Coordinates> coordinatesSupplier)
    {
        super(ByteOrder.nativeOrder());

        this.coordinatesSupplier = coordinatesSupplier;

        reassemblyBuffer = new ExpandableBuffer(INITIAL_REASSEMBLY_BUFFER_CAPACITY);

        standardHeaders = new EnumMap<>(HttpHeaderName.class);
        for (final HttpHeaderName name : HttpHeaderName.Singleton.ALL_NAMES)
        {
            standardHeaders.put(name, coordinatesSupplier.get());
        }
    }

    public boolean isDecoded()
    {
        return (State.BODY == state);
    }

    public int cursor()
    {
        return cursor;
    }

    public int length()
    {
        return cursor;
    }

    public int limit()
    {
        return limit;
    }

    public Coordinates header(final HttpHeaderName name)
    {
        final Coordinates coordinates = standardHeaders.get(name);

        if (0 == coordinates.length())
        {
            final Coordinates unmatchedCoordinates = matchHeaderAndRemove(name.buffer(), name.lowerCaseBuffer(),
                    name.upperCaseBuffer());

            if (null != unmatchedCoordinates)
            {
                coordinates.offset(unmatchedCoordinates.offset() + name.length());
                coordinates.length(unmatchedCoordinates.length() - name.length());
            }
        }

        return coordinates;
    }

    /**
     * Fill in given buffer with value of given header if present
     *
     * This caches the standard headers for later lookup if desired.
     * This removes leading, but not trailing whitespace
     *
     * @param name        of the header
     * @param valueBuffer to fill in
     * @param offset      to start filling in from
     * @return            number of bytes copied or 0 if header not found
     */
    public int header(final HttpHeaderName name, final MutableDirectBuffer valueBuffer, final int offset)
    {
        final Coordinates coordinates = standardHeaders.get(name);

        if (0 == coordinates.length())
        {
            final Coordinates unmatchedCoordinates = matchHeaderAndRemove(name.buffer(), name.lowerCaseBuffer(),
                    name.upperCaseBuffer());

            if (null != unmatchedCoordinates)
            {
                final int startingOffset = unmatchedCoordinates.offset() + name.length();
                final int startingLength = unmatchedCoordinates.length() - name.length();
                final int leadingWs =
                    ProtocolUtil.leadingCount(buffer(), offset() + startingOffset, SPACE_OR_HTAB, startingLength);

                coordinates.offset(startingOffset + leadingWs);
                coordinates.length(startingLength - leadingWs);
            }
        }

        buffer().getBytes(offset() + coordinates.offset(), valueBuffer, offset, coordinates.length());

        return coordinates.length();
    }

    /**
     * Fill in given buffer with value of the given header if present
     *
     * This does NOT cache the header value. Each invocation will search for the header from the start.
     *
     * @param name        of the header
     * @param valueBuffer to fill in
     * @param offset      to start filling in from
     * @return            number of bytes copied or 0 if header not found
     */
    public int header(final byte[] name, final MutableDirectBuffer valueBuffer, final int offset)
    {
        tmpHeaderBuffer.wrap(name);
        return header(tmpHeaderBuffer, valueBuffer, offset);
    }

    /**
     * Fill in given buffer with value of the given header if present
     *
     * This does NOT cache the header value. Each invocation will search for the header from the start.
     *
     * @param nameBuffer  of the header
     * @param valueBuffer to fill in
     * @param offset      to start filling in from
     * @return            number of bytes copied or 0 if header not found
     */
    public int header(final DirectBuffer nameBuffer, final MutableDirectBuffer valueBuffer, final int offset)
    {
        final Coordinates coordinates = matchHeader(nameBuffer);

        if (null != coordinates)
        {
            buffer().getBytes(
                coordinates.offset() + nameBuffer.capacity(),
                valueBuffer,
                offset,
                coordinates.length() - nameBuffer.capacity());

            return coordinates.length() - nameBuffer.capacity();
        }

        return 0;
    }

    public void onMessage(
        final Object header, final int typeId, final DirectBuffer buffer, final int offset, final int length)
    {
        if (0 != limit)
        {
            reassemblyBuffer.putBytes(limit, buffer, offset, length);
            limit += length;
            wrap(reassemblyBuffer.atomicBuffer(), 0);
        }
        else
        {
            reset(buffer, offset);
            limit = length;
        }

        attemptDecode();

        if (!isDecoded() && length == limit)
        {
            reassemblyBuffer.putBytes(0, buffer, offset, length);
        }
    }

    // TODO: HTTP responder needs to know connectionId, etc. So, have dispatcher set it when it resets decoder.

    private HttpHeadersDecoder reset(final DirectBuffer buffer, final int offset)
    {
        wrap(buffer, offset);
        cursor = 0;
        limit = 0;
        state = State.METHOD;
        standardHeaders.forEach((n, c) -> c.reset());  // should not allocate
        return this;
    }

    private void header(final HttpHeaderName name, final int offset, final int length)
    {
        final Coordinates coordinates = standardHeaders.get(name);

        coordinates.offset(offset);
        coordinates.length(length);
    }

    private void attemptDecode()
    {
        // pick up where cursor is using fall through technique (no breaks on purpose!)
        switch (state)
        {
            case METHOD:
                if (!decodeMethod())
                {
                    return;
                }
            case PATH:
                if (!decodePath())
                {
                    return;
                }
            case VERSION:
                if (!decodeVersion())
                {
                    return;
                }
            case HEADERS:
                if (!decodeHeaders())
                {
                    return;
                }
        }
    }

    private boolean decodeMethod()
    {
        final int methodStart = offset() + cursor;
        final int methodEnd = ProtocolUtil.findNextOccurrence(buffer(), methodStart, SPACE, limit - cursor);

        if (methodEnd > 0)
        {
            final int position = methodStart - offset();
            header(HttpHeaderName.METHOD, position, methodEnd - methodStart);
            cursor = methodEnd - offset() + 1;
            state = State.PATH;
//            System.out.println("method " + position);
            return true;
        }

        return false;
    }

    private boolean decodePath()
    {
        final int pathStart = offset() + cursor;
        final int pathEnd = ProtocolUtil.findNextOccurrence(buffer(), pathStart, SPACE, limit - cursor);

        if (pathEnd > 0)
        {
            final int position = pathStart - offset();
            header(HttpHeaderName.PATH, position, pathEnd - pathStart);
            cursor = pathEnd - offset() + 1;
            state = State.VERSION;
//            System.out.println("path " + position);
            return true;
        }

        return false;
    }

    private boolean decodeVersion()
    {
        final int versionStart = offset() + cursor;
        final int versionEnd = ProtocolUtil.findNextOccurrence(buffer(), versionStart, CRLF, limit - cursor);

        if (versionEnd > 0)
        {
            final int position = versionStart - offset();
            header(HttpHeaderName.VERSION, position, versionEnd - versionStart - 1);
            cursor = versionEnd - offset() + 1;
            state = State.HEADERS;
//            System.out.println("version " + position);
            return true;
        }

        return false;
    }

    private boolean decodeHeaders()
    {
        while (true)
        {
            final int headerStart = offset() + cursor;
            final int headerEnd = ProtocolUtil.findNextOccurrence(buffer(), headerStart, CRLF, limit - cursor);
            final int position = headerStart - offset();

            if (headerEnd < 0)
            {
                break;
            }

            if (headerEnd - headerStart <= CRLF.length)
            {
                cursor = headerEnd - offset() + 1;
                state = State.BODY;
//                System.out.println("CRLF");
                return true;
            }

            // now have a complete "header: value" line, save it to the unmatched header list
            final Coordinates coordinates = coordinatesSupplier.get();

            coordinates.offset(position);
            coordinates.length(headerEnd - headerStart - 1);
            unmatchedHeaderList.add(coordinates);

//            System.out.println("header " + coordinates.offset() + " " + coordinates.length());

            cursor = headerEnd - offset() + 1;
        }

        return false;
    }

    private Coordinates matchHeaderAndRemove(final DirectBuffer nameBuffer, final DirectBuffer lowerCaseBuffer,
            final DirectBuffer upperCaseBuffer)
    {
        for (int i = unmatchedHeaderList.size() - 1; i >= 0; i--)
        {
            final Coordinates coordinates = unmatchedHeaderList.get(i);

            if (coordinates.length() >= nameBuffer.capacity() &&
                ProtocolUtil.compareMemory(
                    buffer(), offset() + coordinates.offset(), nameBuffer, 0, nameBuffer.capacity()))
            {
                unmatchedHeaderList.remove(i);

                return coordinates;
            }
        }

        // Match with case-insensitive header name
        for (int i = unmatchedHeaderList.size() - 1; i >= 0; i--)
        {
            final Coordinates coordinates = unmatchedHeaderList.get(i);

            if (coordinates.length() >= nameBuffer.capacity() &&
                    ProtocolUtil.compareCaseInsensitiveMemory(buffer(), offset() + coordinates.offset(),
                            lowerCaseBuffer, upperCaseBuffer, 0, nameBuffer.capacity()))
            {
                unmatchedHeaderList.remove(i);

                return coordinates;
            }
        }

        return null;
    }

    private Coordinates matchHeader(final DirectBuffer nameBuffer)
    {
        for (int i = unmatchedHeaderList.size() - 1; i >= 0; i--)
        {
            final Coordinates coordinates = unmatchedHeaderList.get(i);

            if (coordinates.length() >= nameBuffer.capacity() &&
                ProtocolUtil.compareMemory(
                    buffer(), offset() + coordinates.offset(), nameBuffer, 0, nameBuffer.capacity()))
            {
                return coordinates;
            }
        }

        return null;
    }
}
