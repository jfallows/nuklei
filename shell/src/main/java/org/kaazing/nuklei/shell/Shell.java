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
package org.kaazing.nuklei.shell;

import static java.lang.String.format;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.cli.Option.builder;
import static uk.co.real_logic.agrona.IoUtil.tmpDirName;
import static uk.co.real_logic.agrona.concurrent.AgentRunner.startOnThread;

import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.Controller;
import org.kaazing.nuklei.ControllerFactory;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.AgentRunner;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.CompositeAgent;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public final class Shell
{
    public static void main(final String[] args) throws Exception
    {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(builder("d").longOpt("directory").hasArg().desc("configuration directory").build());
        options.addOption(builder("h").longOpt("help").desc("print this message").build());
        options.addOption(builder("c").longOpt("controller").hasArgs().desc("controller name").build());

        CommandLine cmdline = parser.parse(options, args);

        if (cmdline.hasOption("help"))
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("reaktor", options);
        }
        else
        {
            String directory = cmdline.getOptionValue("directory", format("%s/org.kaazing.nuklei.reaktor", tmpDirName()));
            String[] controllers = cmdline.getOptionValues("controller");

            Properties properties = new Properties();
            properties.setProperty(Configuration.DIRECTORY_PROPERTY_NAME, directory);

            final Configuration config = new Configuration(properties);

            ToBooleanFunction<String> includes = name -> true;
            if (controllers != null)
            {
                Comparator<String> c = (o1, o2) -> o1.compareTo(o2);
                sort(controllers, c);
                includes = name -> (binarySearch(controllers, name, c) >= 0);
            }

            launch(config, includes);
        }
    }

    private static void launch(
        final Configuration config,
        final ToBooleanFunction<String> includes) throws Exception
    {
        final Map<String, Controller> controllers = new HashMap<>();
        final ControllerFactory factory = ControllerFactory.instantiate();
        for (Class<? extends Controller> kind : factory.kinds())
        {
            String name = factory.name(kind);
            if (includes.applyAsBoolean(name))
            {
                Controller controller = factory.create(kind, config);
                controllers.put(name, controller);
            }
        }

        Agent control = null;
        for (Controller controller : controllers.values())
        {
            if (control == null)
            {
                control = new ControllerAgent(controller);
            }
            else
            {
                control = new CompositeAgent(control, new ControllerAgent(controller));
            }
        }

        if (control == null)
        {
            System.out.println("Exiting: no controllers found");
        }
        else
        {
            IdleStrategy idleStrategy = new SleepingIdleStrategy(MILLISECONDS.toNanos(100));
            ErrorHandler errorHandler = throwable -> throwable.printStackTrace(System.err);
            AtomicBuffer labelsBuffer = new UnsafeBuffer(new byte[CountersManager.LABEL_LENGTH]);
            AtomicBuffer countersBuffer = new UnsafeBuffer(new byte[CountersManager.COUNTER_LENGTH]);
            CountersManager counters = new CountersManager(labelsBuffer, countersBuffer);
            AtomicCounter errorCounter = counters.newCounter("errors");

            AgentRunner runner = new AgentRunner(idleStrategy, errorHandler, errorCounter, control);

            startOnThread(runner);

            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("nashorn");

            Bindings bindings = engine.createBindings();
            bindings.put("nuklei", controllers);

            engine.eval(new InputStreamReader(System.in), bindings);

            // TODO: avoid closing eagerly
            runner.close();
        }
    }

    private static final class ControllerAgent implements Agent
    {
        private final Controller controller;

        private ControllerAgent(Controller controller)
        {
            this.controller = controller;
        }
        @Override
        public int doWork() throws Exception
        {
            return controller.process();
        }

        @Override
        public String roleName()
        {
            return controller.kind().getSimpleName();
        }
    }

    @FunctionalInterface
    private interface ToBooleanFunction<T>
    {
        boolean applyAsBoolean(T value);
    }
}
