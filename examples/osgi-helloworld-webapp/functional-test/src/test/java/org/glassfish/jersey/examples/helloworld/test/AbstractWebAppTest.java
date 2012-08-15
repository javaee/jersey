/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.helloworld.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.repositories;

/**
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class AbstractWebAppTest {

    public abstract List<Option> osgiRuntimeOptions();

    public List<Option> genericOsgiOptions() {
        @SuppressWarnings("RedundantStringToString")
        final String bundleLocation = mavenBundle().
                groupId("org.glassfish.jersey.examples.osgi-helloworld-webapp").
                artifactId("war-bundle").
                type("war").versionAsInProject().getURL().toString();

        List<Option> options = Arrays.asList(options(
                systemProperty("org.osgi.service.http.port").value(String.valueOf(port)),
                systemProperty("jersey.config.test.container.port").value(String.valueOf(port)),
                systemProperty(BundleLocationProperty).value(bundleLocation),

                // do not remove the following line
                // systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("FINEST"),

                repositories("http://repo1.maven.org/maven2",
                        "http://repository.apache.org/content/groups/snapshots-group",
                        "http://repository.ops4j.org/maven2",
                        "http://svn.apache.org/repos/asf/servicemix/m2-repo",
                        "http://repository.springsource.com/maven/bundles/release",
                        "http://repository.springsource.com/maven/bundles/external",
                        "http://maven.java.net/content/repositories/snapshots"),

                // uncomment for logging (do not remove the following two lines)
                // mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.4"),
                // mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.4"),

                mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").versionAsInProject(),

                // Google Guava
                mavenBundle().groupId("com.googlecode.guava-osgi").artifactId("guava-osgi").versionAsInProject(),

                // HK2
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-api").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("osgi-resource-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-utils").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("javax.inject").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("asm-all-repackaged").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("cglib").versionAsInProject(),

                // javax.annotation
                wrappedBundle(mavenBundle().groupId("javax.annotation").artifactId("jsr250-api").versionAsInProject()),

                // JAX-RS API
                mavenBundle().groupId("javax.ws.rs").artifactId("javax.ws.rs-api").versionAsInProject(),

                // Jersey bundles
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-common").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-server").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-client").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.containers").artifactId("jersey-container-servlet-core")
                        .versionAsInProject()

                // Debug
                // vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" )
        ));

        final String localRepository = System.getProperty("localRepository");
        if (localRepository != null) {
            options = new ArrayList<Option>(options);
            options.add(systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepository));
        }

        return options;
    }

    public List<Option> felixOptions() {
        return Arrays.asList(options(
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.eventadmin").versionAsInProject(),
                felix()));
    }

    public List<Option> equinoxOptions() {
        return Arrays.asList(options(
                mavenBundle().groupId("org.eclipse.equinox").artifactId("event").versionAsInProject(),
                equinox()));
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
            semaphore.release();
        }

        public WebEventHandler(String handlerName) {
            this.handlerName = handlerName;
        }
        private final String handlerName;

        protected String getHandlerName() {
            return handlerName;
        }
    }

    final Semaphore semaphore = new Semaphore(0);

    private static final int port = getProperty("jersey.config.test.container.port", 8080);
    private static final String CONTEXT = "/helloworld";
    private static final URI baseUri = UriBuilder.fromUri("http://localhost").port(port).path(CONTEXT).build();
    private static final String BundleLocationProperty = "jersey.bundle.location";

    @Inject BundleContext bundleContext;

    @SuppressWarnings("UnusedDeclaration")
    @Configuration
    public Option[] configuration() {
        List<Option> options = new LinkedList<Option>();

        options.addAll(genericOsgiOptions());
        options.addAll(jettyOptions());
        options.addAll(osgiRuntimeOptions());

        return options.toArray(new Option[options.size()]);
    }

    public void defaultMandatoryBeforeMethod() {
        bundleContext.registerService(EventHandler.class.getName(), new WebEventHandler("Deploy Handler"), getHandlerServiceProperties("jersey/test/DEPLOYED"));
    }

    public void defaultWebAppTestMethod() throws Exception {

        final Bundle warBundle = bundleContext.installBundle(System.getProperty(BundleLocationProperty));
        warBundle.start();

        semaphore.acquire();

        Client c = ClientFactory.newClient();

        final WebTarget target = c.target(baseUri);

        String result;

        result = target.path("/webresources/helloworld").request().build("GET").invoke().readEntity(String.class);

        System.out.println("HELLO RESULT = " + result);
        assertEquals("Hello World", result);

        String result2 = target.path("/webresources/another").request().build("GET").invoke().readEntity(String.class);

        System.out.println("ANOTHER RESULT = " + result2);
        assertEquals("Another", result2);
    }

    public static int getProperty(final String varName, int defaultValue) {
        if (null == varName) {
            return defaultValue;
        }
        String varValue = System.getProperty(varName);
        if (null != varValue) {
            try {
                return Integer.parseInt(varValue);
            } catch (NumberFormatException e) {
                // will return default value bellow
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
