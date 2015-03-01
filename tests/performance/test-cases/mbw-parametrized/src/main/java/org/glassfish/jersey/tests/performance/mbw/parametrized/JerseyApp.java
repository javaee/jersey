/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.tests.performance.mbw.parametrized;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

import org.glassfish.grizzly.http.server.HttpServer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.TimerContext;

/**
 * Application class to start generic performance test rest service. Deployed restful application
 * is configurable by command line arguments. You can specify the URI to which the application should be
 * deployed, media type which should consume and produce, file from which data will be read, etc. Execute
 * the application with parameter {@code --help} to see more details.
 * <p>
 * The rest app contains three methods: {@code GET}, {@code PUT} and {@code POST}. {@code GET} method
 * can return data from a custom file. In order to use specific message body provider, define the provider
 * full class name in command line arguments. The provider must be on the classpath.
 * </p>
 *
 * @author Miroslav Fuksa
 */
public class JerseyApp {

    public static final Person PERSON_FROM_FILE = new Person("Custom", 1, "V Parku");
    private static final URI BASE_URI = URI.create("http://localhost:8080/");
    public static final String ROOT_PATH = "person";
    private static final Logger LOGGER = Logger.getLogger(JerseyApp.class.getName());

    public static final Person PERSON = new Person("Jozef Zwejk", 40, "Very important address");
    public static final String MBEAN_GROUP_NAME = "JerseyPerformanceTests";
    private volatile HttpServer server;

    /**
     * Starts the server.
     *
     * @param args Command line arguments.
     * @throws Exception
     */
    public void start(String[] args) throws Exception {
        System.out.println("Jersey performance test web service application");
        final Config config = parseCommandLineArgs(args);
        if (config == null) {
            System.out.println("Web Application not started.");
            return;
        }

        final ResourceConfig resourceConfig = new ResourceConfig();
        configure(resourceConfig, config);
        final URI uri = config.getUri();

        server = GrizzlyHttpServerFactory.createHttpServer(uri, resourceConfig);

        System.out.println(String.format("Application started.\nTry out %s%s\nHit Ctrl-C to stop it...",
                uri, ROOT_PATH));
        if (!config.isJunitTest()) {
            while (server.isStarted()) {
                Thread.sleep(600000);
            }
        }
    }

    /**
     * Stop the server (used for tests).
     */
    public void stop() {
        server.shutdown();
    }

