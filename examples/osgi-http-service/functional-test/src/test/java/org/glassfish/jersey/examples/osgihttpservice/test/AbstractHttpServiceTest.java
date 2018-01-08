/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.osgihttpservice.test;

import java.net.URI;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import javax.inject.Inject;

import org.glassfish.jersey.internal.util.PropertiesHelper;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class AbstractHttpServiceTest {

    @Inject
    BundleContext bundleContext;

    /** maximum waiting time for runtime initialization and Jersey deployment */
    public static final long MAX_WAITING_SECONDS = 10L;

    /** Latch for blocking the testing thread until the runtime is ready and Jersey deployed */
    final CountDownLatch countDownLatch = new CountDownLatch(1);

    private static final int port = getProperty("jersey.config.test.container.port", 8080);
    private static final String CONTEXT = "/jersey-http-service";
    private static final URI baseUri = UriBuilder.fromUri("http://localhost").port(port).path(CONTEXT).build();
    private static final String BundleLocationProperty = "jersey.bundle.location";

    private static final Logger LOGGER = Logger.getLogger(AbstractHttpServiceTest.class.getName());

    public abstract List<Option> httpServiceProviderOptions();

    public abstract List<Option> osgiRuntimeOptions();

    public List<Option> genericOsgiOptions() {
        @SuppressWarnings("RedundantStringToString")
        final String bundleLocation = mavenBundle()
                .groupId("org.glassfish.jersey.examples.osgi-http-service")
                .artifactId("bundle")
                .versionAsInProject().getURL().toString();

        List<Option> options = Arrays.asList(options(
                systemProperty("org.osgi.service.http.port").value(String.valueOf(port)),
                systemProperty(BundleLocationProperty).value(bundleLocation),
                systemProperty("jersey.config.test.container.port").value(String.valueOf(port)),
                systemProperty("org.osgi.framework.system.packages.extra").value("javax.annotation"),

                // do not remove the following line
                // systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("FINEST"),

                // uncomment for logging (do not remove the following two lines)
                // mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.4"),
                // mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.4"),

                // javax.annotation has to go first!
                mavenBundle().groupId("javax.annotation").artifactId("javax.annotation-api").versionAsInProject(),

                mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),
                junitBundles(),

                // HK2
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-api").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("osgi-resource-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-utils").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("javax.inject").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("aopalliance-repackaged").versionAsInProject(),
                mavenBundle().groupId("org.javassist").artifactId("javassist").versionAsInProject(),

                // JAX-RS API
                mavenBundle().groupId("javax.ws.rs").artifactId("javax.ws.rs-api").versionAsInProject(),

                // validation
                mavenBundle().groupId("javax.validation").artifactId("validation-api").versionAsInProject(),

                // Jersey bundles
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-common").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-server").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-client").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.containers").artifactId("jersey-container-servlet-core")
                .versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.inject").artifactId("jersey-hk2").versionAsInProject()
                ));

        final String localRepository = AccessController.doPrivileged(PropertiesHelper.getSystemProperty("localRepository"));
        if (localRepository != null) {
            options = new ArrayList<>(options);
            options.add(systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepository));
        }

        return options;
    }

    public List<Option> grizzlyOptions() {
        return Arrays.asList(options(
                mavenBundle().groupId("com.sun.grizzly.osgi").artifactId("grizzly-httpservice-bundle").versionAsInProject()
        ));
    }

    public List<Option> jettyOptions() {
        return Arrays.asList(options(
                mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-jetty-bundle").versionAsInProject(),
                mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-extender-war").versionAsInProject()));
    }

    @SuppressWarnings("UnusedDeclaration")
    public class WebEventHandler implements EventHandler {

        @Override
        public void handleEvent(Event event) {
            countDownLatch.countDown();
        }

        public WebEventHandler(String handlerName) {
            this.handlerName = handlerName;
        }

        private final String handlerName;

        protected String getHandlerName() {
            return handlerName;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Configuration
    public Option[] configuration() {
        final List<Option> options = new LinkedList<>();

        options.addAll(genericOsgiOptions());
        options.addAll(httpServiceProviderOptions());
        options.addAll(osgiRuntimeOptions());

        return options.toArray(new Option[options.size()]);
    }

    public void defaultMandatoryBeforeMethod() {
        LOGGER.fine("Registering event handler for jersey/test/DEPLOYED");
        bundleContext.registerService(EventHandler.class.getName(), new WebEventHandler("Deploy Handler"),
                getHandlerServiceProperties("jersey/test/DEPLOYED"));
    }

    public void defaultHttpServiceTestMethod() throws Exception {
        // log the list of bundles and result of the attempt to start
        StringBuilder sb = new StringBuilder();
        sb.append("-- Bundle list -- \n");
        for (Bundle b : bundleContext.getBundles()) {
            sb.append(String.format("%1$5s", "[" + b.getBundleId() + "]")).append(" ")
                    .append(String.format("%1$-70s", b.getSymbolicName())).append(" | ")
                    .append(String.format("%1$-20s", b.getVersion())).append(" |");
            try {
                b.start();
                sb.append(" STARTED  | ");
            } catch (BundleException e) {
                sb.append(" *FAILED* | ").append(e.getMessage());
            }
            sb.append(b.getLocation()).append("\n");
        }
        sb.append("-- \n\n");
        LOGGER.fine(sb.toString());

        // start the example bundle
        bundleContext.installBundle(
                AccessController.doPrivileged(PropertiesHelper.getSystemProperty(BundleLocationProperty))
        ).start();

        LOGGER.fine("Waiting for jersey/test/DEPLOYED event with timeout " + MAX_WAITING_SECONDS + " seconds...");
        if (!countDownLatch.await(MAX_WAITING_SECONDS, TimeUnit.SECONDS)) {
            throw new TimeoutException("The event jersey/test/DEPLOYED did not arrive in "
                    + MAX_WAITING_SECONDS
                    + " seconds. Waiting timed out.");
        }

        Client c = ClientBuilder.newClient();
        final WebTarget target = c.target(baseUri);

        String result = target.path("/status").request().build("GET").invoke().readEntity(String.class);

        LOGGER.info("JERSEY RESULT = " + result);
        assertEquals("active", result);
    }

    public static int getProperty(final String varName, int defaultValue) {
        if (null == varName) {
            return defaultValue;
        }
        String varValue = AccessController.doPrivileged(PropertiesHelper.getSystemProperty(varName));
        if (null != varValue) {
            try {
                return Integer.parseInt(varValue);
            } catch (NumberFormatException e) {
                // will return default value below
            }
        }
        return defaultValue;
    }

    @SuppressWarnings({"UseOfObsoleteCollectionType", "unchecked"})
    private Dictionary getHandlerServiceProperties(String... topics) {
        Dictionary result = new Hashtable();
        result.put(EventConstants.EVENT_TOPIC, topics);
        return result;
    }
}
