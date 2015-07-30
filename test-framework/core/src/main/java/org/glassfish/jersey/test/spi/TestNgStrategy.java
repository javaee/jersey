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

package org.glassfish.jersey.test.spi;

import javax.ws.rs.client.Client;

/**
 * Strategy defining how test containers and clients are stored and passed to TestNG tests.
 * <p/>
 * {@link org.glassfish.jersey.test.JerseyTestNg Jersey Test} calls {@link #testContainer(TestContainer)} /
 * {@link #client(javax.ws.rs.client.Client)} methods before {@link #testContainer()} / {@link #client()}. Strategy is not
 * supposed to create instances of test container / client. It's purpose is to appropriately store given instances for different
 * TestNG approaches defined by {@code @BeforeXXX} and {@code @AfterXXX} annotations.
 *
 * @author Michal Gajdos
 */
public interface TestNgStrategy {

    /**
     * Return a test container to run the tests in. This method is called after {@link #testContainer(TestContainer)}.
     *
     * @return a test container instance or {@code null} if the test container is not set.
     */
    public TestContainer testContainer();

    /**
     * Set a new test container instance to run the tests in and return the old, previously stored, instance.
     *
     * @param testContainer new container instance.
     * @return an old container instance or {@code null} if the container is not set.
     */
    public TestContainer testContainer(final TestContainer testContainer);

    /**
     * Return a JAX-RS client. This method is called after {@link #client(javax.ws.rs.client.Client)}.
     *
     * @return a client instance or {@code null} if the client is not set.
     */
    public Client client();

    /**
     * Set a new JAX-RS client instance and return the old, previously stored, instance.
     *
     * @param client new client.
     * @return an old client instance or {@code null} if the client is not set.
     */
    public Client client(final Client client);
}
