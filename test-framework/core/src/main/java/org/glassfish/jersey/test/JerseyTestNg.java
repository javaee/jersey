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

package org.glassfish.jersey.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestNgStrategy;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Parent class for testing JAX-RS and Jersey-based applications using Jersey test framework and TestNG framework.
 *
 * @author Michal Gajdos
 *
 * @see org.glassfish.jersey.test.JerseyTest
 * @see org.glassfish.jersey.test.spi.TestNgStrategy
 */
public abstract class JerseyTestNg extends JerseyTest {

    private TestNgStrategy strategy;

    /**
     * Initialize JerseyTestNg instance.
     *
     * This constructor can be used from an extending subclass. When this constructor is used, the extending
     * concrete subclass must implement {@link #configureStrategy()} method.
     * <p>
     * When this constructor is used, the extending concrete subclass must implement one of the
     * {@link #configure()} or {@link #configureDeployment()} methods to provide the tested application
     * configuration and deployment context.
     * </p>
     */
    public JerseyTestNg() throws TestContainerException {
        super();

        strategy = configureStrategy();
    }

    /**
     * Initialize JerseyTestNg instance and specify the test container factory to be used by this test.
     *
     * This constructor can be used from an extending subclass. When this constructor is used, the extending
     * concrete subclass must implement {@link #configureStrategy()} method.
     * <p>
     * When this constructor is used, the extending concrete subclass must implement one of the
     * {@link #configure()} or {@link #configureDeployment()} methods to provide the tested application
     * configuration and deployment context.
     * </p>
     *
     * @param testContainerFactory the test container factory to use for testing.
     */
    public JerseyTestNg(final TestContainerFactory testContainerFactory) {
        super(testContainerFactory);

        strategy = configureStrategy();
    }

    /**
     * Initialize JerseyTestNg instance.
     *
     * This constructor can be used from an extending subclass.
     * <p>
     * When this constructor is used, the extending concrete subclass the {@link #configure()}
     * or {@link #configureDeployment()} method are ignored. However it must implement
     * {@link #configureStrategy()} method.
     * </p>
     * <p>
     * Please note that when this constructor is used, recording of startup logs as well as configuring
     * other {@code JerseyTestNg} properties and features may not work properly. While using this constructor
     * should generally be avoided, in certain scenarios it may be necessary to use this constructor.
     * </p>
     *
     * @param jaxrsApplication tested application.
     */
    public JerseyTestNg(final Application jaxrsApplication) throws TestContainerException {
        super(jaxrsApplication);

        strategy = configureStrategy();
    }

    @Override
    /* package */ final TestContainer getTestContainer() {
        return strategy.testContainer();
    }

    @Override
    /* package */ final TestContainer setTestContainer(final TestContainer testContainer) {
        return strategy.testContainer(testContainer);
    }

    @Override
    protected final Client getClient() {
        return strategy.client();
    }

    @Override
    protected final Client setClient(final Client client) {
        return strategy.client(client);
    }

    /**
     * Configure {@link org.glassfish.jersey.test.spi.TestNgStrategy strategy} for this TestNG JerseyTest. The strategy defines
     * how a test container / client is stored (per class, per thread) and is also responsible for disposing stored instances.
     *
     * @return TestNG strategy instance.
     */
    protected TestNgStrategy configureStrategy() {
        throw new UnsupportedOperationException("The configureStrategy method must be implemented by the extending class");
    }

    /**
     * Parent for TestNg tests that needs to create a test container only once per a test class.
     * <p/>
     * The creation and disposal of the test container (or client) are managed by {@link #setUp()} / {@link #tearDown()} methods
     * annotated by TestNG annotations {@link BeforeClass} / {@link AfterClass}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public abstract static class ContainerPerClassTest extends JerseyTestNg {

        public ContainerPerClassTest() throws TestContainerException {
            super();
        }

        public ContainerPerClassTest(final TestContainerFactory testContainerFactory) {
            super(testContainerFactory);
        }

        public ContainerPerClassTest(final Application jaxrsApplication) throws TestContainerException {
            super(jaxrsApplication);
        }

        @BeforeClass
        @Override
        public void setUp() throws Exception {
            super.setUp();
        }

        @AfterClass
        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }

        @Override
        protected TestNgStrategy configureStrategy() {
            return new ContainerPerClassTestNgStrategy();
        }
    }

    /**
     * Parent for TestNg tests that needs to create a separate test container for each test in a test class.
     * <p/>
     * The creation and disposal of the test container (or client) are managed by {@link #setUp()} / {@link #tearDown()} methods
     * annotated by TestNG annotations {@link BeforeMethod} / {@link AfterMethod}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public abstract static class ContainerPerMethodTest extends JerseyTestNg {

        public ContainerPerMethodTest() throws TestContainerException {
            super();
        }

        public ContainerPerMethodTest(final TestContainerFactory testContainerFactory) {
            super(testContainerFactory);
        }

        public ContainerPerMethodTest(final Application jaxrsApplication) throws TestContainerException {
            super(jaxrsApplication);
        }

        @BeforeMethod
        @Override
        public void setUp() throws Exception {
            super.setUp();
        }

        @AfterMethod
        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }

        @Override
        protected TestNgStrategy configureStrategy() {
            return new ContainerPerMethodTestNgStrategy();
        }
    }
}
