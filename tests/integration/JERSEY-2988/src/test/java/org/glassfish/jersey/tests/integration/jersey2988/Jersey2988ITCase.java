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
 */

package org.glassfish.jersey.tests.integration.jersey2988;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.fail;

/**
 * JERSEY-2988 reproducer and JERSEY-2990 (duplicate of the previous one)
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class Jersey2988ITCase extends JerseyTest {

    private static final String HEADER_NAME = "x-test-header";
    private static final String HEADER_VALUE = "cool-header";

    @Override
    protected Application configure() {
        return new TestApplication();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    /**
     * Reproducer for JERSEY-2988
     *
     * @throws Exception
     */
    @Test
    public void contextFieldInjection() throws Exception {
        testCdiBeanContextInjection("field");
    }

    @Test
    public void contextSetterInjection() throws Exception {
        testCdiBeanContextInjection("setter");
    }

    private void testCdiBeanContextInjection(String path) {
        int status = target("test/" + path).request().get().getStatus();
        if (status != 200) {
            fail("@Context field is not properly injected into CDI Bean.");
        }
    }

    /**
     * Reproducer for JERSEY-2990
     *
     * @throws Exception
     */
    @Test
    public void contextFieldInjectionExceptionMapper() throws Exception {
        testExceptionMapperContextInjection("field");
    }

    @Test
    public void contextSetterExceptionMapper() throws Exception {
        testExceptionMapperContextInjection("setter");
    }

    private void testExceptionMapperContextInjection(String path) {
        Response response = target("test/ex/" + path).request().header(HEADER_NAME, HEADER_VALUE).get();
        if (response.getStatus() != 520 || !HEADER_VALUE.equals(response.getHeaderString(HEADER_NAME))) {
            fail("@Context method was not properly injected into ExceptionMapper.");
        }
    }
}
