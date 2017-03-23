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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;
import org.glassfish.jersey.test.util.runner.RunSeparately;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Michal Gajdos
 */
@RunWith(ConcurrentRunner.class)
public class ValidateOnExecutionBasicTest extends ValidateOnExecutionAbstractTest {

    /**
     * On METHOD.
     */

    @Path("on-method")
    public static class ValidateExecutableOnMethodsResource {

        @POST
        @Path("validateExecutableDefault")
        @ValidateOnExecution
        @Min(0)
        public Integer validateExecutableDefault(@Max(10) final Integer value) {
            return value;
        }

        @POST
        @Path("validateExecutableMatch")
        @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
        @Min(0)
        public Integer validateExecutableMatch(@Max(10) final Integer value) {
            return value;
        }

        @POST
        @Path("validateExecutableMiss")
        @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
        @Min(0)
        public Integer validateExecutableMiss(@Max(10) final Integer value) {
            return value;
        }

        @POST
        @Path("validateExecutableNone")
        @ValidateOnExecution(type = ExecutableType.NONE)
        @Min(0)
        public Integer validateExecutableNone(@Max(10) final Integer value) {
            return value;
        }
    }

    /**
     * On TYPE.
     */

    public abstract static class ValidateExecutableOnType {

        @POST
        @Min(0)
        public Integer validateExecutable(@Max(10) final Integer value) {
            return value;
        }
    }

    @Path("on-type-default")
    @ValidateOnExecution
    public static class ValidateExecutableOnTypeDefault extends ValidateExecutableOnType {
    }

    @Path("on-type-match")
    @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
    public static class ValidateExecutableOnTypeMatch extends ValidateExecutableOnType {
    }

    @Path("on-type-miss")
    @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
    public static class ValidateExecutableOnTypeMiss extends ValidateExecutableOnType {
    }

    @Path("on-type-none")
    @ValidateOnExecution(type = ExecutableType.NONE)
    public static class ValidateExecutableOnTypeNone extends ValidateExecutableOnType {
    }

    /**
     * MIXED.
     */

    @Path("mixed-default")
    @ValidateOnExecution(type = ExecutableType.NONE)
    public static class ValidateExecutableMixedDefault {

        @POST
        @Min(0)
        @ValidateOnExecution
        public Integer validateExecutable(@Max(10) final Integer value) {
            return value;
        }
    }

    @Path("mixed-none")
    @ValidateOnExecution
    public static class ValidateExecutableMixedNone {

        @POST
        @Min(0)
        @ValidateOnExecution(type = ExecutableType.NONE)
        public Integer validateExecutable(@Max(10) final Integer value) {
            return value;
        }
    }

    /**
     * GETTERS.
     */

    public abstract static class ValidateGetterExecutable {

        @GET
        @Path("sanity")
        public String getSanity() {
            return "ok";
        }
    }

    @Path("getter-on-method-default")
    public static class ValidateGetterExecutableOnMethodDefault extends ValidateGetterExecutable {

        @GET
        @ValidateOnExecution
        @NotNull
        public String getDefault() {
            return null;
        }
    }

    @Path("getter-on-method-miss")
    public static class ValidateGetterExecutableOnMethodMiss extends ValidateGetterExecutable {

        @GET
        @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
        @NotNull
        public String getMiss() {
            return null;
        }
    }

    @Path("getter-on-method-match")
    public static class ValidateGetterExecutableOnMethodMatch extends ValidateGetterExecutable {

        @GET
        @ValidateOnExecution(type = {ExecutableType.NON_GETTER_METHODS, ExecutableType.GETTER_METHODS})
        @NotNull
        public String getMatch() {
            return null;
        }
    }

    @Path("getter-on-type-default")
    @ValidateOnExecution
    public static class ValidateGetterExecutableOnTypeDefault extends ValidateGetterExecutable {

        @GET
        @NotNull
        public String getDefault() {
            return null;
        }
    }

    @Path("getter-on-type-miss")
    @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
    public static class ValidateGetterExecutableOnTypeMiss extends ValidateGetterExecutable {

        @GET
        @NotNull
        public String getMiss() {
            return null;
        }
    }

    @Path("getter-on-type-match")
    @ValidateOnExecution(type = {ExecutableType.NON_GETTER_METHODS, ExecutableType.GETTER_METHODS})
    public static class ValidateGetterExecutableOnTypeMatch extends ValidateGetterExecutable {

        @GET
        @NotNull
        public String getMatch() {
            return null;
        }
    }

    /**
     * BEANS.
     */

    @Path("getter-on-beans")
    public static class ValidateGetterExecutableOnBeans {

        @POST
        @Valid
        public AnotherContactBean post(@Valid final AnotherContactBean bean) {
            return bean;
        }

        @POST
        @Path("invalidMail")
        @Valid
        public AnotherContactBean invalidMail(@Valid final AnotherContactBean bean) {
            bean.setEmail("ab");
            return bean;
        }

        @POST
        @Path("invalidPhone")
        @Valid
        public AnotherContactBean invalidPhone(@Valid final AnotherContactBean bean) {
            bean.setPhone("12");
            return bean;
        }
    }

    @Path("getter-resource-method")
    @Singleton
    public static class ValidateGetterResourceMethod {

        private int count = 1;

        @GET
        @Max(1)
        public int getValue() {
            return count++;
        }
    }

    @Path("on-type-getter-null")
    @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
    public static class ValidateExecutableResource {

