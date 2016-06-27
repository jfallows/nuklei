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
package org.kaazing.nuklei.tcp.internal.watcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Arrays.stream;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;
import org.kaazing.nuklei.tcp.internal.Context;
import org.kaazing.nuklei.tcp.internal.router.Router;

import uk.co.real_logic.agrona.LangUtil;

@Reaktive
public final class Watcher implements Nukleus
{
    private final WatchService service;
    private final Path streamsPath;
    private final Set<Path> sourcePaths;
    private final Consumer<WatchEvent<?>> handleEvent;

    private Router router;
    private WatchKey streamsKey;

    public Watcher(
        Context context)
    {
        this.service = context.watchService();
        this.streamsPath = context.streamsPath();
        this.sourcePaths = new HashSet<>();

        Map<WatchEvent.Kind<?>, Consumer<WatchEvent<?>>> handlerMap = new HashMap<>();
        handlerMap.put(StandardWatchEventKinds.OVERFLOW, this::handleOverflow);
        handlerMap.put(StandardWatchEventKinds.ENTRY_CREATE, this::handleCreate);
        handlerMap.put(StandardWatchEventKinds.ENTRY_DELETE, this::handleDelete);
        this.handleEvent = e -> handlerMap.getOrDefault(e.kind(), this::handleUnexpected).accept(e);
    }

    public void setRouter(
        Router router)
    {
        this.router = router;
    }

    @Override
    public String name()
    {
        return "watcher";
    }

    @Override
    public int process()
    {
        registerIfNecessary();

        int workCount = 0;

        WatchKey key = service.poll();
        if (key != null && key.isValid())
        {
            List<WatchEvent<?>> events = key.pollEvents();
            workCount += events.size();
            events.forEach(handleEvent);
            key.reset();
        }

        return workCount;
    }

    @Override
    public void close() throws Exception
    {
        cancelIfNecessary();
    }

    private void handleCreate(
        WatchEvent<?> event)
    {
        Path sourcePath = (Path) event.context();
        handleCreatePath(sourcePath);
    }

    private void handleCreatePath(
        Path sourcePath)
    {
        if (sourcePaths.add(sourcePath))
        {
            router.onReadable(sourcePath);
        }
    }

    private void handleDelete(
        WatchEvent<?> event)
    {
        Path sourcePath = (Path) event.context();
        handleDeletePath(sourcePath);
    }

    private void handleDeletePath(
        Path sourcePath)
    {
        if (sourcePaths.remove(sourcePath))
        {
            router.onExpired(sourcePath);
        }
    }

    private void handleOverflow(
        WatchEvent<?> event)
    {
        syncWithFileSystem();
    }

    private void handleUnexpected(
        WatchEvent<?> event)
    {
        // ignore
    }

    private void registerIfNecessary()
    {
        if (streamsKey == null)
        {
            try
            {
                streamsPath.toFile().mkdirs();
                streamsKey = streamsPath.register(service, ENTRY_CREATE, ENTRY_DELETE, OVERFLOW);
                syncWithFileSystem();
            }
            catch (IOException ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    private void cancelIfNecessary()
    {
        if (streamsKey != null)
        {
            streamsKey.cancel();
        }
    }

    private void syncWithFileSystem()
    {
        sourcePaths.stream().filter(p -> !p.toFile().exists()).forEach(this::handleDeletePath);
        stream(streamsPath.toFile().listFiles()).map(f -> f.toPath()).forEach(this::handleCreatePath);
    }
}
