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
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.After;
import org.junit.Before;

/**
 * Parent class for testing JAX-RS and Jersey-based applications using Jersey test framework.
 * <p>
 * At construction this class will obtain a {@link org.glassfish.jersey.test.spi.TestContainerFactory
 * test container factory} implementation.
 * </p>
 * <p>
 * Before each test method in an extending class is run the test container factory is used to obtain
 * a configured {@link org.glassfish.jersey.test.spi.TestContainer test container}.
 * Then the {@link TestContainer#start()} method is invoked on the configured test container. After each test method
 * has run, the {@link TestContainer#stop()} method is invoked on the test container. Stopped test container
 * generally shouldn't be again started for another test, rather a new test container should be created.
 * Every test method in the {@code JerseyTest} subclass can invoke the {@link #client()} to obtain a JAX-RS
 * {@link javax.ws.rs.client.Client}, from which {@link javax.ws.rs.client.WebTarget} instances can be created
 * to send arbitrary requests.
 * Also, one of the {@code target} methods ({@link #target()} or {@link #target(String)}) may be invoked to obtain
 * a JAX-RS {@link javax.ws.rs.client.WebTarget} instances from which requests can be sent to and responses
 * received from the Web application under test.
 * </p>
 * <p>
 * If a test container factory is not explicitly declared using the appropriate constructor
 * (see {@link #JerseyTest(TestContainerFactory)}) or by overriding the {@link #getTestContainerFactory()} method,
 * then a default test container factory will be obtained as follows:
 * <ol>
 * <li>
 * If a system property <tt>{@value org.glassfish.jersey.test.TestProperties#CONTAINER_FACTORY}</tt> is set
 * and the value is a fully qualified class name of a class that extends from {@code TestContainerFactory}
 * then the test container factory used by default will be an instance of that class.
 * A {@link TestContainerException} will be thrown if the class cannot be loaded or instantiated.
 * </li>
 * <li>
 * Otherwise, {@code META-INF/services} locations on the class path will be scanned for implementation providers
 * of {@code TestContainerFactory} SPI. If a single implementation is found, it will be used. If multiple implementations
 * are found, the default <tt>{@value org.glassfish.jersey.test.TestProperties#CONTAINER_FACTORY}</tt> implementation
 * will be selected if present, otherwise the first found implementation will be selected and a warning message will be logged.
 * </li>
 * <li>
 * If no {@code TestContainerFactory} has been selected in the steps above, Jersey test framework will try to
 * instantiate the default test container factory implementation (
 * <tt>{@value org.glassfish.jersey.test.TestProperties#DEFAULT_CONTAINER_FACTORY}</tt>) directly.
 * A {@link TestContainerException} will be thrown if this class cannot be loaded or instantiated.
 * </li>
 * </ol>
 * </p>
 * <p>
 * The test container is configured by a {@link DeploymentContext} that is either provided
 * by  subclass or automatically created by {@code JerseyTest} based on the provided JAX-RS / Jersey {@code Application}
 * class or instance to be tested. A {@link TestContainerException} will be thrown if the configured test container
 * factory cannot support the deployment context type.
 * Two deployment context are provided:
 * <ol>
 * <li>A basic deployment context, of type {@link DeploymentContext}, compatible with all test containers that are not
 * based on Servlet deployment model.</li>
 * <li>A Servlet-based deployment context, of type {@link ServletDeploymentContext}, compatible with Servlet-based test
 * containers.</li>
 * </ol>
 * </p>
 *
 * @author Paul Sandoz
 * @author Srinivas Bhimisetty
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Michal Gajdos
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(JerseyTest.class.getName());

    /**
     * Holds the test container factory class to be used for running the tests by default
     * (if testContainerFactory has not been set).
     * This static field is initialized in {@link #getDefaultTestContainerFactory()} method
     * and is reused by any instances of {@code JerseyTest} that are subsequently run.
     * This is done to optimize the number of TestContainerFactory service provider look-ups
     * and class loading.
     */
    private static Class<? extends TestContainerFactory> defaultTestContainerFactoryClass;

    /**
     * Configured deployment context for the tested application.
     */
    private final DeploymentContext context;

    /**
     * The test container factory which creates an instance of the test container
     * on which the tests would be run.
     */
    private TestContainerFactory testContainerFactory;

    /**
     * The test container on which the tests would be run.
     */
    private TestContainer testContainer;

    private final AtomicReference<Client> client = new AtomicReference<>(null);

    /**
     * JerseyTest property bag that can be used to configure the test behavior.
     * These properties can be overridden with a system property.
     */
    private final Map<String, String> propertyMap = new HashMap<>();

    /**
     * JerseyTest forced property bag that can be used to configure the test behavior.
     * These property cannot be overridden with a system property.
     */
    private final Map<String, String> forcedPropertyMap = new HashMap<>();

    private JerseyTestLogHandler logHandler;
    private final Map<Logger, Level> logLevelMap = new IdentityHashMap<>();

    /**
     * Initialize JerseyTest instance.
     * <p>
     * This constructor can be used from an extending subclass.
     * <p>
     * When this constructor is used, the extending concrete subclass must implement one of the
     * {@link #configure()} or {@link #configureDeployment()} methods to provide the tested application
     * configuration and deployment context.
     * </p>
     */
    public JerseyTest() {
        // Note: this must be the first call in the constructor to allow setting config
        // properties (especially around logging) in the configure() or configureDeployment()
        // method overridden in subclass, otherwise the properties set in the subclass would
        // not be set soon enough
        this.context = configureDeployment();
        this.testContainerFactory = getTestContainerFactory();
    }

    /**
     * Initialize JerseyTest instance and specify the test container factory to be used by this test.
     * <p>
     * This constructor can be used from an extending subclass.
     * <p>
     * When this constructor is used, the extending concrete subclass must implement one of the
     * {@link #configure()} or {@link #configureDeployment()} methods to provide the tested application
     * configuration and deployment context.
     * </p>
     *
     * @param testContainerFactory the test container factory to use for testing.
     */
    public JerseyTest(final TestContainerFactory testContainerFactory) {
        // Note: this must be the first call in the constructor to allow setting config
        // properties (especially around logging) in the configure() or configureDeployment()
        // method overridden in subclass, otherwise the properties set in the subclass would
        // not be set soon enough
        this.context = configureDeployment();
        this.testContainerFactory = testContainerFactory;
    }

    /**
     * Initialize JerseyTest instance.
     * <p>
     * This constructor can be used from an extending subclass.
     * <p>
     * When this constructor is used, the extending concrete subclass must implement one of the
     * {@link #configure()} or {@link #configureDeployment()} methods are ignored.
     * </p>
     * <p>
     * Please note that when this constructor is used, recording of startup logs as well as configuring
     * other {@code JerseyTest} properties and features may not work properly. While using this constructor
     * should generally be avoided, in certain scenarios it may be necessary to use this constructor.
     * (E.g. when running parameterized tests in which application is created based on test parameters
     * passed in by JUnit framework via test constructor - in such case it is not possible to propagate
     * the necessary information to one of the overridden {@code JerseyTest.configure...} methods).
     * </p>
     *
     * @param jaxrsApplication tested application.
     */
    public JerseyTest(final Application jaxrsApplication) {
        this.context = DeploymentContext.newInstance(jaxrsApplication);
        this.testContainerFactory = getTestContainerFactory();
    }

    /**
     * Return currently used test container to run the tests in. This method can be overridden.
     *
     * @return a test container instance or {@code null} if the container is not set.
     */
    /* package */ TestContainer getTestContainer() {
        return testContainer;
    }

    /**
     * Returns old test container used to run the tests in and set a new one. This method can be overridden.
     *
     * @param testContainer a test container instance or {@code null} it the current test container should be released.
     * @return old test container instance.
     */
    /* package */ TestContainer setTestContainer(final TestContainer testContainer) {
        final TestContainer old = this.testContainer;
        this.testContainer = testContainer;
        return old;
    }

    private TestContainer createTestContainer(final DeploymentContext context) {
        return getTestContainerFactory().create(getBaseUri(), context);
    }

    /**
     * Programmatically enable a feature with a given name.
     * Enabling of the feature may be overridden via a system property.
     *
     * @param featureName name of the enabled feature.
     */
    protected final void enable(final String featureName) {
        // TODO: perhaps we could reuse the resource config for the test properties?
        propertyMap.put(featureName, Boolean.TRUE.toString());
    }

    /**
     * Programmatically disable a feature with a given name.
     * Disabling of the feature may be overridden via a system property.
     *
     * @param featureName name of the disabled feature.
     */
    protected final void disable(final String featureName) {
        propertyMap.put(featureName, Boolean.FALSE.toString());
    }

    /**
     * Programmatically force-enable a feature with a given name.
     * Force-enabling of the feature cannot be overridden via a system property.
     * Use with care!
     *
     * @param featureName name of the force-enabled feature.
     */
    protected final void forceEnable(final String featureName) {
        forcedPropertyMap.put(featureName, Boolean.TRUE.toString());
    }

    /**
     * Programmatically force-disable a feature with a given name.
     * Force-disabling of the feature cannot be overridden via a system property.
     * Use with care!
     *
     * @param featureName name of the force-disabled feature.
     */
    protected final void forceDisable(final String featureName) {
        forcedPropertyMap.put(featureName, Boolean.FALSE.toString());
    }

    /**
     * Programmatically set a value of a property with a given name.
     * The property value may be overridden via a system property.
     *
     * @param propertyName name of the property.
     * @param value        property value.
     */
    protected final void set(final String propertyName, final Object value) {
        set(propertyName, value.toString());
    }

    /**
     * Programmatically set a value of a property with a given name.
     * The property value may be overridden via a system property.
     *
     * @param propertyName name of the property.
     * @param value        property value.
     */
    protected final void set(final String propertyName, final String value) {
        propertyMap.put(propertyName, value);
    }

    /**
     * Programmatically force-set a value of a property with a given name.
     * The force-set property value cannot be overridden via a system property.
     *
     * @param propertyName name of the property.
     * @param value        property value.
     */
    protected final void forceSet(final String propertyName, final String value) {
        forcedPropertyMap.put(propertyName, value);
    }

    /**
     * Check if the Jersey test boolean property (flag) has been set to {@code true}.
     *
     * @param propertyName name of the Jersey test boolean property.
     * @return {@code true} if the test property has been enabled, {@code false} otherwise.
     */
    protected final boolean isEnabled(final String propertyName) {
        return Boolean.valueOf(getProperty(propertyName));
    }

    private String getProperty(final String propertyName) {
        if (forcedPropertyMap.containsKey(propertyName)) {
            return forcedPropertyMap.get(propertyName);
        }

        final Properties systemProperties = AccessController.doPrivileged(PropertiesHelper.getSystemProperties());
        if (systemProperties.containsKey(propertyName)) {
            return systemProperties.getProperty(propertyName);
        }

        if (propertyMap.containsKey(propertyName)) {
            return propertyMap.get(propertyName);
        }

        return null;
    }

    private static String getSystemProperty(final String propertyName) {
        final Properties systemProperties = AccessController.doPrivileged(PropertiesHelper.getSystemProperties());
        return systemProperties.getProperty(propertyName);
    }

    /**
     * Create the tested JAX-RS /Jersey application.
     * <p>
     * This method may be overridden by subclasses to provide the configured JAX-RS /Jersey application to be tested.
     * The method may be also used to configure {@code JerseyTest} instance properties.
     * <p>
     * Unless {@link #configureDeployment()} method is overridden in the subclass, the {@code configure()} method is invoked
     * by {@code configureDeployment()} to create default deployment context for the tested application. As such, the method
     * is invoked in the scope of one of the {@code JerseyTest} constructors.
     * Default implementation of this method throws {@link UnsupportedOperationException}, so that construction of
     * {@code JerseyTest} instance fails unless one of the {@code configure()} or {@code configureDeployment()} methods is
     * overridden in the subclass.
     * </p>
     * <p>
     * Note that since the method is invoked from {@code JerseyTest} constructor, the overriding implementation of the method
     * must not depend on any subclass fields as those will not be initialized yet when the method is invoked.
     * </p>
     * <p>
     * Also note that in case the {@link #JerseyTest(javax.ws.rs.core.Application)} constructor is used, the method is never
     * invoked.
     * </p>
     *
     * @return tested JAX-RS /Jersey application.
     */
    protected Application configure() {
        throw new UnsupportedOperationException("The configure method must be implemented by the extending class");
    }

    /**
     * Create and configure deployment context for the tested application.
     * <p>
     * This method may be overridden by subclasses to provide custom test container deployment context for the tested
     * application. The method may be also used to configure {@code JerseyTest} instance properties.
     * <p>
     * The method is invoked from {@code JerseyTest} constructors to provide deployment context for the tested application.
     * Default implementation of this method creates
     * {@link DeploymentContext#newInstance(javax.ws.rs.core.Application) new deployment context}
     * using JAX-RS application instance obtained by calling the {@link #configure()} method.
     * </p>
     * <p>
     * Note that since the method is invoked from {@code JerseyTest} constructor, the overriding implementation of the method
     * must not depend on any subclass fields as those will not be initialized yet when the method is invoked.
     * </p>
     * <p>
     * Also note that in case the {@link #JerseyTest(javax.ws.rs.core.Application)} constructor is used, the method is never
     * invoked.
     * </p>
     *
     * @return configured deployment context for the tested application.
     * @since 2.8
     */
    protected DeploymentContext configureDeployment() {
        return DeploymentContext.builder(configure()).build();
    }

    /**
     * Return an instance of {@link TestContainerFactory} class.
     * <p>
     * <p>
     * This method is used only once during {@code JerseyTest} instance construction to retrieve the factory responsible
     * for providing {@link org.glassfish.jersey.test.spi.TestContainer} that will be used to deploy the tested application.
     * </p>
     * <p>
     * A default implementation first searches for the {@code TestContainerFactory} set via
     * {@link #JerseyTest(org.glassfish.jersey.test.spi.TestContainerFactory) constructor}, then it looks for a
     * {@code TestContainerFactory} implementation class name set via
     * <tt>{@value org.glassfish.jersey.test.TestProperties#CONTAINER_FACTORY}</tt> system property with a fallback to
     * searching for {@code TestContainerFactory} service providers on the class path. At last, if no
     * {@code TestContainerFactory} has been found, the method attempts to create new default
     * {@code TestContainerFactory} implementation instance
     * (<tt>{@value org.glassfish.jersey.test.TestProperties#DEFAULT_CONTAINER_FACTORY}</tt>).
     * </p>
     * <p>
     * Alternatively, this method may be overridden to directly provide a custom {@code TestContainerFactory} instance.
     * Note that since the method is invoked from {@code JerseyTest} constructor, the overriding implementation of the method
     * must not depend on any subclass fields as those will not be initialized yet when the method is invoked.
     * </p>
     *
     * @return an instance of {@link TestContainerFactory} class.
     * @throws TestContainerException if the initialization of {@link TestContainerFactory} instance is not successful.
     */
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        if (testContainerFactory == null) {
            testContainerFactory = getDefaultTestContainerFactory();
        }
        return testContainerFactory;
    }

    private static synchronized TestContainerFactory getDefaultTestContainerFactory() {

        if (defaultTestContainerFactoryClass == null) {
            final String factoryClassName = getSystemProperty(TestProperties.CONTAINER_FACTORY);
            if (factoryClassName != null) {
                LOGGER.log(Level.CONFIG,
                        "Loading test container factory '{0}' specified in the '{1}' system property.",
                        new Object[] {factoryClassName, TestProperties.CONTAINER_FACTORY});

                defaultTestContainerFactoryClass = loadFactoryClass(factoryClassName);
            } else {
                final TestContainerFactory[] factories = ServiceFinder.find(TestContainerFactory.class).toArray();
                if (factories.length > 0) {
                    // if there is only one factory instance, just return it
                    if (factories.length == 1) {
                        // cache the class for future reuse
                        defaultTestContainerFactoryClass = factories[0].getClass();
                        LOGGER.log(
                                Level.CONFIG,
                                "Using the single found TestContainerFactory service provider '{0}'",
                                defaultTestContainerFactoryClass.getName());
                        return factories[0];
                    }

                    // if default factory is present, use it.
                    for (final TestContainerFactory tcf : factories) {
                        if (TestProperties.DEFAULT_CONTAINER_FACTORY.equals(tcf.getClass().getName())) {
                            // cache the class for future reuse
                            defaultTestContainerFactoryClass = tcf.getClass();
                            LOGGER.log(
                                    Level.CONFIG,
                                    "Found multiple TestContainerFactory service providers, using the default found '{0}'",
                                    TestProperties.DEFAULT_CONTAINER_FACTORY);
                            return tcf;
                        }
                    }

                    // default factory is not in the list - log warning and return the first found factory instance
                    // cache the class for future reuse
                    defaultTestContainerFactoryClass = factories[0].getClass();
                    LOGGER.log(
                            Level.WARNING,
                            "Found multiple TestContainerFactory service providers, using the first found '{0}'",
                            defaultTestContainerFactoryClass.getName());
                    return factories[0];
                }

                LOGGER.log(
                        Level.CONFIG,
                        "No TestContainerFactory configured, trying to load and instantiate the default implementation '{0}'",
                        TestProperties.DEFAULT_CONTAINER_FACTORY);
                defaultTestContainerFactoryClass = loadFactoryClass(TestProperties.DEFAULT_CONTAINER_FACTORY);
            }
        }

        try {
            return defaultTestContainerFactoryClass.newInstance();
        } catch (final Exception ex) {
            throw new TestContainerException(String.format(
                    "Could not instantiate test container factory '%s'", defaultTestContainerFactoryClass.getName()), ex);
        }
    }

    private static Class<? extends TestContainerFactory> loadFactoryClass(final String factoryClassName) {
        Class<? extends TestContainerFactory> factoryClass;
        final Class<Object> loadedClass = AccessController.doPrivileged(ReflectionHelper.classForNamePA(factoryClassName, null));
        if (loadedClass == null) {
            throw new TestContainerException(String.format(
                    "Test container factory class '%s' cannot be loaded", factoryClassName));
        }
        try {
            return loadedClass.asSubclass(TestContainerFactory.class);
        } catch (final ClassCastException ex) {
            throw new TestContainerException(String.format(
                    "Class '%s' does not implement TestContainerFactory SPI.", factoryClassName), ex);
        }
    }

    /**
     * Create a JAX-RS web target whose URI refers to the {@link #getBaseUri() base URI} the tested
     * JAX-RS / Jersey application is deployed at, plus the path specified in the {@code path} argument.
     * <p>
     * This method is an equivalent of calling <tt>client().target(getBaseUri())</tt>.
     * </p>
     *
     * @return the created JAX-RS web target.
     */
    public final WebTarget target() {
        return client().target(getTestContainer().getBaseUri());
    }

    /**
     * Create a JAX-RS web target whose URI refers to the {@link #getBaseUri() base URI} the tested
     * JAX-RS / Jersey application is deployed at, plus the path specified in the {@code path} argument.
     * <p>
     * This method is an equivalent of calling {@code target().path(path)}.
     * </p>
     *
     * @param path relative path (from tested application base URI) this web target should point to.
     * @return the created JAX-RS web target.
     */
    public final WebTarget target(final String path) {
        return target().path(path);
    }

    /**
     * Get the JAX-RS test client that is {@link #configureClient(org.glassfish.jersey.client.ClientConfig) pre-configured}
     * for this test.
     *
     * @return the configured test client.
     */
    public final Client client() {
        return getClient();
    }

    /**
     * Set up the test by creating a test container instance, {@link TestContainer#start() starting} it and by creating a new
     * {@link #configureClient(org.glassfish.jersey.client.ClientConfig) pre-configured} test client.
     * The test container is obtained from the {@link #getTestContainerFactory() test container factory}.
     *
     * @throws TestContainerException if the default test container factory cannot be obtained,
     *                                or the test application deployment context is not supported
     *                                by the test container factory.
     * @throws Exception              if an exception is thrown during setting up the test environment.
     */
    @Before
    public void setUp() throws Exception {
        if (isLogRecordingEnabled()) {
            registerLogHandler();
        }

        final TestContainer testContainer = createTestContainer(context);

        // Set current instance of test container and start it.
        setTestContainer(testContainer);
        testContainer.start();

        // Create an set new client.
        setClient(getClient(testContainer.getClientConfig()));
    }

    /**
     * Tear down the test by {@link TestContainer#stop() stopping} the test container obtained from the
     * {@link #getTestContainerFactory() test container factory} and by {@link javax.ws.rs.client.Client#close() closing}
     * and discarding the {@link #configureClient(org.glassfish.jersey.client.ClientConfig) pre-configured} test client
     * that was {@link #setUp() set up} for the test.
     *
     * @throws Exception if an exception is thrown during tearing down the test environment.
     */
    @After
    public void tearDown() throws Exception {
        if (isLogRecordingEnabled()) {
            unregisterLogHandler();
        }

        try {
            TestContainer oldContainer = setTestContainer(null);
            if (oldContainer != null) {
                oldContainer.stop();
            }
        } finally {
            closeIfNotNull(setClient(null));
        }
    }

    /**
     * Get the JAX-RS test client that is {@link #configureClient(org.glassfish.jersey.client.ClientConfig) pre-configured}
     * for this test. This method can be overridden.
     *
     * @return the configured test client.
     */
    protected Client getClient() {
        return client.get();
    }

    /**
     * Get the old JAX-RS test client and set a new one. This method can be overridden.
     *
     * @param client the configured test client.
     * @return old configured test client.
     */
    protected Client setClient(final Client client) {
        return this.client.getAndSet(client);
    }

    /**
     * Create an instance of test {@link Client} using the client configuration provided by the configured
     * {@link org.glassfish.jersey.test.spi.TestContainer}.
     * <p>
     * If the {@code TestContainer} does not provide any client configuration (passed {@code clientConfig} is {@code null}),
     * the default implementation of this method first creates an empty new {@link org.glassfish.jersey.client.ClientConfig}
     * instance. The client configuration (provided by test container or created) is then passed to
     * {@link #configureClient(org.glassfish.jersey.client.ClientConfig)} which can be overridden in the {@code JerseyTest}
     * subclass to provide custom client configuration. At last, new JAX-RS {@link Client} instance is created based on the
     * resulting client configuration.
     * </p>
     *
     * @param clientConfig test client default configuration. May be {@code null}.
     * @return A Client instance.
     */
    private Client getClient(ClientConfig clientConfig) {
        if (clientConfig == null) {
            clientConfig = new ClientConfig();
        }

        //check if logging is required
        if (isEnabled(TestProperties.LOG_TRAFFIC)) {
            clientConfig.register(new LoggingFeature(LOGGER, isEnabled(TestProperties.DUMP_ENTITY)
                    ? LoggingFeature.Verbosity.PAYLOAD_ANY
                    : LoggingFeature.Verbosity.HEADERS_ONLY));

        }

        configureClient(clientConfig);

        return ClientBuilder.newClient(clientConfig);
    }

    /**
     * Configure the test client.
     * <p>
     * The method can be overridden by {@code JerseyTest} subclasses to conveniently configure the test client instance
     * used by Jersey test framework (either returned from {@link #client()} method or used to create
     * {@link javax.ws.rs.client.WebTarget} instances returned from one of the {@code target} methods
     * ({@link #target()} or {@link #target(String)}).
     * <p>
     * Prior to every test method run, a new client instance is configured and created using the client configuration
     * provided by the {@link org.glassfish.jersey.test.spi.TestContainer} as well as any internal {@code JerseyTest}
     * client configuration settings.
     * </p>
     * <p>
     * Before the actual client instance creation, Jersey test framework invokes this method in order to allow the subclasses
     * to further customize created client instance.
     * </p>
     * <p>
     * After each test method is run, the existing client instance is {@link javax.ws.rs.client.Client#close() closed}
     * and discarded.
     * </p>
     * <p>
     * Default implementation of the method is "no-op".
     * </p>
     *
     * @param config Jersey test client configuration that can be modified before the client is created.
     */
    protected void configureClient(final ClientConfig config) {
        // do nothing
    }

    /**
     * Returns the base URI of the tested application.
     *
     * @return the base URI of the tested application.
     */
    // TODO make final
    protected URI getBaseUri() {
        final TestContainer container = getTestContainer();

        if (container != null) {
            // called from outside of JerseyTest constructor
            return container.getBaseUri();
        }

        // called from within JerseyTest constructor
        return UriBuilder.fromUri("http://localhost/").port(getPort()).build();
    }

    /**
     * Get the port to be used for test application deployments.
     *
     * @return The HTTP port of the URI
     */
    protected final int getPort() {
        final TestContainer container = getTestContainer();

        if (container != null) {
            // called from outside of JerseyTest constructor
            return container.getBaseUri().getPort();
        }

        // called from within JerseyTest constructor
        final String value = getProperty(TestProperties.CONTAINER_PORT);
        if (value != null) {

            try {
                final int i = Integer.parseInt(value);
                if (i < 0) {
                    throw new NumberFormatException("Value not positive.");
                }
                return i;
            } catch (final NumberFormatException e) {
                LOGGER.log(Level.CONFIG,
                        "Value of " + TestProperties.CONTAINER_PORT
                                + " property is not a valid positive integer [" + value + "]."
                                + " Reverting to default [" + TestProperties.DEFAULT_CONTAINER_PORT + "].",
                        e
                );
            }
        }
        return TestProperties.DEFAULT_CONTAINER_PORT;
    }

    /**
     * Get stored {@link LogRecord log records} if enabled by setting {@link TestProperties#RECORD_LOG_LEVEL} or an empty list.
     *
     * @return list of log records or an empty list.
     */
    protected final List<LogRecord> getLoggedRecords() {
        return getLogHandler().getRecords();
    }

    /**
     * Get last stored {@link LogRecord log record} if enabled by setting {@link TestProperties#RECORD_LOG_LEVEL}
     * or {@code null}.
     *
     * @return last stored {@link LogRecord log record} or {@code null}.
     */
    protected final LogRecord getLastLoggedRecord() {
        final List<LogRecord> loggedRecords = getLoggedRecords();
        return loggedRecords.isEmpty() ? null : loggedRecords.get(loggedRecords.size() - 1);
    }

    /**
     * Retrieves a list of root loggers.
     *
     * @return list of root loggers.
     */
    private Set<Logger> getRootLoggers() {
        final LogManager logManager = LogManager.getLogManager();
        final Enumeration<String> loggerNames = logManager.getLoggerNames();

        final Set<Logger> rootLoggers = new HashSet<>();

        while (loggerNames.hasMoreElements()) {
            Logger logger = logManager.getLogger(loggerNames.nextElement());
            if (logger != null) {
                while (logger.getParent() != null) {
                    logger = logger.getParent();
                }
                rootLoggers.add(logger);
            }
        }

        return rootLoggers;
    }

    /**
     * Register {@link Handler log handler} to the list of root loggers.
     */
    private void registerLogHandler() {
        final String recordLogLevel = getProperty(TestProperties.RECORD_LOG_LEVEL);
        final int recordLogLevelInt = Integer.valueOf(recordLogLevel);
        final Level level = Level.parse(recordLogLevel);

        logLevelMap.clear();

        for (final Logger root : getRootLoggers()) {
            logLevelMap.put(root, root.getLevel());

            if (root.getLevel().intValue() > recordLogLevelInt) {
                root.setLevel(level);
            }

            root.addHandler(getLogHandler());
        }
    }

    /**
     * Un-register {@link Handler log handler} from the list of root loggers.
     */
    private void unregisterLogHandler() {
        for (final Logger root : getRootLoggers()) {
            root.setLevel(logLevelMap.get(root));
            root.removeHandler(getLogHandler());
        }
        logHandler = null;
    }

    /**
     * Return {@code true} if log recoding is enabled.
     *
     * @return {@code true} if log recoding is enabled, {@code false} otherwise.
     */
    private boolean isLogRecordingEnabled() {
        return getProperty(TestProperties.RECORD_LOG_LEVEL) != null;
    }

    /**
     * Retrieves {@link Handler log handler} capable of storing {@link LogRecord logged records}.
     *
     * @return log handler.
     */
    private JerseyTestLogHandler getLogHandler() {
        if (logHandler == null) {
            logHandler = new JerseyTestLogHandler();
        }
        return logHandler;
    }

    /**
     * Returns {@link TestProperties#ASYNC_TIMEOUT_MULTIPLIER} or {@code 1} if the property is not defined.
     *
     * @return Multiplier of the async timeout for async test.
     */
    protected int getAsyncTimeoutMultiplier() {
        final String property = getProperty(TestProperties.ASYNC_TIMEOUT_MULTIPLIER);
        Integer multi = 1;
        if (property != null) {
            multi = Integer.valueOf(property);
            if (multi <= 0) {
                throw new NumberFormatException(
                        "Property " + TestProperties.ASYNC_TIMEOUT_MULTIPLIER + " must be a number greater than 0.");
            }
        }
        return multi;

    }

    /**
     * Utility method that safely closes a response without throwing an exception.
     *
     * @param responses responses to close. Each response may be {@code null}.
     * @since 2.5
     */
    public final void close(final Response... responses) {
        if (responses == null || responses.length == 0) {
            return;
        }

        for (final Response response : responses) {
            if (response == null) {
                continue;
            }
            try {
                response.close();
            } catch (final Throwable t) {
                LOGGER.log(Level.WARNING, "Error closing a response.", t);
            }
        }
    }

    /**
     * Utility method that safely closes a client instance without throwing an exception.
     *
     * @param clients client instances to close. Each instance may be {@code null}.
     * @since 2.5
     */
    public static void closeIfNotNull(final Client... clients) {
        if (clients == null || clients.length == 0) {
            return;
        }

        for (final Client c : clients) {
            if (c == null) {
                continue;
            }
            try {
                c.close();
            } catch (final Throwable t) {
                LOGGER.log(Level.WARNING, "Error closing a client instance.", t);
            }

        }
    }

    /**
     * Custom logging handler used to store log records produces during an invocation of a test.
     */
    private class JerseyTestLogHandler extends Handler {

        private final int logLevel;
        private final List<LogRecord> records;

        private JerseyTestLogHandler() {
            this.logLevel = Integer.parseInt(getProperty(TestProperties.RECORD_LOG_LEVEL));
            this.records = new ArrayList<>();
        }

        @Override
        public void publish(final LogRecord record) {
            final String loggerName = record.getLoggerName();

            if (record.getLevel().intValue() >= logLevel
                    && loggerName.startsWith("org.glassfish.jersey")
                    && !loggerName.startsWith("org.glassfish.jersey.test")) {
                records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        public List<LogRecord> getRecords() {
            return records;
        }
    }
}