        @Path("nogetter")
        @GET
        @NotNull
        public String daNull() {
            return null;
        }

        @Path("getter")
        @GET
        @NotNull
        public String getNull() {
            return null;
        }
    }

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new ResourceConfig(ValidateExecutableOnMethodsResource.class,
                ValidateExecutableOnTypeDefault.class,
                ValidateExecutableOnTypeMatch.class,
                ValidateExecutableOnTypeMiss.class,
                ValidateExecutableOnTypeNone.class,
                ValidateExecutableMixedDefault.class,
                ValidateExecutableMixedNone.class,
                ValidateGetterExecutableOnMethodDefault.class,
                ValidateGetterExecutableOnMethodMiss.class,
                ValidateGetterExecutableOnMethodMatch.class,
                ValidateGetterExecutableOnTypeDefault.class,
                ValidateGetterExecutableOnTypeMiss.class,
                ValidateGetterExecutableOnTypeMatch.class,
                ValidateGetterExecutableOnBeans.class,
                ValidateGetterResourceMethod.class,
                ValidateExecutableResource.class)
                .property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }

    @Test
    public void testOnTypeValidateInputFailValidateExecutableDefault() throws Exception {
        _testOnType("default", 15, 400);
    }

    @Test
    @RunSeparately
    public void testOnTypeValidateResultFailValidateExecutableDefault() throws Exception {
        _testOnType("default", -15, 500);
    }

    @Test
    public void testOnMethodGetterDefault() throws Exception {
        final WebTarget target = target("getter-on-method-default");

        assertThat(target.request().get().getStatus(), equalTo(400));
        assertThat(target.path("sanity").request().get().getStatus(), equalTo(400));
    }

    @Test
    public void testOnMethodGetterMiss() throws Exception {
        final WebTarget target = target("getter-on-method-miss");

        Response response = target.request().get();
        assertThat(response.getStatus(), equalTo(400));

        response = target.path("sanity").request().get();
        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testOnMethodGetterMatch() throws Exception {
        final WebTarget target = target("getter-on-method-match");

        assertThat(target.request().get().getStatus(), equalTo(400));
        assertThat(target.path("sanity").request().get().getStatus(), equalTo(400));
    }

    @Test
    public void testOnTypeGetterDefault() throws Exception {
        final WebTarget target = target("getter-on-type-default");

        assertThat(target.request().get().getStatus(), equalTo(400));
        assertThat(target.path("sanity").request().get().getStatus(), equalTo(400));
    }

    @Test
    public void testOnTypeGetterMiss() throws Exception {
        final WebTarget target = target("getter-on-type-miss");

        Response response = target.request().get();
        assertThat(response.getStatus(), equalTo(400));

        response = target.path("sanity").request().get();
        assertThat(response.getStatus(), equalTo(400));
    }

    /**
     * Validation should fail when getter (also a resource method) is invoked and not when the resource class is validated.
     */
    @Test
    public void testGetterResourceMethod() throws Exception {
        final WebTarget target = target("getter-resource-method");
        final Response response = target.request().get();

        assertThat(response.getStatus(), equalTo(500));
    }

    @Test
    public void testOnTypeGetterNull() throws Exception {
        final WebTarget target = target("on-type-getter-null");

        Response response = target.path("nogetter").request().get();
        assertThat(response.getStatus(), equalTo(400));

        response = target.path("getter").request().get();
        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testOnTypeGetterMatch() throws Exception {
        final WebTarget target = target("getter-on-type-match");

        assertThat(target.request().get().getStatus(), equalTo(400));
        assertThat(target.path("sanity").request().get().getStatus(), equalTo(400));
    }

    @Test
    public void testBeansPositive() throws Exception {
        final WebTarget target = target("getter-on-beans");
        final AnotherContactBean contactBean = new AnotherContactBean("jersey@example.com", null, "Jersey JAX-RS", null);

        final Response response = target.request().post(Entity.xml(contactBean));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(AnotherContactBean.class), equalTo(contactBean));
    }

    @Test
    public void testBeansValidateGetterInvalidEmail() throws Exception {
        final WebTarget target = target("getter-on-beans");
        final AnotherContactBean contactBean = new AnotherContactBean("jersey", null, "Jersey JAX-RS", null);

        final Response response = target.request().post(Entity.xml(contactBean));

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testBeansValidateGetterInvalidPhone() throws Exception {
        final WebTarget target = target("getter-on-beans");
        final AnotherContactBean contactBean = new AnotherContactBean("jersey@example.com", "12", "Jersey JAX-RS", null);

        final Response response = target.request().post(Entity.xml(contactBean));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(AnotherContactBean.class), equalTo(contactBean));
    }

    @Test
    public void testBeansValidateGetterInvalidReturnMail() throws Exception {
        final WebTarget target = target("getter-on-beans").path("invalidMail");
        final AnotherContactBean contactBean = new AnotherContactBean("jersey@example.com", null, "Jersey JAX-RS", null);

        final Response response = target.request().post(Entity.xml(contactBean));

        assertThat(response.getStatus(), equalTo(500));
    }

    @Test
    public void testBeansValidateGetterInvalidReturnPhone() throws Exception {
        final WebTarget target = target("getter-on-beans").path("invalidPhone");
        final AnotherContactBean contactBean = new AnotherContactBean("jersey@example.com", null, "Jersey JAX-RS", null);

        final Response response = target.request().post(Entity.xml(contactBean));
        contactBean.setPhone("12");

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(AnotherContactBean.class), equalTo(contactBean));
    }
}
