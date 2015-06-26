/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.cdi.resources;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.test.external.ExternalTestContainerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Test for the application scoped managed bean resource.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(Parameterized.class)
public class SingletonDependentBeanTest extends CdiTest {

    @Parameterized.Parameters
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][] {
                {"alpha", "beta"},
                {"1", "2"}
        });
    }

    final String p, x;

    /**
     * Construct instance with the above test data injected.
     *
     * @param p path parameter.
     * @param x query parameter.
     */
    public SingletonDependentBeanTest(String p, String x) {
        this.p = p;
        this.x = x;
    }

    @Test
    public void testGet() {
        final WebTarget singleton = target().path("jcdibean/dependent/singleton").path(p).queryParam("x", x);
        String s = singleton.request().get(String.class);
        assertThat(s, containsString(singleton.getUri().toString()));
        assertThat(s, containsString(String.format("p=%s", p)));
        assertThat(s, containsString(String.format("queryParam=%s", x)));
    }

    @Test
    public void testCounter() {

        final WebTarget counter = target().path("jcdibean/dependent/singleton").path(p).queryParam("x", x).path("counter");

        if (!ExternalTestContainerFactory.class.isAssignableFrom(getTestContainerFactory().getClass())) {
            // TODO: remove this workaround once JERSEY-2744 is resolved
            counter.request().put(Entity.text("10"));
        }

        String c10 = counter.request().get(String.class);
        assertThat(c10, containsString("10"));

        String c11 = counter.request().get(String.class);
        assertThat(c11, containsString("11"));

        counter.request().put(Entity.text("32"));

        String c32 = counter.request().get(String.class);
        assertThat(c32, containsString("32"));

        counter.request().put(Entity.text("10"));
    }

    @Test
    public void testException() {
        final WebTarget exception = target().path("jcdibean/dependent/singleton").path(p).queryParam("x", x).path("exception");
        assertThat(exception.request().get().readEntity(String.class), containsString("JDCIBeanDependentException"));
    }
}
