/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import org.glassfish.hk2.api.PerLookup;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Taken from Jersey 1: jersey-server:com.sun.jersey.server.impl.modelapi.validation.BasicValidatorTest.java
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class BasicValidatorTest {

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
        System.out.println(
                "---\nNo issue should be reported if more public ctors exists with the same number of params, " +
                        "but another just one is presented with more params at a root resource:");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(TestRootResourceNonAmbigCtors.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        assertTrue(validator.getIssueList().isEmpty());
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
        System.out.println("---\nAn issue should not be reported with the relaxed Produces/Consumes values parser.");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectMethodParamsForSingleton.class);
        assertTrue(issues.isEmpty());
    }

    @Test
    public void testSingletonFieldsInjection() throws Exception {
        System.out.println("---\nAn issue should be reported if injection is required for a singleton life-cycle:");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectFieldsForSingleton.class);
        assertTrue(!issues.isEmpty());
        assertEquals(5, issues.size());
    }


    @Test
    public void testProviderFieldsInjection() throws Exception {
        System.out.println("---\nAn issue should be reported if injection is required for a class which is provider and " +
                "therefore singleton:");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectFieldsForProvider.class);
        assertTrue(!issues.isEmpty());
        assertEquals(6, issues.size());
    }


    @Test
    public void testSingletonConstructorParamsInjection() throws Exception {
        System.out.println("---\nAn issue should be reported if injection is required for a singleton life-cycle:");
        List<ResourceModelIssue> issues = testResourceValidation(TestCantInjectConstructorParamsForSingleton.class);
        assertTrue(!issues.isEmpty());
        assertEquals(1, issues.size());
    }

    @Test
    public void testSingletonMethodParamsInjection() throws Exception {
        System.out.println("---\nAn issue should not be reported as injections into the methods are allowed.");
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
        System.out.println("---\nAn issue should be reported as resource implements provider but does not define scope.");
        List<ResourceModelIssue> issues = testResourceValidation(ResourceAsProvider.class);
        assertEquals(1, issues.size());
    }

    @Test
    public void testResourceWithMultipleScopes() throws Exception {
        System.out.println("---\nAn issue should not be reported as resource defines multiple scopes.");
        List<ResourceModelIssue> issues = testResourceValidation(ResourceWithMultipleScopes.class);
        assertEquals(1, issues.size());
    }


    private List<ResourceModelIssue> testResourceValidation(Class<?> resourceClass) {
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(resourceClass, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        return issues;
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
        System.out.println("---\nAn issue should be reported if a resource method is not public:");

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
        System.out.println("---\nAn issue should be reported if a resource method takes more than one entity params:");
        List<ResourceModelIssue> issues = testResourceValidation(TestMoreThanOneEntity.class);

        assertTrue(!issues.isEmpty());
    }

    public static class TestGetRMReturningVoid {

        @GET
        public void getMethod() {
        }
    }

    @Test
    public void testGetRMReturningVoid() throws Exception {
        System.out.println("---\nAn issue should be reported if a non-async get method returns void:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetRMReturningVoid.class);
        assertTrue(!issues.isEmpty());
        assertTrue(!issues.get(0).isFatal());
    }

    public static class TestAsyncGetRMReturningVoid {

        @GET
        public void getMethod(@Suspended AsyncResponse ar) {
        }
    }

    @Test
    @Ignore("Async GET void validation not updated to new API.")
    // TODO update validation
    public void testAsyncGetRMReturningVoid() throws Exception {
        System.out.println("---\nAn issue should NOT be reported if a async get method returns void:");
        List<ResourceModelIssue> issues = testResourceValidation(TestAsyncGetRMReturningVoid.class);
        assertTrue(issues.isEmpty());
    }

    public static class TestGetRMConsumingEntity {

        @GET
        public String getMethod(Object o) {
            return "it";
        }
    }

    @Test
    public void testGetRMConsumingEntity() throws Exception {
        System.out.println("---\nAn issue should be reported if a get method consumes an entity:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetRMConsumingEntity.class);
        assertTrue(!issues.isEmpty());
        assertTrue(!issues.get(0).isFatal());
    }

    public static class TestGetRMConsumingFormParam {

        @GET
        public String getMethod(@FormParam("f") Object o, @FormParam("g") Object p) {
            return "it";
        }
    }

    @Test
    public void testGetRMConsumingFormParam() throws Exception {
        System.out.println("---\nAn issue should be reported if a get method consumes a form param:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetRMConsumingFormParam.class);
        assertTrue(issues.size() == 1);
        assertTrue(issues.get(0).isFatal());
    }

    public static class TestSRLReturningVoid {

        @Path("srl")
        public void srLocator() {
        }
    }

    @Test
    public void testSRLReturningVoid() throws Exception {
        System.out.println("---\nAn issue should be reported if a sub-resource locator returns void:");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(TestSRLReturningVoid.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        assertTrue(validator.fatalIssuesFound());
    }

    public static class TestGetSRMReturningVoid {

        @GET
        @Path("srm")
        public void getSRMethod() {
        }
    }

    @Test
    public void testGetSRMReturningVoid() throws Exception {
        System.out.println("---\nAn issue should be reported if a get sub-resource method returns void:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetSRMReturningVoid.class);

        assertTrue(!issues.isEmpty());
        assertTrue(!issues.get(0).isFatal());
    }

    public static class TestGetSRMConsumingEntity {

        @Path("p")
        @GET
        public String getMethod(Object o) {
            return "it";
        }
    }

    @Test
    public void testGetSRMConsumingEntity() throws Exception {
        System.out.println("---\nAn issue should be reported if a get method consumes an entity:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetSRMConsumingEntity.class);

        assertTrue(!issues.isEmpty());
        assertTrue(!issues.get(0).isFatal());
    }

    public static class TestGetSRMConsumingFormParam {

        @GET
        @Path("p")
        public String getMethod(@FormParam("f") Object o) {
            return "it";
        }
    }

    @Test
    public void testGetSRMConsumingFormParam() throws Exception {
        System.out.println("---\nAn issue should be reported if a get method consumes a form param:");
        List<ResourceModelIssue> issues = testResourceValidation(TestGetSRMConsumingFormParam.class);

        assertTrue(!issues.isEmpty());
        assertTrue(issues.get(0).isFatal());
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
        System.out.println("---\nAn issue should be reported if more than one HTTP method designator exist on a resource " +
                "method:");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(TestMultipleHttpMethodDesignatorsRM.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
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
        System.out.println("---\nAn issue should be reported if more than one HTTP method designator exist on a sub-resource " +
                "method:");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(TestMultipleHttpMethodDesignatorsSRM.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
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
        System.out.println("---\nAn issue should be reported if an entity parameter exists on a sub-resource locator:");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(TestEntityParamOnSRL.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        assertTrue(validator.fatalIssuesFound());
    }

    @Path(value = "/DeleteTest")
    public static class TestNonConflictingHttpMethodDelete {

        static String html_content =
                "<html>" + "<head><title>Delete text/html</title></head>" +
                        "<body>Delete text/html</body></html>";

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
        System.out.println("---\nNo issue should be reported if produced mime types differ");
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
        System.out.println("---\nA warning should be reported if ambiguous source of a parameter is seen");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(TestAmbiguousParams.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        assertTrue(!validator.fatalIssuesFound());
        assertEquals(6, validator.getIssueList().size());
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
        System.out.println("---\nA warning should be reported if @Path with \"/\" or empty string value is seen");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(TestEmptyPathSegment.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
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
        public String get(@QueryParam("v") V getV) {
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
        System.out.println("---\n");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(TypeVariableResource.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        assertTrue(!validator.fatalIssuesFound());
        assertEquals(7, validator.getIssueList().size());
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
        public String get(@QueryParam("v") Map<String, List<V>> getV) {
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
        System.out.println("---\n");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(ConcreteParameterizedTypeResource.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
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

        @GET
        public String get(@QueryParam("v") V[] getV) {
            return "";
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
        System.out.println("---\n");
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(ConcreteGenericArrayResource.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        assertTrue(!validator.fatalIssuesFound());
        assertEquals(0, validator.getIssueList().size());
    }

    // TODO: test multiple root resources with the same uriTemplate (in WebApplicationImpl.processRootResources ?)

    private static void printIssueList(BasicValidator validator) {
        for (ResourceModelIssue issue : validator.getIssueList()) {
            System.out.println((issue.isFatal() ? "ERROR: " : "WARNING: ") + issue.getMessage());
        }
    }

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
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(PercentEncodedTest.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        assertEquals(1, validator.getIssueList().size());
        assertTrue(validator.getIssueList().get(0).isFatal());
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
        List<ResourceModelIssue> issues = new LinkedList<ResourceModelIssue>();
        Resource resource = Resource.builder(PercentEncodedCaseSensitiveTest.class, issues).build();
        BasicValidator validator = new BasicValidator(issues);
        validator.validate(resource);
        printIssueList(validator);
        assertEquals(1, validator.getIssueList().size());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }

}
