/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.memleak.common;

import java.io.IOException;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.After;

/**
 * Adds support for web application testing and memory leak detection in target JVM.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class AbstractMemoryLeakWebAppTest extends JerseyTest {

    private static final String CONTEXT_ROOT = System.getProperty(MemoryLeakUtils.JERSEY_CONFIG_TEST_CONTAINER_CONTEXT_ROOT, "/");

    /**
     * Verifies no OutOfMemoryError is present in associated log file.<br/> The motivation is to have the OutOfMemory error
     * included in the JUnit test result if possible.<br/> The problem is that even of OutOfMemoryError occurred, it may not be
     * logged yet. Therefore the log file needs to be inspected when the JVM is shut down and all its resources are flushed and
     * closed.
     *
     * @throws IOException
     */
    @After
    public void verifyNoOutOfMemoryOccurred() throws IOException {
        MemoryLeakUtils.verifyNoOutOfMemoryOccurred();
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return DeploymentContext.builder(configure()).contextPath(CONTEXT_ROOT).build();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config
                .property(ClientProperties.CONNECT_TIMEOUT, 30000)
                .property(ClientProperties.READ_TIMEOUT, 30000);
    }

}
