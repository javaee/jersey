/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.internal.util.PropertiesClass;
import org.glassfish.jersey.test.spi.TestContainerFactory;

/**
 * Jersey test framework configuration properties.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@PropertiesClass
public final class TestProperties {

    /**
     * If set to {@code true} the property enables basic logging of the request and
     * response flow on both - client and server. Note that traffic logging does not
     * dump message entities by default. Please see {@link #DUMP_ENTITY} documentation
     * for instructions how to enable entity content dumping.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String LOG_TRAFFIC = "jersey.config.test.logging.enable";

    /**
     * If set to {@code true} the property instructs the test traffic logger to
     * dump message entities as part of the test traffic logging. Message entity
     * dumping is turned off by default for performance reasons. Note that the
     * value of the property will be ignored unless {@link #LOG_TRAFFIC traffic
     * logging} is enabled too.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String DUMP_ENTITY = "jersey.config.test.logging.dumpEntity";

    /**
     * Specifies the {@link TestContainerFactory test container factory} implementation
     * class to be used to create a test container instance for the test. The value
     * of the property must be a {@code String} identifying a valid, fully qualified
     * name of a test container factory implementation class, otherwise it will
     * be ignored.
     * <p/>
     * The default value is <tt>{@value #DEFAULT_CONTAINER_FACTORY}</tt>.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     *
     * @see #CONTAINER_PORT
     */
    public static final String CONTAINER_FACTORY = "jersey.config.test.container.factory";

    /**
     * Specifies the default {@link TestContainerFactory test container factory}
     * implementation class to be used to create a test container instance for the
     * test.
     *
     * @see #CONTAINER_FACTORY
     */
    public static final String DEFAULT_CONTAINER_FACTORY = "org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory";

    /**
     * Specifies the network connection port to be used by an active test container
     * for test application deployment. The value of the property must be a valid
     * positive integer, otherwise it will be ignored.
     * <p/>
     * If the value of the property is {@code 0} then first available port is used.
     * <p/>
     * The default value is <tt>{@value #DEFAULT_CONTAINER_PORT}</tt>.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     *
     * @see #CONTAINER_FACTORY
     */
    public static final String CONTAINER_PORT = "jersey.config.test.container.port";

    /**
     * Specifies the default network connection port to be used by an active test
     * container for test application deployment.
     *
     * @see #CONTAINER_PORT
     */
    public static final int DEFAULT_CONTAINER_PORT = 9998;

    /**
     * If set to a numeric value then this property enables to store log records at {@link java.util.logging.Level log level}
     * value (or higher) defined by the value of this property.
     * Log records can be retrieved in tests using {@link org.glassfish.jersey.test.JerseyTest#getLoggedRecords()}.
     * <p/>
     * This property is not supported for parallel tests.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String RECORD_LOG_LEVEL = "jersey.config.test.logging.record.level";

    /**
     * Specifies the multiplier which will be applied to timeouts for asynchronous tests. This property is useful to be
     * defined if there are problems with environment in which tests run and tests fail for timeout due to slow
     * processing.
     * <p>
     * For example if the timeout for asynchronous test is 5 seconds and this property is defined to 3
     * then the timeout final will be 15.
     * </p>
     *
     * <p/>
     * The property must be an integer value greater than 1.
     * <p/>
     * The default value is <tt>1</tt>.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String ASYNC_TIMEOUT_MULTIPLIER = "jersey.config.test.async.timeout.multiplier";

    private TestProperties() {
        // prevents instantiation
    }
}
