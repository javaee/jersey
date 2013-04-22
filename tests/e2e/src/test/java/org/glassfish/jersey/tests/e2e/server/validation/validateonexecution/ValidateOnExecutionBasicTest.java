/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
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

    public static abstract class ValidateExecutableOnType {

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

    public static abstract class ValidateGetterExecutable {

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
                ValidateGetterExecutableOnTypeMatch.class)
                .property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }

    @Test
    public void testOnTypeValidateInputFailValidateExecutableDefault() throws Exception {
        _testOnType("default", 15, 400);
    }

    @Test
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
        assertThat(response.getStatus(), equalTo(204));

        response = target.path("sanity").request().get();
        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("ok"));
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

        assertThat(target.request().get().getStatus(), equalTo(500));
        assertThat(target.path("sanity").request().get().getStatus(), equalTo(200));
    }

    @Test
    public void testOnTypeGetterMiss() throws Exception {
        final WebTarget target = target("getter-on-type-miss");

        Response response = target.request().get();
        assertThat(response.getStatus(), equalTo(204));

        response = target.path("sanity").request().get();
        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("ok"));
    }

    @Test
    public void testOnTypeGetterMatch() throws Exception {
        final WebTarget target = target("getter-on-type-match");

        assertThat(target.request().get().getStatus(), equalTo(400));
        assertThat(target.path("sanity").request().get().getStatus(), equalTo(400));
    }
}
