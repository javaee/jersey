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
package org.glassfish.jersey.tests.e2e.server.wadl;

import java.io.StringWriter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.glassfish.jersey.internal.util.SimpleNamespaceResolver;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndTextQualifier;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableMap;

/**
 * Tests whether WADL for a {@link BeanParam} annotated resource method parameter is generated properly.
 * <p/>
 * The tests of this class perform a comparison of
 * <pre><ul>
 *     <li>a WADL of a reference resource that has {@code *Param} annotated class fields or resource method parameters</li>
 *     <li>with a resource configured with {@link BeanParam} annotated parameters where some of the reference resource parameters
 *          are aggregated in the class that is used as a bean param</li>
 * </ul></pre>
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class WadlBeanParamTest extends JerseyTest {

    private final ElementNameAndTextQualifier elementQualifier = new ElementNameAndTextQualifier() {

        /**
         * For {@code <param ??? />} nodes, the comparison is based on matching {@code name} attributes while ignoring
         * their order. For any other nodes, strict comparison (including ordering) is made.
         *
         * @param control The reference element to compare the {@code test} with.
         * @param test The test element to compare against {@code control}.
         * @return Whether given nodes qualify for comparison.
         */
        @Override
        public boolean qualifyForComparison(final Element control, final Element test) {
            if (test != null && !"param".equals(test.getNodeName())) {
                return super.qualifyForComparison(control, test);
            }
            if (!(control != null && test != null
                          && equalsNamespace(control, test)
                          && getNonNamespacedNodeName(control).equals(getNonNamespacedNodeName(test)))) {
                return false;
            }
            if (control.hasAttribute("name") && test.hasAttribute("name")) {
                if (control.getAttribute("name").equals(test.getAttribute("name"))) {
                    return true;
                }
            }
            return false;
        }
    };

    @Override
    protected Application configure() {
        return new ResourceConfig(ReferenceResourceBeanParam.class, TestResourceBeanParam.class,
                TestResourceConstructorInitializedBeanParam.class, TestResourceFieldBeanParam.class);
    }

    private String nodeAsString(final Object resourceNode) throws TransformerException {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource((Node) resourceNode), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Tests whether class with {@code *Param} annotated fields if used as a {@code BeanParam} annotated resource method parameter
     * results in a correctly generated WADL.
     *
     * @throws Exception In case of any problem.
     */
    @Test
    public void testBeanParamFullBean() throws Exception {
        testBeanParamConstructorInitializedBean("wadlBeanParamTest");
    }

    /**
     * Tests whether class with {@code *Param} annotated constructor parameters if used as a {@code BeanParam} annotated resource
     * method parameter results in a correctly generated WADL.
     *
     * @throws Exception In case of any problem.
     */
    @Test
    public void testBeanParamConstructorInitializedBean() throws Exception {
        testBeanParamConstructorInitializedBean("wadlBeanParamConstructorInitializedTest");
    }

    /**
     * Tests whether class with {@code *Param} annotated constructor parameters if used as a {@code BeanParam} annotated resource
     * class field parameter results in a correctly generated WADL.
     *
     * @throws Exception In case of any problem.
     */
    @Test
    public void testBeanParamFieldBean() throws Exception {
        testBeanParamConstructorInitializedBean("wadlBeanParamFieldTest");
    }

    private void testBeanParamConstructorInitializedBean(String resource) throws Exception {
        final Response response = target("/application.wadl").request().get();
        final Document d = WadlResourceTest.extractWadlAsDocument(response);
        final XPath xp = XPathFactory.newInstance().newXPath();
        final SimpleNamespaceResolver nsContext = new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02");
        xp.setNamespaceContext(nsContext);

        final Diff diff = XMLUnit.compareXML(
                nodeAsString(
                        xp.evaluate("//wadl:resource[@path='wadlBeanParamReference']/wadl:resource", d,
                                XPathConstants.NODE)),
                nodeAsString(
                        xp.evaluate("//wadl:resource[@path='" + resource + "']/wadl:resource", d,
                                XPathConstants.NODE))
        );
        XMLUnit.setXpathNamespaceContext(
                new SimpleNamespaceContext(ImmutableMap.of("wadl", "http://wadl.dev.java.net/2009/02")));
        diff.overrideElementQualifier(elementQualifier);
        XMLAssert.assertXMLEqual(diff, true);
    }

    @Path("wadlBeanParamReference")
    private static class ReferenceResourceBeanParam {

        @QueryParam("classFieldQueryParam")
        private String classFieldQueryParam;

        //////////////////////////////////////////////////////////////////
        // following fields make this class compatible with 'FullBean', //
        // 'ConstructorInitializedBean' and 'SmallBean'                 //
        @HeaderParam("header")
        private String headerParam;

        @MatrixParam("matrix")
        private String matrixParam;

        @Encoded
        @QueryParam("query")
        private String queryParam;

        @CookieParam("cookie")
        private String cookie;

        @FormParam("form")
        private String formParam;

        @POST
        @Path("singleBean/{path}")
        public String postBeanParam(/* pathParam is also extracted from FullBean */
                                    @PathParam("path") String pathParam,
                                    @QueryParam("methodParam") int methodParam,
                                    @HeaderParam("header") String duplicateHeaderParam) {
            return "";
        }

    }

    @Path("wadlBeanParamTest")
    private static class TestResourceBeanParam {

        @QueryParam("classFieldQueryParam")
        private String classFieldQueryParam;

        @POST
        @Path("singleBean/{path}")
        public String postBeanParam(@BeanParam WadlFullBean bean,
                                    @BeanParam WadlSmallBean wadlSmallBean,
                                    @QueryParam("methodParam") int methodParam) {
            return "";
        }

    }

    @Path("wadlBeanParamConstructorInitializedTest")
    private static class TestResourceConstructorInitializedBeanParam {

        @QueryParam("classFieldQueryParam")
        private String classFieldQueryParam;

        @POST
        @Path("singleBean/{path}")
        public String postBeanParam(@BeanParam WadlConstructorInitializedBean bean,
                                    @BeanParam WadlSmallBean wadlSmallBean,
                                    @QueryParam("methodParam") int methodParam) {
            return "";
        }

    }

    @Path("wadlBeanParamFieldTest")
    private static class TestResourceFieldBeanParam {

        @QueryParam("classFieldQueryParam")
        private String classFieldQueryParam;

        @BeanParam
        private WadlConstructorInitializedBean bean;

        @BeanParam
        private WadlSmallBean wadlSmallBean;

        @POST
        @Path("singleBean/{path}")
        public String postBeanParam(@QueryParam("methodParam") int methodParam) {
            return "";
        }

    }

    /**
     * The purpose of this unknown annotation is to verify that a usage of an unknown annotation in a {@link BeanParam} annotated
     * class does not cause a failure during WADL generation.
     */
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface WadlUnknownAnnotation {

        String value();
    }

    public static class WadlSmallBean {

        @HeaderParam("header")
        private String headerParam;

        @PathParam("path")
        private String pathParam;

        public WadlSmallBean(WadlFullBean bean) {
            headerParam = bean.getHeaderParam();
            pathParam = bean.getPathParam();
        }
    }

    public static class WadlEncodedBean {

        @MatrixParam("matrix")
        private String matrixParam;

        @Encoded
        @QueryParam("query")
        private String queryParam;

        public WadlEncodedBean(String matrixParam, String queryParam) {
            this.matrixParam = matrixParam;
            this.queryParam = queryParam;
        }

    }

    public static class WadlFullBean {

        @HeaderParam("header")
        private String headerParam;

        @PathParam("path")
        private String pathParam;

        @MatrixParam("matrix")
        private String matrixParam;

        @QueryParam("query")
        private String queryParam;

        @CookieParam("cookie")
        private String cookie;

        @FormParam("form")
        private String formParam;

        @WadlUnknownAnnotation("unknown")
        private String unknownAnnotationParam;

        @Context
        private Request request;

        private boolean overrideRequestNull;

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }

        public String getFormParam() {
            return formParam;
        }

        public void setFormParam(String formParam) {
            this.formParam = formParam;
        }

        public String getHeaderParam() {
            return headerParam;
        }

        public void setHeaderParam(String headerParam) {
            this.headerParam = headerParam;
        }

        public String getMatrixParam() {
            return matrixParam;
        }

        public void setMatrixParam(String matrixParam) {
            this.matrixParam = matrixParam;
        }

        public String getPathParam() {
            return pathParam;
        }

        public void setPathParam(String pathParam) {
            this.pathParam = pathParam;
        }

        public String getQueryParam() {
            return queryParam;
        }

        public void setQueryParam(String queryParam) {
            this.queryParam = queryParam;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public boolean isOverrideRequestNull() {
            return overrideRequestNull;
        }

        public void setOverrideRequestNull(boolean overrideRequestNull) {
            this.overrideRequestNull = overrideRequestNull;
        }

        public String getUnknownAnnotationParam() {
            return unknownAnnotationParam;
        }

        public void setUnknownAnnotationParam(String unknownAnnotationParam) {
            this.unknownAnnotationParam = unknownAnnotationParam;
        }
    }

    public static class WadlConstructorInitializedBean {

        private String headerParam;
        private String pathParam;
        private String matrixParam;
        private String queryParam;
        private String cookie;
        private String formParam;
        private String unknownAnnotationParam;
        private Request request;

        public WadlConstructorInitializedBean(@CookieParam("cookie") String cookie,
                                              @FormParam("form") String formParam,
                                              @HeaderParam("header") String headerParam,
                                              @MatrixParam("matrix") String matrixParam,
                                              @QueryParam("query") String queryParam,
                                              @PathParam("path") String pathParam,
                                              @WadlUnknownAnnotation("unknown") String unknownAnnotationParam,
                                              @Context Request request) {
            this.cookie = cookie;
            this.formParam = formParam;
            this.headerParam = headerParam;
            this.matrixParam = matrixParam;
            this.queryParam = queryParam;
            this.pathParam = pathParam;
            this.unknownAnnotationParam = unknownAnnotationParam;
            this.request = request;
        }

        private boolean overrideRequestNull;

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }

        public String getFormParam() {
            return formParam;
        }

        public void setFormParam(String formParam) {
            this.formParam = formParam;
        }

        public String getHeaderParam() {
            return headerParam;
        }

        public void setHeaderParam(String headerParam) {
            this.headerParam = headerParam;
        }

        public String getMatrixParam() {
            return matrixParam;
        }

        public void setMatrixParam(String matrixParam) {
            this.matrixParam = matrixParam;
        }

        public String getPathParam() {
            return pathParam;
        }

        public void setPathParam(String pathParam) {
            this.pathParam = pathParam;
        }

        public String getQueryParam() {
            return queryParam;
        }

        public void setQueryParam(String queryParam) {
            this.queryParam = queryParam;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public boolean isOverrideRequestNull() {
            return overrideRequestNull;
        }

        public void setOverrideRequestNull(boolean overrideRequestNull) {
            this.overrideRequestNull = overrideRequestNull;
        }

        public String getUnknownAnnotationParam() {
            return unknownAnnotationParam;
        }

        public void setUnknownAnnotationParam(String unknownAnnotationParam) {
            this.unknownAnnotationParam = unknownAnnotationParam;
        }
    }
}
