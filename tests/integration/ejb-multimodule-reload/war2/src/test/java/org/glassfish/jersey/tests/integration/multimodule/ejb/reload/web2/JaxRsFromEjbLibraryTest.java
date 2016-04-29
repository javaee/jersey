/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.multimodule.ejb.reload.web2;

import java.net.URI;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Test for EJB web application resources. The JAX-RS resources come from bundled EJB library jar.
 * Run with:
 * <pre>
 * mvn clean package
 * $AS_HOME/bin/asadmin deploy ../ear/target/ejb-multimodule-ear-*.ear
 * mvn -DskipTests=false test</pre>
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class JaxRsFromEjbLibraryTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new SecondApp();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("ejb-multimodule-war").path("resources").build();
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(LoggingFeature.class);
    }

    @Test
    public void testRequestCountGetsIncremented() {
        final int requestCount1 = _nextCount(target().path("counter"));

        final int requestCount2 = _nextCount(target().path("counter"));
        assertThat(requestCount2, is(greaterThan(requestCount1)));

        final int requestCount3 = _nextCount(target().path("stateless"));
        assertThat(requestCount3, is(greaterThan(requestCount2)));

        final int requestCount4 = _nextCount(target().path("stateless"));
        assertThat(requestCount4, is(greaterThan(requestCount3)));

        final int requestCount5 = _nextCount(target().path("stateful").path("count"));
        assertThat(requestCount5, is(greaterThan(requestCount4)));

        final int requestCount6 = _nextCount(target().path("stateful").path("count"));
        assertThat(requestCount6, is(greaterThan(requestCount5)));

        final int requestCount7 = _nextCount(target().path("war-stateless"));
        assertThat(requestCount7, is(greaterThan(requestCount6)));

        final int requestCount8 = _nextCount(target().path("war-stateless"));
        assertThat(requestCount8, is(greaterThan(requestCount7)));
    }

    private int _nextCount(final WebTarget target) throws NumberFormatException {
        final Response response = target.request().get();
        assertThat(response.getStatus(), is(200));
        return Integer.parseInt(response.readEntity(String.class));
    }

    @Test
    public void testUriInfoInjection() {
        _testPath(target().path("counter").path("one"), "counter/one");
        _testPath(target().path("counter").path("two"), "counter/two");
        _testPath(target().path("stateless").path("three"), "stateless/three");
        _testPath(target().path("stateless").path("four"), "stateless/four");
        _testPath(target().path("war-stateless").path("five"), "war-stateless/five");
        _testPath(target().path("war-stateless").path("six"), "war-stateless/six");
    }

    private void _testPath(final WebTarget target, final String expectedResult) {
        final Response response = target.request().get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), equalTo(expectedResult));
    }
}