    /**
     * Start the server. The server is started and the execution thread waits until the server finished if the
     * command line parameter {@code --junit} is not defined. For unit testing use {@link #start(String[])} and
     * {@link #stop()} instead.
     *
     * @param args Command line arguments (important for service configuration). See options
     *             by using {@code --help} parameter.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final JerseyApp jerseyApp = new JerseyApp();
        jerseyApp.start(args);
    }

    private static Config parseCommandLineArgs(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(new Option("t", "type", true, "media type; mandatory (for example: \"application/json\"). Now supports application/json,application/xml,text/plain."));
        options.addOption(new Option("n", "name", true, "defines group name of Metric exposed by JMX MBeans; optional (default valeue JerseyPerformanceTests)."));
        options.addOption(new Option("u", "uri", true, "base uri; optional (for example: \"http://localhost:8080/\"). Default value is standard JerseyTest base uri."));
        options.addOption(new Option("p", "provider", true, "provider full class; optional (for example \"org.glassfish.jersey.moxy.json.MoxyJsonFeature\")"));
        options.addOption(new Option("f", "file", true, "file with content that should be returned from the GET method; optional (for example \"input.xml\")"));
        options.addOption(new Option("j", "junit", false, "use this flag if the application is run in the junit test mode. In this mode the server is started but the thread is not blocked in the main method."));
        options.addOption(new Option("h", "help", false, "prints this help."));

        CommandLineParser parser = new BasicParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            printHelp(options);
            e.printStackTrace();

            throw e;
        }
        if (cmd.hasOption("h")) {
            printHelp(options);
            return null;
        }

        if (!cmd.hasOption("t")) {
            printHelp(options);
            final String msg = "Parameter -t must be defined.";
            System.out.println(msg);

            throw new IllegalArgumentException(msg);
        }


        final String type = cmd.getOptionValue("t");
        final MediaType mediaType = MediaType.valueOf(type);
        final String provider = cmd.getOptionValue("p", null);
        final String fileName = cmd.getOptionValue("f", null);
        final String uriStr = cmd.getOptionValue("u", null);
        final URI uri = uriStr == null ? BASE_URI : new URI(uriStr);
        final boolean junitTest = cmd.hasOption("j");

        String name = cmd.getOptionValue("n", null);
        if (name == null) {
            name = "MbwParametrizedTest";
        }

        Class providerClass = null;
        File file = null;
        if (provider != null) {
            providerClass = Class.forName(provider);
        }
        if (fileName != null) {
            file = new File(fileName);
        }
        return new Config(mediaType, providerClass, file, uri, junitTest, name);
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("<executable>", options);
        System.out.println("The deployed application contains three methods:");
        System.out.println("GET: return entity media type defined by the --type parameter. The entity is generated but you"
                + " can specify --file <file> from which the data will be loaded.");
        System.out.println("POST: reads entity and writes the same entity back to the wire.");
        System.out.println("PUT: reads entity and returns 204.");
        System.out.println("\nexample: java <java params> JerseyApp -t application/json -p org.glassfish.jersey.moxy.json.MoxyJsonFeature --name MyCustomMoxyTest -u http://localhost:9998");
    }

    private void configure(ResourceConfig resourceConfig, final Config config) throws ClassNotFoundException, FileNotFoundException {
        if (config.getProvider() != null) {
            resourceConfig.register(config.getProvider());
        }

        final com.yammer.metrics.core.Timer postTimer =
                Metrics.newTimer(new MetricName(MBEAN_GROUP_NAME, config.getMetricName(), "posts"),
                        TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        final com.yammer.metrics.core.Timer getTimer =
                Metrics.newTimer(new MetricName(MBEAN_GROUP_NAME, config.getMetricName(), "gets"),
                        TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        final com.yammer.metrics.core.Timer putTimer =
                Metrics.newTimer(new MetricName(MBEAN_GROUP_NAME, config.getMetricName(), "puts"),
                        TimeUnit.MILLISECONDS, TimeUnit.SECONDS);


        final boolean isText = config.getType().equals(MediaType.TEXT_PLAIN_TYPE);
        final Resource.Builder builder = Resource.builder("/" + ROOT_PATH);


        builder.addMethod("GET").produces(config.getType()).handledBy(new Inflector<ContainerRequestContext, Object>() {
            @Override
            public Response apply(ContainerRequestContext containerRequestContext) {
                TimerContext timer = getTimer.time();
                try {
                    Object entity;
                    if (config.getFile() != null) {
                        entity = JerseyApp.class.getClassLoader().getResourceAsStream(config.getFile().getName());
                    } else {
                        if (isText) {
                            entity = "text";
                        } else {
                            entity = PERSON;
                        }
                    }

                    return Response.ok().entity(entity).build();
                } finally {
                    timer.stop();
                }
            }
        });

        builder.addMethod("POST").consumes(config.getType()).produces(config.getType()).handledBy(new Inflector<ContainerRequestContext, Object>() {
            @Override
            public Response apply(ContainerRequestContext containerRequestContext) {
                TimerContext timer = postTimer.time();
                try {
                    final Object entity = ((ContainerRequest) containerRequestContext).readEntity(isText ? String.class : Person.class);
                    return Response.ok().entity(entity).build();
                } finally {
                    timer.stop();
                }

            }
        });

        builder.addMethod("PUT").consumes(config.getType()).produces(config.getType()).handledBy(new Inflector<ContainerRequestContext, Object>() {
            @Override
            public Response apply(ContainerRequestContext containerRequestContext) {
                TimerContext timer = putTimer.time();
                try {
                    // read entity in order to test performance of MBR
                    ((ContainerRequest) containerRequestContext).readEntity(isText ? String.class : Person.class);
                    return Response.noContent().build();
                } finally {
                    timer.stop();
                }
            }
        });


        resourceConfig.registerResources(builder.build());
    }
}
