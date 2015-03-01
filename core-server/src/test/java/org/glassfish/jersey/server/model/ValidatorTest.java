/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.Suspended;

import javax.inject.Singleton;

import org.glassfish.jersey.Severity;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerLocatorFactory;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.internal.ModelErrors;

import org.glassfish.hk2.api.PerLookup;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Taken from Jersey 1: jersey-server:com.sun.jersey.server.impl.modelapi.validation.ResourceModelValidatorTest.java
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ValidatorTest {
    private static final Logger LOGGER = Logger.getLogger(ValidatorTest.class.getName());


    @Path("rootNonAmbigCtors")
    public static class TestRootResourceNonAmbigCtors {

        // TODO: hmmm, even if this is not ambiguous, it is strange; shall we warn, the 1st and the 2nd ctor won't be used?
        public TestRootResourceNonAmbigCtors(@QueryParam("s") String s) {
        }

        public TestRootResourceNonAmbigCtors(@QueryParam("n") int n) {
        }

        public TestRootResourceNonAmbigCtors(@QueryParam("s") String s, @QueryParam("n") int n) {
        }

        @GET
        public String getIt() {
            return "it";
        }
    }

    @Test
    public void testRootResourceNonAmbigConstructors() throws Exception {
        LOGGER.info("No issue should be reported if more public ctors exists with the same number of params, "
                + "but another just one is presented with more params at a root resource:");
        Resource resource = Resource.builder(TestRootResourceNonAmbigCtors.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);
        assertTrue(validator.getIssueList().isEmpty());
    }


    public static class MyBeanParam {
        @HeaderParam("h")
        String hParam;
    }


    @Singleton
    @Path("rootSingleton/{p}")
    public static class TestCantInjectFieldsForSingleton {

        @MatrixParam("m")
        String matrixParam;
        @QueryParam("q")
        String queryParam;
        @PathParam("p")
        String pParam;
        @CookieParam("c")
        String cParam;
        @HeaderParam("h")
        String hParam;
        @BeanParam
        MyBeanParam beanParam;

        @GET
        public String getIt() {
            return "it";
        }
    }

    public static interface ChildOfContainerRequestFilter extends ContainerRequestFilter {
    }

    @Path("rootSingleton/{p}")
    public static class TestCantInjectFieldsForProvider implements ChildOfContainerRequestFilter {

        @MatrixParam("m")
        String matrixParam;
        @QueryParam("q")
        String queryParam;
        @PathParam("p")
        String pParam;
        @CookieParam("c")
        String cParam;
        @HeaderParam("h")
        String hParam;
        @BeanParam
        MyBeanParam beanParam;


        @GET
        public String getIt() {
            return "it";
        }

        @Override
        public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        }
    }


    @Singleton
    @Path("rootSingletonConstructorB/{p}")
    public static class TestCantInjectConstructorParamsForSingleton {
        public TestCantInjectConstructorParamsForSingleton() {
        }

        public TestCantInjectConstructorParamsForSingleton(@QueryParam("q") String queryParam) {
        }

        @GET
        public String getIt() {
            return "it";
        }
    }

    @Singleton
    @Path("rootSingletonConstructorB/{p}")
    public static class TestCantInjectMethodParamsForSingleton {

        @GET
        public String getIt(@QueryParam("q") String queryParam) {
            return "it";
        }
    }

    @Path("rootRelaxedParser")
    @Produces(" a/b, c/d ")
    @Consumes({"e/f,g/h", " i/j"})
    public static class TestRelaxedProducesConsumesParserRules {

        @GET
        @Produces({"e/f,g/h", " i/j"})
        @Consumes(" a/b, c/d ")
        public String getIt(@QueryParam("q") String queryParam) {
            return "it";
        }
    }

    @Test
    public void testRelaxedProducesConsumesParserRules() throws Exception {
        LOGGER.info("An issue should not be reported with the relaxed Produces/Consumes values parser.");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectMethodParamsForSingleton.class);
        assertTrue(issues.isEmpty());
    }

    @Test
    public void testSingletonFieldsInjection() throws Exception {
        LOGGER.info("An issue should be reported if injection is required for a singleton life-cycle:");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectFieldsForSingleton.class);
        assertTrue(!issues.isEmpty());
        assertEquals(6, issues.size());
    }


    @Test
    public void testProviderFieldsInjection() throws Exception {
        LOGGER.info("An issue should be reported if injection is required for a class which is provider and "
                + "therefore singleton:");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectFieldsForProvider.class);
        assertTrue(!issues.isEmpty());
        assertEquals(7, issues.size());
    }


    @Test
    public void testSingletonConstructorParamsInjection() throws Exception {
        LOGGER.info("An issue should be reported if injection is required for a singleton life-cycle:");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectConstructorParamsForSingleton.class);
        assertTrue(!issues.isEmpty());
        assertEquals(1, issues.size());
    }

    @Test
    public void testSingletonMethodParamsInjection() throws Exception {
        LOGGER.info("An issue should not be reported as injections into the methods are allowed.");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectMethodParamsForSingleton.class);
        assertTrue(issues.isEmpty());
    }

    @Path("resourceAsProvider")
    public static class ResourceAsProvider implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }

        @GET
        public String get() {
            return "get";
        }
    }

    @Singleton
    @PerLookup
    @Path("resourceMultipleScopes")
    public static class ResourceWithMultipleScopes implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }

        @GET
        public String get() {
            return "get";
        }
    }

    @Test
    public void testResourceAsProvider() throws Exception {
        LOGGER.info("An issue should be reported as resource implements provider but does not define scope.");
        List<ResourceModelIssue> issues = testResourceValidation(ResourceAsProvider.class);
        assertEquals(1, issues.size());
    }

    @Test
    public void testResourceWithMultipleScopes() throws Exception {
        LOGGER.info("An issue should not be reported as resource defines multiple scopes.");
        List<ResourceModelIssue> issues = testResourceValidation(ResourceWithMultipleScopes.class);
        assertEquals(1, issues.size());
    }


    private List<ResourceModelIssue> testResourceValidation(final Class<?>... resourceClasses) {
        return Errors.process(new Producer<List<ResourceModelIssue>>() {
            @Override
            public List<ResourceModelIssue> call() {
                List<Resource> resources = Lists.newArrayList();
                for (Class<?> clazz : resourceClasses) {
                    final Resource res = Resource.builder(clazz).build();
                    if (res.getPath() != null) {
                        resources.add(res);
                    }
                }

                ResourceModel model = new ResourceModel.Builder(resources, false).build();
                ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
                validator.validate(model);
                return ModelErrors.getErrorsAsResourceModelIssues();
            }
        });
    }


    public static class TestNonPublicRM {

        @POST
        public String publicPost() {
            return "public";
        }

        @GET
        private String privateGet() {
            return "private";
        }
    }

    @Test
    public void testNonPublicRM() throws Exception {
        LOGGER.info("An issue should be reported if a resource method is not public:");

        List<ResourceModelIssue> issues = testResourceValidation(TestNonPublicRM.class);
        assertTrue(!issues.isEmpty());
    }

    public static class TestMoreThanOneEntity {

        @PUT
        public void put(String one, String two) {
        }
    }

    @Test
    @Ignore("Multiple entity validation not implemented yet.")
    // TODO implement validation
    public void suspendedTestMoreThanOneEntity() throws Exception {
        LOGGER.info("An issue should be reported if a resource method takes more than one entity params:");
        List<ResourceModelIssue> issues = testResourceValidation(TestMoreThanOneEntity.class);

        assertTrue(!issues.isEmpty());
    }

    @Path("test")
    public static class TestGetRMReturningVoid {

        @GET
        public void getMethod() {
        }
    }

    @Test
    public void testGetRMReturningVoid() throws Exception {
        LOGGER.info("An issue should be reported if a non-async get method returns void:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetRMReturningVoid.class);
        assertFalse(issues.isEmpty());
        assertNotEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    public static class TestAsyncGetRMReturningVoid {

        @GET
        public void getMethod(@Suspended AsyncResponse ar) {
        }
    }

    @Test
    public void testAsyncGetRMReturningVoid() throws Exception {
        LOGGER.info("An issue should NOT be reported if a async get method returns void:");
        List<ResourceModelIssue> issues = testResourceValidation(TestAsyncGetRMReturningVoid.class);
        assertTrue(issues.isEmpty());
    }

    @Path("test")
    public static class TestGetRMConsumingEntity {

        @GET
        public String getMethod(Object o) {
            return "it";
        }
    }

    @Test
    public void testGetRMConsumingEntity() throws Exception {
        LOGGER.info("An issue should be reported if a get method consumes an entity:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetRMConsumingEntity.class);
        assertFalse(issues.isEmpty());
        assertNotEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("test")
    public static class TestGetRMConsumingFormParam {

        @GET
        public String getMethod(@FormParam("f") String s1, @FormParam("g") String s2) {
            return "it";
        }
    }

    @Test
    public void testGetRMConsumingFormParam() throws Exception {
        LOGGER.info("An issue should be reported if a get method consumes a form param:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetRMConsumingFormParam.class);
        assertTrue(issues.size() == 1);
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("test")
    public static class TestSRLReturningVoid {

        @Path("srl")
        public void srLocator() {
        }
    }

    @Test
    public void testSRLReturningVoid() throws Exception {
        LOGGER.info("An issue should be reported if a sub-resource locator returns void:");
        Resource resource = Resource.builder(TestSRLReturningVoid.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);
        assertTrue(validator.fatalIssuesFound());
    }

    @Path("test")
    public static class TestGetSRMReturningVoid {

        @GET
        @Path("srm")
        public void getSRMethod() {
        }
    }

    @Test
    public void testGetSRMReturningVoid() throws Exception {
        LOGGER.info("An issue should be reported if a get sub-resource method returns void:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetSRMReturningVoid.class);

        assertFalse(issues.isEmpty());
        assertNotEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("test")
    public static class TestGetSRMConsumingEntity {

        @Path("p")
        @GET
        public String getMethod(Object o) {
            return "it";
        }
    }

    @Test
    public void testGetSRMConsumingEntity() throws Exception {
        LOGGER.info("An issue should be reported if a get method consumes an entity:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetSRMConsumingEntity.class);

        assertFalse(issues.isEmpty());
        assertNotEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("root")
    public static class TestGetSRMConsumingFormParam {

        @GET
        @Path("p")
        public String getMethod(@FormParam("f") String formParam) {
            return "it";
        }
    }

    @Test
    public void testGetSRMConsumingFormParam() throws Exception {
        LOGGER.info("An issue should be reported if a get method consumes a form param:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetSRMConsumingFormParam.class);

        assertFalse(issues.isEmpty());
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("rootMultipleHttpMethodDesignatorsRM")
    public static class TestMultipleHttpMethodDesignatorsRM {

        @GET
        @PUT
        public String getPutIt() {
            return "it";
        }
    }

    @Test
    public void testMultipleHttpMethodDesignatorsRM() throws Exception {
        LOGGER.info("An issue should be reported if more than one HTTP method designator exist on a resource "
                + "method:");
        Resource resource = Resource.builder(TestMultipleHttpMethodDesignatorsRM.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);
        assertTrue(validator.fatalIssuesFound());
    }

    @Path("rootMultipleHttpMethodDesignatorsSRM")
    public static class TestMultipleHttpMethodDesignatorsSRM {

        @Path("srm")
        @POST
        @PUT
        public String postPutIt() {
            return "it";
        }
    }

    @Test
    public void testMultipleHttpMethodDesignatorsSRM() throws Exception {
        LOGGER.info("An issue should be reported if more than one HTTP method designator exist on a sub-resource "
                + "method:");
        Resource resource = Resource.builder(TestMultipleHttpMethodDesignatorsSRM.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);
        assertTrue(validator.fatalIssuesFound());
    }

    @Path("rootEntityParamOnSRL")
    public static class TestEntityParamOnSRL {

        @Path("srl")
        public String locator(String s) {
            return "it";
        }
    }

    @Test
    public void testEntityParamOnSRL() throws Exception {
        LOGGER.info("An issue should be reported if an entity parameter exists on a sub-resource locator:");
        Resource resource = Resource.builder(TestEntityParamOnSRL.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);
        assertTrue(validator.fatalIssuesFound());
    }

    @Path(value = "/DeleteTest")
    public static class TestNonConflictingHttpMethodDelete {

        static String html_content =
                "<html>" + "<head><title>Delete text/html</title></head>"
                        + "<body>Delete text/html</body></html>";

        @DELETE
        @Produces(value = "text/plain")
        public String getPlain() {
            return "Delete text/plain";
        }

        @DELETE
        @Produces(value = "text/html")
        public String getHtml() {
            return html_content;
        }

        @DELETE
        @Path(value = "/sub")
        @Produces(value = "text/html")
        public String getSub() {
            return html_content;
        }
    }

    @Test
    public void testNonConflictingHttpMethodDelete() throws Exception {
        LOGGER.info("No issue should be reported if produced mime types differ");
        List<ResourceModelIssue> issues = testResourceValidation(TestNonConflictingHttpMethodDelete.class);

        assertTrue(issues.isEmpty());
    }

    @Path(value = "/AmbigParamTest")
    public static class TestAmbiguousParams {

        @QueryParam("q")
        @HeaderParam("q")
        private int a;

        @QueryParam("b")
        @HeaderParam("b")
        @MatrixParam("q")
        public void setB(String b) {
        }

        @GET
        @Path("a")
        public String get(@PathParam("a") @QueryParam("a") String a) {
            return "hi";
        }

        @GET
        @Path("b")
        public String getSub(@PathParam("a") @QueryParam("b") @MatrixParam("c") String a,
                             @MatrixParam("m") @QueryParam("m") int i) {
            return "hi";
        }

        @Path("c")
        public Object getSubLoc(@MatrixParam("m") @CookieParam("c") String a) {
            return null;
        }
    }

    @Test
    public void testAmbiguousParams() throws Exception {
        LOGGER.info("A warning should be reported if ambiguous source of a parameter is seen");
        Errors.process(new Runnable() {
            @Override
            public void run() {
                Resource resource = Resource.builder(TestAmbiguousParams.class).build();
                ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
                validator.validate(resource);

                assertTrue(!validator.fatalIssuesFound());
                assertEquals(4, validator.getIssueList().size());
                assertEquals(6, Errors.getErrorMessages().size());
            }
        });
    }

    @Path(value = "/EmptyPathSegmentTest")
    public static class TestEmptyPathSegment {

        @GET
        @Path("/")
        public String get() {
            return "hi";
        }
    }

    @Test
    public void testEmptyPathSegment() throws Exception {
        LOGGER.info("A warning should be reported if @Path with \"/\" or empty string value is seen");
        Resource resource = Resource.builder(TestEmptyPathSegment.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);

        assertTrue(!validator.fatalIssuesFound());
        assertEquals(1, validator.getIssueList().size());
    }


    public static class TypeVariableResource<T, V> {
        @QueryParam("v")
        V fieldV;

        V methodV;

        @QueryParam("v")
        public void set(V methodV) {
            this.methodV = methodV;
        }

        @GET
        public String get(@BeanParam() V getV) {
            return getV.toString() + fieldV.toString() + methodV.toString();
        }

        @POST
        public T post(T t) {
            return t;
        }

        @Path("sub")
        @POST
        public T postSub(T t) {
            return t;
        }
    }

    @Test
    public void testTypeVariableResource() throws Exception {
        LOGGER.info("");
        Errors.process(new Runnable() {
            @Override
            public void run() {
                Resource resource = Resource.builder(TypeVariableResource.class).build();
                ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
                validator.validate(resource);

                assertTrue(!validator.fatalIssuesFound());
                assertEquals(5, validator.getIssueList().size());
                assertEquals(7, Errors.getErrorMessages().size());
            }
        });
    }

    public static class ParameterizedTypeResource<T, V> {
        @QueryParam("v")
        Collection<V> fieldV;

        List<List<V>> methodV;

        @QueryParam("v")
        public void set(List<List<V>> methodV) {
            this.methodV = methodV;
        }

        @GET
        public String get(@QueryParam("v") List<V> getV) {
            return "";
        }

        @POST
        public Collection<T> post(Collection<T> t) {
            return t;
        }

        @Path("sub")
        @POST
        public Collection<T> postSub(Collection<T> t) {
            return t;
        }
    }

    public static class ConcreteParameterizedTypeResource extends ParameterizedTypeResource<String, String> {
    }

    @Test
    public void testParameterizedTypeResource() throws Exception {
        LOGGER.info("");
        Resource resource = Resource.builder(ConcreteParameterizedTypeResource.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);

        assertTrue(!validator.fatalIssuesFound());
        assertEquals(0, validator.getIssueList().size());
    }

    public static class GenericArrayResource<T, V> {
        @QueryParam("v")
        V[] fieldV;

        V[] methodV;

        @QueryParam("v")
        public void set(V[] methodV) {
            this.methodV = methodV;
        }

        @POST
        public T[] post(T[] t) {
            return t;
        }

        @Path("sub")
        @POST
        public T[] postSub(T[] t) {
            return t;
        }
    }

    public static class ConcreteGenericArrayResource extends GenericArrayResource<String, String> {
    }

    @Test
    public void testGenericArrayResource() throws Exception {
        LOGGER.info("");
        Resource resource = Resource.builder(ConcreteGenericArrayResource.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);

        assertTrue(!validator.fatalIssuesFound());
        assertEquals(0, validator.getIssueList().size());
    }

    // TODO: test multiple root resources with the same uriTemplate (in WebApplicationImpl.processRootResources ?)

    @Path("test1")
    public static class PercentEncodedTest {
        @GET
        @Path("%5B%5D")
        public String percent() {
            return "percent";
        }

        @GET
        @Path("[]")
        public String notEncoded() {
            return "not-encoded";
        }
    }

    @Test
    public void testPercentEncoded() throws Exception {
        List<ResourceModelIssue> issues = testResourceValidation(PercentEncodedTest.class);
        assertEquals(1, issues.size());
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }


    @Path("test2")
    public static class PercentEncodedCaseSensitiveTest {
        @GET
        @Path("%5B%5D")
        public String percent() {
            return "percent";
        }

        @GET
        @Path("%5b%5d")
        public String notEncoded() {
            return "not-encoded";
        }
    }

    @Test
    public void testPercentEncodedCaseSensitive() throws Exception {
        List<ResourceModelIssue> issues = testResourceValidation(PercentEncodedCaseSensitiveTest.class);
        assertEquals(1, issues.size());
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("ambiguous-parameter")
    public static class AmbiguousParameterResource {
        @POST
        public String moreNonAnnotatedParameters(@HeaderParam("something") String header, String entity1, String entity2) {
            return "x";
        }
    }

    @Test
    public void testNotAnnotatedParameters() throws Exception {
        Resource resource = Resource.builder(AmbiguousParameterResource.class).build();
        ComponentModelValidator validator = new ComponentModelValidator(ServerLocatorFactory.createLocator());
        validator.validate(resource);

        final List<ResourceModelIssue> errorMessages = validator.getIssueList();
        assertEquals(1, errorMessages.size());
        assertEquals(Severity.FATAL, errorMessages.get(0).getSeverity());
    }


    public static class SubResource {
        public static final String MESSAGE = "Got it!";

        @GET
        public String getIt() {
            return MESSAGE;
        }
    }

    /**
     * Should report warning during validation as Resource cannot have resource method and sub resource locators on the same path.
     */
    @Path("failRoot")
    public static class MethodAndLocatorResource {


        @Path("/")
        public SubResource getSubResourceLocator() {
            return new SubResource();
        }

        @GET
        public String get() {
            return "should never be called - fails during validation";
        }
    }


    @Test
    public void testLocatorAndMethodValidation() throws Exception {
        LOGGER.info("Should report warning during validation as Resource cannot have resource method and sub "
                + "resource locators on the same path.");
        List<ResourceModelIssue> issues = testResourceValidation(MethodAndLocatorResource.class);
        assertEquals(1, issues.size());
        assertNotEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    /**
     * Should report warning during validation as Resource cannot have resource method and sub resource locators on the same path.
     */
    @Path("failRoot")
    public static class MethodAndLocatorResource2 {


        @Path("a")
        public SubResource getSubResourceLocator() {
            return new SubResource();
        }

        @GET
        @Path("a")
        public String get() {
            return "should never be called - fails during validation";
        }
    }


    @Test
    public void testLocatorAndMethod2Validation() throws Exception {
        LOGGER.info("Should report warning during validation as Resource cannot have resource method and sub "
                + "resource locators on the same path.");
        List<ResourceModelIssue> issues = testResourceValidation(MethodAndLocatorResource2.class);
        assertEquals(1, issues.size());
        assertNotEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    /**
     * Warning should be reported informing wich locator will be used in runtime
     */
    @Path("locator")
    public static class TwoLocatorsResource {
        @Path("a")
        public SubResource locator() {
            return new SubResource();
        }

        @Path("a")
        public SubResource locator2() {
            return new SubResource();
        }
    }

    @Test
    public void testLocatorPathValidationFail() throws Exception {
        LOGGER.info("Should report error during validation as Resource cannot have ambiguous sub resource locators.");
        List<ResourceModelIssue> issues = testResourceValidation(TwoLocatorsResource.class);
        assertEquals(1, issues.size());
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("root")
    public static class ResourceRoot {
        @GET
        @Path("sub-root") // in path collision with ResourceSubRoot.get()
        public String get() {
            return "should never be called - fails during validation";
        }
    }

    @Path("root/sub-root")
    public static class ResourceSubPathRoot {
        @GET
        public String get() {
            return "should never be called - fails during validation";
        }
    }

    @Path("root")
    public static class ResourceRootNotUnique {
        @GET
        @Path("sub-root") // in path collision with ResourceSubRoot.get()
        public String get() {
            return "should never be called - fails during validation";
        }
    }

    @Test
    @Ignore
    // TODO: need to add validation to detect ambiguous problems of ResourceSubPathRoot and two other resources.
    public void testTwoOverlappingSubResourceValidation() throws Exception {
        List<ResourceModelIssue> issues = testResourceValidation(ResourceRoot.class, ResourceSubPathRoot.class);
        assertEquals(1, issues.size());
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Test
    @Ignore
    public void testTwoOverlappingResourceValidation() throws Exception {
        List<ResourceModelIssue> issues = testResourceValidation(ResourceRoot.class, ResourceRootNotUnique.class);
        assertEquals(1, issues.size());
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("root")
    public static class EmptyResource {
        public String get() {
            return "not a get method.";
        }
    }

    @Test
    public void testEmptyResourcel() throws Exception {
        LOGGER.info("Should report warning during validation as Resource cannot have resource method and sub "
                + "resource locators on the same path.");
        List<ResourceModelIssue> issues = testResourceValidation(EmptyResource.class);
        assertEquals(1, issues.size());
        assertFalse(issues.get(0).getSeverity() == Severity.FATAL);
    }


    @Path("{abc}")
    public static class AmbiguousResource1 {
        @Path("x")
        @GET
        public String get() {
            return "get";
        }
    }

    @Path("{def}")
    public static class AmbiguousResource2 {
        @Path("x")
        @GET
        public String get() {
            return "get";
        }
    }

    @Path("unique")
    public static class UniqueResource {
        @Path("x")
        public String get() {
            return "get";
        }
    }

    @Test
    public void testAmbiguousResources() throws Exception {
        LOGGER.info("Should report warning during validation error as resource path patterns are ambiguous ({abc} and {def} "
                + "results into same path pattern).");
        List<ResourceModelIssue> issues = testResourceValidation(AmbiguousResource1.class, AmbiguousResource2.class,
                UniqueResource.class);
        assertEquals(1, issues.size());
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }


    @Path("{abc}")
    public static class AmbiguousLocatorResource1 {
        @Path("x")
        public SubResource locator() {
            return new SubResource();
        }
    }

    @Path("{def}")
    public static class AmbiguousLocatorResource2 {
        @Path("x")
        public SubResource locator2() {
            return new SubResource();
        }
    }

    @Test
    public void testAmbiguousResourceLocators() throws Exception {
        LOGGER.info("Should report warning during validation error as resource path patterns are ambiguous ({abc} and {def} "
                + "results into same path pattern).");
        List<ResourceModelIssue> issues = testResourceValidation(AmbiguousLocatorResource1.class,
                AmbiguousLocatorResource2.class);
        assertEquals(1, issues.size());
        assertEquals(Severity.FATAL, issues.get(0).getSeverity());
    }

    @Path("resource")
    public static class ResourceMethodWithVoidReturnType {
        @GET
        @Path("error")
        public void error() {
        }
    }

    @Test
    public void testVoidReturnType() throws Exception {
        LOGGER.info("Should report hint during validation as @GET resource method returns void.");
        List<ResourceModelIssue> issues = testResourceValidation(ResourceMethodWithVoidReturnType.class);

        assertEquals(1, issues.size());
        assertEquals(Severity.HINT, issues.get(0).getSeverity());
    }

    /**
     * Test of disabled validation failing on errors.
     */
    @Path("test-disable-validation-fail-on-error")
    public static class TestDisableValidationFailOnErrorResource {
        @GET
        public String get() {
            return "PASSED";
        }
    }

    @Test
    public void testDisableFailOnErrors() throws ExecutionException, InterruptedException {
        final ResourceConfig rc = new ResourceConfig(
                AmbiguousLocatorResource1.class,
                AmbiguousLocatorResource2.class,
                AmbiguousParameterResource.class,
                AmbiguousResource1.class,
                AmbiguousResource2.class,
                ConcreteGenericArrayResource.class,
                ConcreteParameterizedTypeResource.class,
                EmptyResource.class,
                GenericArrayResource.class,
                MethodAndLocatorResource.class,
                MethodAndLocatorResource2.class,
                MyBeanParam.class,
                ParameterizedTypeResource.class,
                PercentEncodedCaseSensitiveTest.class,
                PercentEncodedTest.class,
                ResourceAsProvider.class,
                ResourceMethodWithVoidReturnType.class,
                ResourceRoot.class,
                ResourceRootNotUnique.class,
                ResourceSubPathRoot.class,
                ResourceWithMultipleScopes.class,
                TestAmbiguousParams.class,
                TestAsyncGetRMReturningVoid.class,
                TestEmptyPathSegment.class,
                TestEntityParamOnSRL.class,
                TestGetRMConsumingEntity.class,
                TestGetRMConsumingFormParam.class,
                TestGetRMReturningVoid.class,
                TestGetSRMConsumingEntity.class,
                TestGetSRMConsumingFormParam.class,
                TestGetSRMReturningVoid.class,
                TestMoreThanOneEntity.class,
                TestMultipleHttpMethodDesignatorsRM.class,
                TestMultipleHttpMethodDesignatorsSRM.class,
                TestNonConflictingHttpMethodDelete.class,
                TestNonPublicRM.class,
                TestRelaxedProducesConsumesParserRules.class,
                TestRootResourceNonAmbigCtors.class,
                TestSRLReturningVoid.class,
                TwoLocatorsResource.class,
                TypeVariableResource.class,
                UniqueResource.class,

                TestDisableValidationFailOnErrorResource.class // we should still be able to invoke a GET on this one.
        );
        rc.property(ServerProperties.RESOURCE_VALIDATION_IGNORE_ERRORS, true);
        ApplicationHandler ah = new ApplicationHandler(rc);

        final ContainerRequest request = RequestContextBuilder.from("/test-disable-validation-fail-on-error", "GET").build();
        ContainerResponse response = ah.apply(request).get();

        assertEquals(200, response.getStatus());
        assertEquals("PASSED", response.getEntity());
    }
}
