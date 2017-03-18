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

package org.glassfish.jersey.tests.e2e.server.validation.validateonexecution;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.util.runner.RunSeparately;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Michal Gajdos
 */
public abstract class ValidateOnExecutionAbstractTest extends JerseyTest {

    @Test
    public void testOnMethodValidateInputPassValidateExecutableDefault() throws Exception {
        _testOnMethod("validateExecutableDefault", 0, 200);
    }

    @Test
    public void testOnMethodValidateInputFailValidateExecutableDefault() throws Exception {
        _testOnMethod("validateExecutableDefault", 15, 400);
    }

    @Test
    public void testOnMethodValidateInputPassValidateExecutableMatch() throws Exception {
        _testOnMethod("validateExecutableMatch", 0, 200);
    }

    @Test
    public void testOnMethodValidateInputFailValidateExecutableMatch() throws Exception {
        _testOnMethod("validateExecutableMatch", 15, 400);
    }

    @Test
    public void testOnMethodValidateInputPassValidateExecutableMiss() throws Exception {
        _testOnMethod("validateExecutableMiss", 0, 200);
    }

    @Test
    public void testOnMethodValidateInputPassBiggerValidateExecutableMiss() throws Exception {
        _testOnMethod("validateExecutableMiss", 15, 400);
    }

    @Test
    public void testOnMethodValidateInputPassValidateExecutableNone() throws Exception {
        _testOnMethod("validateExecutableNone", 0, 200);
    }

    @Test
    public void testOnMethodValidateInputPassBiggerValidateExecutableNone() throws Exception {
        _testOnMethod("validateExecutableNone", 15, 400);
    }

    @Test
    public void testOnMethodValidateResultPassValidateExecutableDefault() throws Exception {
        _testOnMethod("validateExecutableDefault", 0, 200);
    }

    @Test
    public void testOnMethodValidateResultFailValidateExecutableDefault() throws Exception {
        _testOnMethod("validateExecutableDefault", -15, 500);
    }

    @Test
    public void testOnMethodValidateResultPassValidateExecutableMatch() throws Exception {
        _testOnMethod("validateExecutableMatch", 0, 200);
    }

    @Test
    public void testOnMethodValidateResultFailValidateExecutableMatch() throws Exception {
        _testOnMethod("validateExecutableMatch", -15, 500);
    }

    @Test
    public void testOnMethodValidateResultPassValidateExecutableMiss() throws Exception {
        _testOnMethod("validateExecutableMiss", 0, 200);
    }

    @Test
    public void testOnMethodValidateResultPassBiggerValidateExecutableMiss() throws Exception {
        _testOnMethod("validateExecutableMiss", -15, 200);
    }

    @Test
    public void testOnMethodValidateResultPassValidateExecutableNone() throws Exception {
        _testOnMethod("validateExecutableNone", 0, 200);
    }

    @Test
    public void testOnMethodValidateResultPassBiggerValidateExecutableNone() throws Exception {
        _testOnMethod("validateExecutableNone", -15, 200);
    }

    @Test
    public void testOnTypeValidateInputPassValidateExecutableDefault() throws Exception {
        _testOnType("default", 0, 200);
    }

    @Test
    public void testOnTypeValidateInputPassValidateExecutableMatch() throws Exception {
        _testOnType("match", 0, 200);
    }

    @Test
    public void testOnTypeValidateInputFailValidateExecutableMatch() throws Exception {
        _testOnType("match", 15, 400);
    }

    @Test
    public void testOnTypeValidateInputPassValidateExecutableMiss() throws Exception {
        _testOnType("miss", 0, 200);
    }

    @Test
    public void testOnTypeValidateInputPassBiggerValidateExecutableMiss() throws Exception {
        _testOnType("miss", 15, 400);
    }

    @Test
    public void testOnTypeValidateInputPassValidateExecutableNone() throws Exception {
        _testOnType("none", 0, 200);
    }

    @Test
    public void testOnTypeValidateInputPassBiggerValidateExecutableNone() throws Exception {
        _testOnType("none", 15, 400);
    }

    @Test
    public void testOnTypeValidateResultPassValidateExecutableDefault() throws Exception {
        _testOnType("default", 0, 200);
    }

    @Test
    public void testOnTypeValidateResultPassValidateExecutableMatch() throws Exception {
        _testOnType("match", 0, 200);
    }

    @Test
    @RunSeparately
    public void testOnTypeValidateResultFailValidateExecutableMatch() throws Exception {
        _testOnType("match", -15, 500);
    }

    @Test
    public void testOnTypeValidateResultPassValidateExecutableMiss() throws Exception {
        _testOnType("miss", 0, 200);
    }

    @Test
    @RunSeparately
    public void testOnTypeValidateResultPassBiggerValidateExecutableMiss() throws Exception {
        _testOnType("miss", -15, 200);
    }

    @Test
    public void testOnTypeValidateResultPassValidateExecutableNone() throws Exception {
        _testOnType("none", 0, 200);
    }

    @Test
    @RunSeparately
    public void testOnTypeValidateResultPassBiggerValidateExecutableNone() throws Exception {
        _testOnType("none", -15, 200);
    }

    @Test
    public void testMixedValidatePassDefault() throws Exception {
        _test("mixed-default", 0, 200);
    }

    @Test
    public void testMixedValidateInputFailDefault() throws Exception {
        _test("mixed-default", 15, 400);
    }

    @Test
    public void testMixedValidateResultFailDefault() throws Exception {
        _test("mixed-default", -15, 500);
    }

    @Test
    public void testMixedValidatePassNone() throws Exception {
        _test("mixed-none", 0, 200);
    }

    @Test
    public void testMixedValidateInputPassNone() throws Exception {
        _test("mixed-none", 15, 400);
    }

    @Test
    public void testMixedValidateResultPassNone() throws Exception {
        _test("mixed-none", -15, 200);
    }

    void _testOnMethod(final String path, final Integer value, final int returnStatus) throws Exception {
        _test("on-method/" + path, value, returnStatus);
    }

    void _testOnType(final String path, final Integer value, final int returnStatus) throws Exception {
        _test("on-type-" + path, value, returnStatus);
    }

    void _test(final String path, final Integer value, final int returnStatus) throws Exception {
        final Response response = target(path)
                .request()
                .post(Entity.text(value));

        assertEquals(returnStatus, response.getStatus());

        if (returnStatus == 200) {
            assertEquals(value, response.readEntity(Integer.class));
        }
    }
}
