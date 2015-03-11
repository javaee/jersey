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
package org.glassfish.jersey.tests.cdi.resources;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for exception mapper injection.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(Parameterized.class)
public class ExceptionMappersTest extends CdiTest {

    @Parameterized.Parameters
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][] {
                {"app-field-injected"},
                {"app-ctor-injected"},
                {"request-field-injected"},
                {"request-ctor-injected"}
        });
    }

    final String resource;

    /**
     * Construct instance with the above test data injected.
     *
     * @param resource query parameter.
     */
    public ExceptionMappersTest(final String resource) {
        this.resource = resource;
    }

    /**
     * Check that for one no NPE happens on the server side, and for two
     * the injected mappers remains the same across requests.
     */
    @Test
    public void testMappersNotNull() {
        final WebTarget target = target().path(resource).path("mappers");
        final Response firstResponse = target.request().get();
        assertThat(firstResponse.getStatus(), equalTo(200));
        final String firstValue = firstResponse.readEntity(String.class);
        assertThat(target.request().get(String.class), equalTo(firstValue));
    }
}
