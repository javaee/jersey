/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.validation;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Michal Gajdos
 */
public class InheritanceValidationTest extends JerseyTest {

    public static interface ResourceNumberInterface<T extends Number> {

        @POST
        @Min(0)
        @NotNull
        public T post(@NotNull @Max(100) final T value);
    }

    public static interface ResourceStringInterface {

        @Min(-50)
        public String post(@Max(50) final String value);
    }

    @Path("/")
    public static class ResourceNumberString implements ResourceNumberInterface<Integer>, ResourceStringInterface {

        @Override
        public Integer post(final Integer value) {
            return value;
        }

        @POST
        @Path("string")
        @Override
        public String post(final String value) {
            return value;
        }
    }

    @Path("/sub")
    public static class SubClassResourceNumberString extends ResourceNumberString {

        @Override
        public Integer post(final Integer value) {
            return value;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ResourceNumberString.class, SubClassResourceNumberString.class);
    }

    @Test
    public void testValidateNumberPositive() throws Exception {
        _test(75, 200);
    }

    @Test
    public void testValidateNumberInputNegative() throws Exception {
        _test(150, 400);
    }

    @Test
    public void testValidateStringPositive() throws Exception {
        _test("string", "25", 200);
    }

    @Test
    public void testValidateStringInputNegative() throws Exception {
        _test("string", "150", 400);
    }

    @Test
    public void testValidateNumberSubClassPositive() throws Exception {
        _test("sub", 75, 200);
    }

    @Test
    public void testValidateNumberInputSubClassNegative() throws Exception {
        _test("sub", 150, 400);
    }

    @Test
    public void testValidateStringSubClassPositive() throws Exception {
        _test("sub/string", "25", 200);
    }

    @Test
    public void testValidateStringInputSubClassNegative() throws Exception {
        _test("sub/string", "150", 400);
    }

    @Test
    public void testValidateNumberResponseNegative() throws Exception {
        _test(-150, 500);
    }

    @Test
    public void testValidateStringResponseNegative() throws Exception {
        _test("string", "-150", 500);
    }

    @Test
    public void testValidateNumberResponseSubClassNegative() throws Exception {
        _test("sub", -150, 500);
    }

    @Test
    public void testValidateStringResponseSubClassNegative() throws Exception {
        _test("sub/string", "-150", 500);
    }

    private void _test(final Object value, final int responseStatus) {
        _test("", value, responseStatus);
    }

    private void _test(final String path, final Object value, final int responseStatus) {
        final Response response = target(path).request().post(Entity.text(value));

        assertThat("Wrong response.", response.getStatus(), equalTo(responseStatus));

        if (responseStatus == 200) {
            assertThat("Invalid entity.", response.readEntity(value.getClass()), equalTo(value));
        }
    }
}
