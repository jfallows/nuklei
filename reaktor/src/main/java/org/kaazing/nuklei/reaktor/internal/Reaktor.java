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
package org.kaazing.nuklei.reaktor.internal;

import static java.lang.String.format;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.cli.Option.builder;
import static uk.co.real_logic.agrona.IoUtil.tmpDirName;
import static uk.co.real_logic.agrona.LangUtil.rethrowUnchecked;

import java.util.Comparator;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.kaazing.nuklei.Configuration;
import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.NukleusFactory;

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.collections.ArrayUtil;
import uk.co.real_logic.agrona.concurrent.Agent;
import uk.co.real_logic.agrona.concurrent.AgentRunner;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.SigIntBarrier;
import uk.co.real_logic.agrona.concurrent.SleepingIdleStrategy;

public final class Reaktor implements AutoCloseable
{
    private final AgentRunner runner;

    private Reaktor(Configuration config, ToBooleanFunction<String> includes)
    {
        NukleusFactory factory = NukleusFactory.instantiate();

        Nukleus[] nuklei = new Nukleus[0];

        for (String name : factory.names())
        {
            if (includes.applyAsBoolean(name))
            {
                Nukleus nukleus = factory.create(name, config);
                nuklei = ArrayUtil.add(nuklei, nukleus);
            }
        }

        final Nukleus[] workers = nuklei;
        IdleStrategy idleStrategy = new SleepingIdleStrategy(MILLISECONDS.toNanos(50L));
        ErrorHandler errorHandler = throwable -> throwable.printStackTrace(System.err);
        Agent agent = new Agent()
        {
            @Override
            public String roleName()
            {
                return "reaktor";
            }

            @Override
            public int doWork() throws Exception
            {
                int work = 0;

                for (int i=0; i < workers.length; i++)
                {
                    work += workers[i].process();
                }

                return work;
            }
        };

        this.runner = new AgentRunner(idleStrategy, errorHandler, null, agent);
    }

    public Reaktor start()
    {
        new Thread(runner).start();
        return this;
    }

    @Override
    public void close() throws Exception
    {
        try
        {
            runner.close();
        }
        catch (final Exception ex)
        {
            rethrowUnchecked(ex);
        }
    }

    public static Reaktor launch(final Configuration config, ToBooleanFunction<String> includes)
    {
        Reaktor reaktor = new Reaktor(config, includes);
        reaktor.start();
        return reaktor;
    }

    public static void main(final String[] args) throws Exception
    {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(builder("d").longOpt("directory").hasArg().desc("configuration directory").build());
        options.addOption(builder("h").longOpt("help").desc("print this message").build());
        options.addOption(builder("n").longOpt("nukleus").hasArgs().desc("nukleus name").build());

        CommandLine cmdline = parser.parse(options, args);

        if (cmdline.hasOption("help"))
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("reaktor", options);
        }
        else
        {
            String directory = cmdline.getOptionValue("directory", format("%s/org.kaazing.nuklei.reaktor", tmpDirName()));
            String[] nuklei = cmdline.getOptionValues("nukleus");

            Properties properties = new Properties();
            properties.setProperty(Configuration.DIRECTORY_PROPERTY_NAME, directory);

            Configuration config = new Configuration(properties);

            ToBooleanFunction<String> includes = name -> true;
            if (nuklei != null)
            {
                Comparator<String> c = (o1, o2) -> o1.compareTo(o2);
                sort(nuklei, c);
                includes = name -> (binarySearch(nuklei, name, c) >= 0);
            }

            try (final Reaktor reaktor = Reaktor.launch(config, includes))
            {
                System.out.println("Started in " + directory);

                new SigIntBarrier().await();
            }
        }
    }

    @FunctionalInterface
    private interface ToBooleanFunction<T>
    {
        boolean applyAsBoolean(T value);
    }
}
