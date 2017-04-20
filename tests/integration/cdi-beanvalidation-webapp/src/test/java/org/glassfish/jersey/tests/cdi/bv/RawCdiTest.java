/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.bv;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;

import org.jboss.weld.environment.se.Weld;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;


/**
 * Validation result test for CDI environment.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class RawCdiTest extends BaseValidationTest {

    Weld weld;

    @Before
    public void setup() {
        Assume.assumeTrue(Hk2InjectionManagerFactory.isImmediateStrategy());
    }

    @Override
    public void setUp() throws Exception {
        if (Hk2InjectionManagerFactory.isImmediateStrategy()) {
            if (!ExternalTestContainerFactory.class.isAssignableFrom(getTestContainerFactory().getClass())) {
                weld = new Weld();
                weld.initialize();
            }
            super.setUp();
        }
    }

    @Override
    public void tearDown() throws Exception {
        if (Hk2InjectionManagerFactory.isImmediateStrategy()) {
            if (!ExternalTestContainerFactory.class.isAssignableFrom(getTestContainerFactory().getClass())) {
                weld.shutdown();
            }
            super.tearDown();
        }
    }

    @Override
    protected Application configure() {
        return ResourceConfig.forApplicationClass(CdiApplication.class);
    }

    @Override
    public String getAppPath() {
        return "cdi";
    }

    @Test
    public void testNonJaxRsValidationFieldValidatedResourceNoParam() {
        BaseValidationTest._testNonJaxRsValidationFieldValidatedResourceNoParam(target());
    }

    @Test
    public void testNonJaxRsValidationFieldValidatedResourceParamProvided() {
        BaseValidationTest._testNonJaxRsValidationFieldValidatedResourceParamProvided(target());
    }
}
