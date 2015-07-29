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
package org.glassfish.jersey.tests.integration.servlet_request_wrapper2;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.tests.integration.servlet_request_wrapper_binding2.RequestResponseWrapperProvider;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Make sure that injected request/response instances
 * are of the types injected by {@link RequestResponseWrapperProvider}.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class AbstractRequestResponseTypeTest extends JerseyTest {

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testRequestType() throws Exception {
        final String requestType = target(getAppBasePath()).path("requestType").request().get(String.class);
        assertThat(requestType, is(equalTo(RequestResponseWrapperProvider.RequestWrapper.class.getName())));
    }

    @Test
    public void testResponseType() throws Exception {
        final String requestType = target(getAppBasePath()).path("responseType").request().get(String.class);
        assertThat(requestType, is(equalTo(RequestResponseWrapperProvider.ResponseWrapper.class.getName())));
    }

    @Test
    public void testSingletonRequestType() throws Exception {
        final String requestType = target(getAppBasePath()).path("singleton/requestType").request().get(String.class);
        assertThat(requestType, is(equalTo(RequestResponseWrapperProvider.RequestWrapper.class.getName())));
    }

    @Test
    public void testSingletonRequestAttr() throws Exception {
        for (String q : new String[] {"1", "2", "3", "95", "98", "NT", "2000", "XP", "Vista", "7", "8", "10"}) {
            _testSingletonRequestAttr("one");
        }
    }

    public void _testSingletonRequestAttr(String q) throws Exception {
        final String requestType = target(getAppBasePath()).path("singleton/request/param")
                .queryParam("q", q).request().get(String.class);
        assertThat(requestType, is(equalTo(q)));
    }

    @Test
    public void testSingletonResponseType() throws Exception {
        final String requestType = target(getAppBasePath()).path("singleton/responseType").request().get(String.class);
        assertThat(requestType, is(equalTo(RequestResponseWrapperProvider.ResponseWrapper.class.getName())));
    }

    protected abstract String getAppBasePath();
}