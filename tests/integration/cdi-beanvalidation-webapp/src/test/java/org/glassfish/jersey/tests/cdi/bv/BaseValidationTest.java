/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2016 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Common test for resource validation. The same set of tests is used
 * for the following scenarios: Grizzly based combined deployment with CDI enabled,
 * WAR based combined deployment with CDI enabled, Grizzly based deployment without CDI enabled.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class BaseValidationTest extends JerseyTest {

    public abstract String getAppPath();

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("cdi-beanvalidation-webapp").path(getAppPath()).build();
    }

    @Test
    public void testParamValidatedResourceNoParam() throws Exception {
        _testParamValidatedResourceNoParam(target());
    }

    public static void _testParamValidatedResourceNoParam(final WebTarget target) throws Exception {

        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("param").path("validate")
                .request().get(Integer.class);

        assertThat(errors, is(1));
    }

    @Test
    public void testParamValidatedResourceParamProvided() throws Exception {
        _testParamValidatedResourceParamProvided(target());
    }

    public static void _testParamValidatedResourceParamProvided(WebTarget target) throws Exception {
        Integer errors = target.register(LoggingFeature.class).path("validated").path("field").path("validate")
                .queryParam("q", "one").request().get(Integer.class);
        assertThat(errors, is(0));
    }

    @Test
    public void testFieldValidatedResourceNoParam() throws Exception {
        _testFieldValidatedResourceNoParam(target());
    }

    public static void _testFieldValidatedResourceNoParam(final WebTarget target) throws Exception {

        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("field").path("validate")
                .request().get(Integer.class);

        assertThat(errors, is(1));
    }

    @Test
    public void testFieldValidatedResourceParamProvided() throws Exception {
        _testFieldValidatedResourceParamProvided(target());
    }

    public static void _testFieldValidatedResourceParamProvided(final WebTarget target) throws Exception {
        Integer errors = target.register(LoggingFeature.class).path("validated").path("field").path("validate")
                .queryParam("q", "one").request().get(Integer.class);
        assertThat(errors, is(0));
    }

    @Test
    public void testPropertyValidatedResourceNoParam() throws Exception {
        _testPropertyValidatedResourceNoParam(target());
    }

    public static void _testPropertyValidatedResourceNoParam(final WebTarget target) throws Exception {

        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("property").path("validate")
                .request().get(Integer.class);

        assertThat(errors, is(1));
    }

    @Test
    public void testPropertyValidatedResourceParamProvided() throws Exception {
        _testPropertyValidatedResourceParamProvided(target());
    }

    public static void _testPropertyValidatedResourceParamProvided(final WebTarget target) throws Exception {
        Integer errors = target.register(LoggingFeature.class).path("validated").path("property").path("validate")
                .queryParam("q", "one").request().get(Integer.class);
        assertThat(errors, is(0));
    }

    @Test
    public void testOldFashionedResourceNoParam() {
        _testOldFashionedResourceNoParam(target());
    }

    public static void _testOldFashionedResourceNoParam(final WebTarget target) {

        Response response = target.register(LoggingFeature.class)
                .path("old").path("fashioned").path("validate")
                .request().get();

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testOldFashionedResourceParamProvided() throws Exception {
        _testOldFashionedResourceParamProvided(target());
    }

    public static void _testOldFashionedResourceParamProvided(final WebTarget target) throws Exception {
        String response = target.register(LoggingFeature.class).path("old").path("fashioned").path("validate")
                .queryParam("q", "one").request().get(String.class);
        assertThat(response, is("one"));
    }

    public static void _testNonJaxRsValidationFieldValidatedResourceNoParam(final WebTarget target) {
        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("field").path("validate").path("non-jaxrs")
                .queryParam("q", "not-important-just-to-get-this-through-jax-rs").request().get(Integer.class);

        assertThat(errors, is(1));
    }

    public static void _testNonJaxRsValidationFieldValidatedResourceParamProvided(final WebTarget target) {
        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("field").path("validate").path("non-jaxrs")
                .queryParam("q", "not-important-just-to-get-this-through-jax-rs")
                .queryParam("h", "bummer")
                .request().get(Integer.class);

        assertThat(errors, is(0));
    }
}
