/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.linking;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.zip.ZipEntry;
import javax.ws.rs.BeanParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;

import javax.xml.bind.annotation.XmlTransient;

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.linking.mapping.ResourceMappingContext;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.uri.UriTemplate;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
public class FieldProcessorTest {

    private static final Logger LOG = Logger.getLogger(FieldProcessor.class.getName());

    ExtendedUriInfo mockUriInfo = new ExtendedUriInfo() {

        private static final String baseURI = "http://example.com/application/resources";

        private MultivaluedMap queryParams = new MultivaluedStringMap();

        @Override
        public String getPath() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPath(boolean decode) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<PathSegment> getPathSegments() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<PathSegment> getPathSegments(boolean decode) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI getRequestUri() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder getRequestUriBuilder() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI getAbsolutePath() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder getAbsolutePathBuilder() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI getBaseUri() {
            return getBaseUriBuilder().build();
        }

        @Override
        public UriBuilder getBaseUriBuilder() {
            return UriBuilder.fromUri(baseURI);
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters() {
            return new MultivaluedStringMap();
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters(boolean decode) {
            return new MultivaluedStringMap();
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            return queryParams;
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
            return queryParams;
        }

        @Override
        public List<String> getMatchedURIs() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<String> getMatchedURIs(boolean decode) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Object> getMatchedResources() {
            Object dummyResource = new Object() {
            };
            return Collections.singletonList(dummyResource);
        }

        @Override
        public Throwable getMappedThrowable() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<MatchResult> getMatchedResults() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<UriTemplate> getMatchedTemplates() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<PathSegment> getPathSegments(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<PathSegment> getPathSegments(String name, boolean decode) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<RuntimeResource> getMatchedRuntimeResources() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ResourceMethod getMatchedResourceMethod() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource getMatchedModelResource() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<ResourceMethod> getMatchedResourceLocators() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Resource> getLocatorSubResources() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI resolve(URI uri) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI relativize(URI uri) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };

    private final ResourceMappingContext mockRmc = new ResourceMappingContext() {

        @Override
        public ResourceMappingContext.Mapping getMapping(Class<?> resource) {
            return null;
        }
    };

    private static final String TEMPLATE_A = "foo";

    public static class TestClassD {

        @InjectLink(value = TEMPLATE_A, style = InjectLink.Style.RELATIVE_PATH)
        private String res1;

        @InjectLink(value = TEMPLATE_A, style = InjectLink.Style.RELATIVE_PATH)
        private URI res2;
    }

    @Test
    public void testProcessLinks() {
        LOG.info("Links");

        FieldProcessor<TestClassD> instance = new FieldProcessor(TestClassD.class);
        TestClassD testClass = new TestClassD();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals(TEMPLATE_A, testClass.res1);
        assertEquals(TEMPLATE_A, testClass.res2.toString());
    }

    private static final String TEMPLATE_B = "widgets/{id}";

    public static class TestClassE {

        @InjectLink(value = TEMPLATE_B, style = InjectLink.Style.RELATIVE_PATH)
        private String link;

        private String id;

        public TestClassE(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    @Test
    public void testProcessLinksWithFields() {
        LOG.info("Links from field values");
        FieldProcessor<TestClassE> instance = new FieldProcessor(TestClassE.class);
        TestClassE testClass = new TestClassE("10");
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("widgets/10", testClass.link);
    }

    public static class TestClassF {

        @InjectLink(value = TEMPLATE_B, style = InjectLink.Style.RELATIVE_PATH)
        private String thelink;

        private String id;
        private TestClassE nested;

        public TestClassF(String id, TestClassE e) {
            this.id = id;
            this.nested = e;
        }

        public String getId() {
            return id;
        }
    }

    @Test
    public void testNesting() {
        LOG.info("Nesting");
        FieldProcessor<TestClassF> instance = new FieldProcessor(TestClassF.class);
        TestClassE nested = new TestClassE("10");
        TestClassF testClass = new TestClassF("20", nested);
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("widgets/20", testClass.thelink);
        assertEquals("widgets/10", testClass.nested.link);
    }

    @Test
    public void testArray() {
        LOG.info("Array");
        FieldProcessor<TestClassE[]> instance = new FieldProcessor(TestClassE[].class);
        TestClassE item1 = new TestClassE("10");
        TestClassE item2 = new TestClassE("20");
        TestClassE array[] = {item1, item2};
        instance.processLinks(array, mockUriInfo, mockRmc);
        assertEquals("widgets/10", array[0].link);
        assertEquals("widgets/20", array[1].link);
    }

    @Test
    public void testCollection() {
        LOG.info("Collection");
        FieldProcessor<List> instance = new FieldProcessor(List.class);
        TestClassE item1 = new TestClassE("10");
        TestClassE item2 = new TestClassE("20");
        List<TestClassE> list = Arrays.asList(item1, item2);
        instance.processLinks(list, mockUriInfo, mockRmc);
        assertEquals("widgets/10", list.get(0).link);
        assertEquals("widgets/20", list.get(1).link);
    }

    @Test
    public void testMap() {
        LOG.info("Map");
        FieldProcessor<Map> instance = new FieldProcessor(Map.class);
        TestClassE item1 = new TestClassE("10");
        TestClassE item2 = new TestClassE("20");
        Map<String, TestClassE> map = new HashMap<>();
        for (TestClassE item : Arrays.asList(item1, item2)) {
            map.put(item.getId(), item);
        }
        instance.processLinks(map, mockUriInfo, mockRmc);
        assertEquals("widgets/10", map.get("10").link);
        assertEquals("widgets/20", map.get("20").link);
    }

    public static class TestClassG {

        @InjectLink(value = TEMPLATE_B, style = InjectLink.Style.RELATIVE_PATH)
        private String relativePath;

        @InjectLink(value = TEMPLATE_B, style = InjectLink.Style.ABSOLUTE_PATH)
        private String absolutePath;

        @InjectLink(value = TEMPLATE_B, style = InjectLink.Style.ABSOLUTE)
        private String absolute;

        @InjectLink(TEMPLATE_B)
        private String defaultStyle;

        private String id;

        public TestClassG(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    @Test
    public void testLinkStyles() {
        LOG.info("Link styles");
        FieldProcessor<TestClassG> instance = new FieldProcessor(TestClassG.class);
        TestClassG testClass = new TestClassG("10");
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("widgets/10", testClass.relativePath);
        assertEquals("/application/resources/widgets/10", testClass.absolutePath);
        assertEquals("/application/resources/widgets/10", testClass.defaultStyle);
        assertEquals("http://example.com/application/resources/widgets/10", testClass.absolute);
    }

    public static class TestClassH {

        @InjectLink(TEMPLATE_B)
        private String link;

        public String getId() {
            return "10";
        }
    }

    @Test
    public void testComputedProperty() {
        LOG.info("Computed property");
        FieldProcessor<TestClassH> instance = new FieldProcessor(TestClassH.class);
        TestClassH testClass = new TestClassH();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/widgets/10", testClass.link);
    }

    public static class TestClassI {

        @InjectLink("widgets/${entity.id}")
        private String link;

        public String getId() {
            return "10";
        }
    }

    @Test
    public void testEL() {
        LOG.info("El link");
        FieldProcessor<TestClassI> instance = new FieldProcessor(TestClassI.class);
        TestClassI testClass = new TestClassI();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/widgets/10", testClass.link);
    }

    public static class TestClassJ {

        @InjectLink("widgets/${entity.id}/widget/{id}")
        private String link;

        public String getId() {
            return "10";
        }
    }

    @Test
    public void testMixed() {
        LOG.info("Mixed EL and template vars link");
        FieldProcessor<TestClassJ> instance = new FieldProcessor(TestClassJ.class);
        TestClassJ testClass = new TestClassJ();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/widgets/10/widget/10", testClass.link);
    }

    public static class DependentInnerBean {

        @InjectLink("${entity.id}")
        public String outerUri;
        @InjectLink("${instance.id}")
        public String innerUri;

        public String getId() {
            return "inner";
        }
    }

    public static class OuterBean {

        public DependentInnerBean inner = new DependentInnerBean();

        public String getId() {
            return "outer";
        }
    }

    @Test
    public void testELScopes() {
        LOG.info("EL scopes");
        FieldProcessor<OuterBean> instance = new FieldProcessor(OuterBean.class);
        OuterBean testClass = new OuterBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/inner", testClass.inner.innerUri);
        assertEquals("/application/resources/outer", testClass.inner.outerUri);
    }

    public static class BoundLinkBean {

        @InjectLink(value = "{id}", bindings = {@Binding(name = "id", value = "${instance.name}")})
        public String uri;

        public String getName() {
            return "name";
        }
    }

    @Test
    public void testELBinding() {
        LOG.info("EL binding");
        FieldProcessor<BoundLinkBean> instance = new FieldProcessor(BoundLinkBean.class);
        BoundLinkBean testClass = new BoundLinkBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/name", testClass.uri);
    }

    public static class BoundLinkOnLinkBean {

        @InjectLink(value = "{id}",
                bindings = {@Binding(name = "id", value = "${instance.name}")},
                rel = "self")
        public Link link;

        public String getName() {
            return "name";
        }
    }

    @Test
    public void testELBindingOnLink() {
        LOG.info("EL binding");
        FieldProcessor<BoundLinkOnLinkBean> instance = new FieldProcessor(BoundLinkOnLinkBean.class);
        BoundLinkOnLinkBean testClass = new BoundLinkOnLinkBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/name", testClass.link.getUri().toString());
        assertEquals("self", testClass.link.getRel());
    }

    public static class BoundLinkOnLinksBean {

        @InjectLinks({
                @InjectLink(value = "{id}",
                        bindings = {@Binding(name = "id", value = "${instance.name}")},
                        rel = "self"),
                @InjectLink(value = "{id}",
                        bindings = {@Binding(name = "id", value = "${instance.name}")},
                        rel = "other"),

        })
        public List<Link> links;

        @InjectLinks({
                @InjectLink(value = "{id}",
                        bindings = {@Binding(name = "id", value = "${instance.name}")},
                        rel = "self"),
                @InjectLink(value = "{id}",
                        bindings = {@Binding(name = "id", value = "${instance.name}")},
                        rel = "other"),

        })
        public Link[] linksArray;

        public String getName() {
            return "name";
        }
    }

    @Test
    public void testELBindingOnLinks() {
        LOG.info("EL binding");
        FieldProcessor<BoundLinkOnLinksBean> instance = new FieldProcessor(BoundLinkOnLinksBean.class);
        BoundLinkOnLinksBean testClass = new BoundLinkOnLinksBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/name", testClass.links.get(0).getUri().toString());
        assertEquals("self", testClass.links.get(0).getRel());
        assertEquals("other", testClass.links.get(1).getRel());

        assertEquals("/application/resources/name", testClass.linksArray[0].getUri().toString());
        assertEquals("self", testClass.linksArray[0].getRel());
        assertEquals("other", testClass.linksArray[1].getRel());

    }

    public static class ConditionalLinkBean {

        @InjectLink(value = "{id}", condition = "${entity.uri1Enabled}")
        public String uri1;

        @InjectLink(value = "{id}", condition = "${entity.uri2Enabled}")
        public String uri2;

        public String getId() {
            return "name";
        }

        public boolean isUri1Enabled() {
            return true;
        }

        public boolean isUri2Enabled() {
            return false;
        }
    }

    @Test
    public void testCondition() {
        LOG.info("Condition");
        FieldProcessor<ConditionalLinkBean> instance = new FieldProcessor(ConditionalLinkBean.class);
        ConditionalLinkBean testClass = new ConditionalLinkBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/name", testClass.uri1);
        assertEquals(null, testClass.uri2);
    }

    @Path("a")
    public static class SubResource {

        @Path("b")
        @GET
        public String getB() {
            return "hello world";
        }
    }

    public static class SubResourceBean {

        @InjectLink(resource = SubResource.class, method = "getB")
        public String uri;
    }

    @Test
    public void testSubresource() {
        LOG.info("Subresource");
        FieldProcessor<SubResourceBean> instance = new FieldProcessor(SubResourceBean.class);
        SubResourceBean testClass = new SubResourceBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/a/b", testClass.uri);
    }

    @Path("a")
    public static class QueryResource {

        @Path("b")
        @GET
        public String getB(@QueryParam("query") String query, @QueryParam("query2") String query2) {
            return "hello world";
        }
    }

    public static class QueryResourceBean {

        public String getQueryParam() {
            return queryExample;
        }

        private String queryExample;

        public QueryResourceBean(String queryExample, String queryExample2) {
            this.queryExample = queryExample;
            this.queryExample2 = queryExample2;
        }

        public String getQueryParam2() {
            return queryExample2;
        }

        private String queryExample2;

        @InjectLink(resource = QueryResource.class, method = "getB",
                bindings = {
                        @Binding(name = "query", value = "${instance.queryParam}"),
                        @Binding(name = "query2", value = "${instance.queryParam2}")
                })
        public String uri;
    }

    public static class QueryResourceBeanNoBindings {
        //query parameters will be populated from uriInfo
        //JERSEY-2863
        @InjectLink(resource = QueryResource.class, method = "getB")
        public String uri;
    }

    @Test
    public void testQueryResource() {
        LOG.info("QueryResource");
        FieldProcessor<QueryResourceBean> instance = new FieldProcessor(QueryResourceBean.class);
        QueryResourceBean testClass = new QueryResourceBean("queryExample", null);
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/a/b?query=queryExample&query2=", testClass.uri);
    }

    @Test
    public void testDoubleQueryResource() {
        LOG.info("QueryResource");
        FieldProcessor<QueryResourceBean> instance = new FieldProcessor(QueryResourceBean.class);
        QueryResourceBean testClass = new QueryResourceBean("queryExample", "queryExample2");
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/a/b?query=queryExample&query2=queryExample2", testClass.uri);
    }

    @Test
    public void testQueryResourceWithoutBindings() {
        LOG.info("QueryResource");
        FieldProcessor<QueryResourceBeanNoBindings> instance = new FieldProcessor(QueryResourceBeanNoBindings.class);
        QueryResourceBeanNoBindings testClass = new QueryResourceBeanNoBindings();
        mockUriInfo.getQueryParameters().putSingle("query", "queryExample");
        mockUriInfo.getQueryParameters().putSingle("query2", "queryExample2");
        assertEquals("queryExample", mockUriInfo.getQueryParameters().getFirst("query"));
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/a/b?query=queryExample&query2=queryExample2", testClass.uri);
        //clean mock
        mockUriInfo.getQueryParameters().clear();
    }

    /** Bean param with method setter QueryParam. */
    public static class BeanParamBeanA {
        private String qparam;
        @QueryParam("qparam")
        public void setQParam(String qparam) {
            this.qparam = qparam;
        }
    }

    /** Bean param with field QueryParam. */
    public static class BeanParamBeanB {
        @QueryParam("query") public String query;
    }

    @Path("a")
    public static class BeanParamQueryResource {

        @Path("b")
        @GET
        public String getB(@BeanParam BeanParamBeanA beanParamBeanA, @BeanParam BeanParamBeanB beanParamBeanB) {
            return "hello world";
        }
    }

    public static class BeanParamResourceBean {

        public String getQueryParam() {
            return queryExample;
        }

        private String queryExample;

        public BeanParamResourceBean(String queryExample) {
            this.queryExample = queryExample;
        }

        @InjectLink(resource = BeanParamQueryResource.class, method = "getB",
                bindings = {
                        @Binding(name = "query", value = "${instance.queryParam}"),
                        @Binding(name = "qparam", value = "foo")
                })
        public String uri;
    }

    @Test
    public void testBeanParamResource() {
        LOG.info("BeanParamResource");
        FieldProcessor<BeanParamResourceBean> instance = new FieldProcessor(BeanParamResourceBean.class);
        BeanParamResourceBean testClass = new BeanParamResourceBean("queryExample");
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/a/b?qparam=foo&query=queryExample", testClass.uri);
    }

    public static class TestClassK {

        public static final ZipEntry zipEntry = new ZipEntry("entry");
    }

    public static class TestClassL {

        public final ZipEntry zipEntry = new ZipEntry("entry");
    }

    private class LoggingFilter implements Filter {

        private int count = 0;

        @Override
        public synchronized boolean isLoggable(LogRecord logRecord) {
            if (logRecord.getThrown() instanceof IllegalAccessException) {
                count++;
                return false;
            }
            return true;
        }

        public int getCount() {
            return count;
        }
    }

    @Test
    public void testKL() {
        final LoggingFilter lf = new LoggingFilter();

        Logger.getLogger(FieldDescriptor.class.getName()).setFilter(lf);
        assertTrue(lf.getCount() == 0);

        FieldProcessor<TestClassK> instanceK = new FieldProcessor(TestClassK.class);
        TestClassK testClassK = new TestClassK();
        instanceK.processLinks(testClassK, mockUriInfo, mockRmc);

        assertTrue(lf.getCount() == 0);

        FieldProcessor<TestClassL> instanceL = new FieldProcessor(TestClassL.class);
        TestClassL testClassL = new TestClassL();
        instanceL.processLinks(testClassL, mockUriInfo, mockRmc);

        assertTrue(lf.getCount() == 0);

        Logger.getLogger(FieldDescriptor.class.getName()).setFilter(null);

    }

    public static class TestClassM {

        @InjectLink(value = TEMPLATE_B, style = InjectLink.Style.RELATIVE_PATH)
        private String thelink;

        private String id;

        @InjectLinkNoFollow
        private TestClassE nested;

        @XmlTransient
        private TestClassE transientNested;

        public TestClassM(String id, TestClassE e, TestClassE transientNested) {
            this.id = id;
            this.nested = e;
            this.transientNested = transientNested;
        }

        public String getId() {
            return id;
        }
    }

    @Test
    public void testNoRecursiveNesting() {
        LOG.info("No Recursive Nesting");
        FieldProcessor<TestClassM> instance = new FieldProcessor(TestClassM.class);
        TestClassE nested = new TestClassE("10");
        TestClassE transientNested = new TestClassE("30");
        TestClassM testClass = new TestClassM("20", nested, transientNested);
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("widgets/20", testClass.thelink);
        assertEquals(null, testClass.nested.link);
        assertEquals(null, testClass.transientNested.link);
    }

    public static class TestClassN {
        // Simulate object injected by JPA
        // in order to test a fix for JERSEY-2625
        private transient Iterable res1 = new Iterable() {
            @Override
            public Iterator iterator() {
                throw new RuntimeException("Declarative linking feature is incorrectly processing a transient iterator");
            }

        };
    }

    @Test
    public void testIgnoreTransient() {
        TestClassN testClass = new TestClassN();
        FieldProcessor<TestClassN> instance = new FieldProcessor(TestClassN.class);
        instance.processLinks(testClass, mockUriInfo, mockRmc);
    }

}
