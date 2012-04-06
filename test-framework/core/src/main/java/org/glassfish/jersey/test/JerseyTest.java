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
package org.glassfish.jersey.test;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Target;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ClientFactory;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.internal.ServiceFinderModule;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.After;
import org.junit.Before;

import com.google.common.collect.Maps;

/**
 * Parent class for all tests written using Jersey test framework.
 *
 * @author Paul Sandoz
 * @author Srinivas.Bhimisetty@Sun.COM
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(TestContainerFactory.class.getName());
    /**
     * Holds the default test container factory class to be used for running the
     * tests.
     */
    private static Class<? extends TestContainerFactory> testContainerFactoryClass;
    /**
     * The test container factory which creates an instance of the test container
     * on which the tests would be run.
     */
    private TestContainerFactory testContainerFactory;
    /**
     * The test container on which the tests would be run.
     */
    private final TestContainer tc;
    private final Client client;
    private final ApplicationHandler application;
    /**
     * JerseyTest property bag that can be used to configure the test behavior.
     * These properties can be overridden with a system property.
     */
    private final Map<String, String> propertyMap = Maps.newHashMap();
    /**
     * JerseyTest forced property bag that can be used to configure the test behavior.
     * These property cannot be overridden with a system property.
     */
    private final Map<String, String> forcedPropertyMap = Maps.newHashMap();

    /**
     * An extending class must implement the {@link #configure()} method to
     * provide an application descriptor.
     *
     * @throws TestContainerException if the default test container factory
     *         cannot be obtained, or the application descriptor is not
     *         supported by the test container factory.
     */
    public JerseyTest() throws TestContainerException {
        ResourceConfig config =  getResourceConfig(configure());
        config.addModules(new ServiceFinderModule<TestContainerFactory>(TestContainerFactory.class));
        this.application = new ApplicationHandler(config);

        this.tc = getContainer(application, getTestContainerFactory());
        this.client = getClient(tc, application);
    }

    /**
     * Construct a new instance with a test container factory.
     * <p>
     * An extending class must implement the {@link #configure()} method to
     * provide an application descriptor.
     *
     * @param testContainerFactory the test container factory to use for testing.
     * @throws TestContainerException if the application descriptor is not
     *         supported by the test container factory.
     */
    public JerseyTest(TestContainerFactory testContainerFactory) {
        setTestContainerFactory(testContainerFactory);

        ResourceConfig config =  getResourceConfig(configure());
        config.addModules(new ServiceFinderModule<TestContainerFactory>(TestContainerFactory.class));
        this.application = new ApplicationHandler(config);

        this.tc = getContainer(application, testContainerFactory);
        this.client = getClient(tc, application);
    }

    private ResourceConfig getResourceConfig(Application app) {
        return (app instanceof ResourceConfig) ? (ResourceConfig) app : new ResourceConfig(app);
    }

    /**
     * Construct a new instance with an application descriptor that defines
     * how the test container is configured.
     *
     * @param application an application describing how to configure the
     *        test container.
     * @throws TestContainerException if the default test container factory
     *         cannot be obtained, or the application descriptor is not
     *         supported by the test container factory.
     */
    public JerseyTest(Application jaxrsApplication) throws TestContainerException {
        ResourceConfig config = getResourceConfig(jaxrsApplication);
        config.addModules(new ServiceFinderModule<TestContainerFactory>(TestContainerFactory.class));
        this.application = new ApplicationHandler(config);

        this.tc = getContainer(application, getTestContainerFactory());
        this.client = getClient(tc, application);
    }

    /**
     * Programmatically enable a feature with a given name.
     * Enabling of the feature may be overridden via a system property.
     *
     * @param featureName name of the enabled feature.
     */
    protected final void enable(String featureName) {
        // TODO: perhaps we could reuse the resouce config for the test properties?
        propertyMap.put(featureName, Boolean.TRUE.toString());
    }

    /**
     * Programmatically disable a feature with a given name.
     * Disabling of the feature may be overridden via a system property.
     *
     * @param featureName name of the disabled feature.
     */
    protected final void disable(String featureName) {
        propertyMap.put(featureName, Boolean.FALSE.toString());
    }

    /**
     * Programmatically force-enable a feature with a given name.
     * Force-enabling of the feature cannot be overridden via a system property.
     * Use with care!
     *
     * @param featureName name of the force-enabled feature.
     */
    protected final void forceEnable(String featureName) {
        forcedPropertyMap.put(featureName, Boolean.TRUE.toString());
    }

    /**
     * Programmatically force-disable a feature with a given name.
     * Force-disabling of the feature cannot be overridden via a system property.
     * Use with care!
     *
     * @param featureName name of the force-disabled feature.
     */
    protected final void forceDisable(String featureName) {
        forcedPropertyMap.put(featureName, Boolean.FALSE.toString());
    }

    /**
     * Programmatically set a value of a property with a given name.
     * The property value may be overridden via a system property.
     *
     * @param propertyName name of the property.
     * @param value property value.
     */
    protected final void set(String propertyName, String value) {
        propertyMap.put(propertyName, value);
    }

    /**
     * Programmatically force-set a value of a property with a given name.
     * The force-set property value cannot be overridden via a system property.
     *
     * @param propertyName name of the property.
     * @param value property value.
     */
    protected final void forceSet(String propertyName, String value) {
        forcedPropertyMap.put(propertyName, value);
    }

    private boolean isEnabled(String featureName) {
        return Boolean.valueOf(getProperty(featureName));
    }

    private String getProperty(String propertyName) {
        if (forcedPropertyMap.containsKey(propertyName)) {
            return forcedPropertyMap.get(propertyName);
        }

        final Properties sysprops = System.getProperties();
        if (sysprops.containsKey(propertyName)) {
            return sysprops.getProperty(propertyName);
        }

        if (propertyMap.containsKey(propertyName)) {
            return propertyMap.get(propertyName);
        }

        return null;
    }

    /**
     * Return an JAX-RS application that defines how the application in the
     * test container is configured.
     * <p>
     * If a constructor is utilized that does not supply an application
     * descriptor then this method must be overridden to return an application
     * descriptor, otherwise an {@link UnsupportedOperationException} exception
     * will be thrown.
     * <p>
     * If a constructor is utilized that does supply an application descriptor
     * then this method does not require to be overridden and will not be
     * invoked.
     *
     * @return the application descriptor.
     */
    protected Application configure() {
        throw new UnsupportedOperationException(
                "The configure method must be implemented by the extending class");
    }

    /**
     * Sets the test container factory to to be used for testing.
     *
     * @param testContainerFactory the test container factory to to be used for
     *        testing.
     */
    protected final void setTestContainerFactory(TestContainerFactory testContainerFactory) {
        this.testContainerFactory = testContainerFactory;
    }

    /**
     * TODO
     * @return TODO
     * @throws TestContainerException TODO
     */
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        if (testContainerFactory == null) {
            if (testContainerFactoryClass == null) {

                Set<TestContainerFactory> testContainerFactories =
                        Providers.getProviders(application.getServices(), TestContainerFactory.class);

                final String tcfClassName = getProperty(TestProperties.CONTAINER_FACTORY);
                if ((tcfClassName == null)) {

                    if (testContainerFactories.size() >= 1) {
                        // if default factory is present, use it.
                        for (TestContainerFactory tcFactory : testContainerFactories) {

                            if (tcFactory.getClass().getName().equals(TestProperties.DEFAULT_CONTAINER_FACTORY)) {
                                LOGGER.log(
                                        Level.CONFIG,
                                        "Found multiple TestContainerFactory implementations, using default {0}",
                                        tcFactory.getClass().getName());

                                testContainerFactoryClass = tcFactory.getClass(); // is this necessary?
                                return tcFactory;
                            }
                        }

                        if (testContainerFactories.size() != 1) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Found multiple TestContainerFactory implementations, using {0}",
                                    testContainerFactories.iterator().next().getClass().getName());
                        }

                        testContainerFactoryClass = testContainerFactories.iterator().next().getClass();
                        return testContainerFactories.iterator().next();

                    }
                } else {
                    try {
                        testContainerFactoryClass = Class.forName(tcfClassName).asSubclass(TestContainerFactory.class);
                    } catch (ClassNotFoundException ex) {
                        throw new TestContainerException(
                                "The default test container factory class name, "
                                + tcfClassName
                                + ", cannot be loaded", ex);
                    } catch (ClassCastException ex) {
                        throw new TestContainerException(
                                "The default test container factory class, "
                                + tcfClassName
                                + ", is not an instance of TestContainerFactory", ex);
                    }
                }
            }

            try {
                return testContainerFactoryClass.newInstance();
            } catch (Exception ex) {
                throw new TestContainerException(
                        "The default test container factory, "
                        + testContainerFactoryClass
                        + ", could not be instantiated", ex);
            }
        }

        return testContainerFactory;
    }

    /**
     * Create a web resource whose URI refers to the base URI the Web
     * application is deployed at.
     *
     * @return the created web resource
     */
    public Target target() {
        return client.target(tc.getBaseUri());
    }

    /**
     * Get the client that is configured for this test.
     *
     * @return the configured client.
     */
    public Client client() {
        return client;
    }

    /**
     * Set up the test by invoking {@link TestContainer#start() } on
     * the test container obtained from the test container factory.
     *
     * @throws Exception TODO
     */
    @Before
    public void setUp() throws Exception {
        tc.start();
    }

    /**
     * Tear down the test by invoking {@link TestContainer#stop() } on
     * the test container obtained from the test container factory.
     *
     * @throws Exception TODO
     */
    @After
    public void tearDown() throws Exception {
        tc.stop();
    }

    private TestContainer getContainer(ApplicationHandler application, TestContainerFactory tcf) {
        if (application == null) {
            throw new IllegalArgumentException("The application cannot be null");
        }

        return tcf.create(getBaseUri(), application);
    }

    /**
     * Creates an instance of {@link Client}.
     *
     * Checks whether TestContainer provides client instance and
     * if not, ClientFactory obtained through getClientFactory()
     * will be used to create new client instance.
     *
     * This method is called exactly once when JerseyTest is created.
     *
     * @param tc instance of {@link TestContainer}
     * @param application instance of {@link JerseyApplication}
     * @return A Client instance.
     */
    protected Client getClient(TestContainer tc, ApplicationHandler application) {
        Client c = tc.getClient();

        if (c != null) {
            return c;
        } else {
            c = ClientFactory.newClient();
        }

        //check if logging is required
        if (isEnabled(TestProperties.LOG_TRAFFIC)) {
            c.configuration().register(new LoggingFilter(LOGGER, isEnabled(TestProperties.DUMP_ENTITY)));
        }

        return c;
    }

    /**
     * Returns the base URI of the application.
     * @return The base URI of the application
     */
    protected URI getBaseUri() {
        return UriBuilder.fromUri("http://localhost/").port(getPort()).build();
    }

    /**
     * Get the port to be used for test application deployments.
     *
     * @return The HTTP port of the URI
     */
    protected final int getPort() {
        final String value = System.getProperty(TestProperties.CONTAINER_PORT);
        if (value != null) {

            try {
                final int i = Integer.parseInt(value);
                if (i <= 0) {
                    throw new NumberFormatException("Value not positive.");
                }
                return i;
            } catch (NumberFormatException e) {
                LOGGER.log(Level.CONFIG,
                        "Value of " + TestProperties.CONTAINER_PORT
                        + " property is not a valid positive integer [" + value + "]."
                        + " Reverting to default [" + TestProperties.DEFAULT_CONTAINER_PORT + "].",
                        e);
            }
        }
        return TestProperties.DEFAULT_CONTAINER_PORT;
    }
}