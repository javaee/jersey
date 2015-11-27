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

package org.glassfish.jersey.linking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.zip.ZipEntry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.linking.mapping.ResourceMappingContext;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.uri.UriTemplate;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ryan Peterson
 */
public class MethodProcessorTest {
    private static final Logger LOG = Logger.getLogger(MethodProcessorTest.class.getName());

    ExtendedUriInfo mockUriInfo = new ExtendedUriInfo() {

        private final static String baseURI = "http://example.com/application/resources";

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
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters(boolean decode) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
            throw new UnsupportedOperationException("Not supported yet.");
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

    private final static String TEMPLATE_A = "foo";

    public static class TestClassD {
        private String res1;
        private URI res2;

        @InjectLink(value = TEMPLATE_A,
                style = InjectLink.Style.RELATIVE_PATH)
        public String getRes1() {
            return res1;
        }

        public void setRes1(String res1) {
            this.res1 = res1;
        }

        @InjectLink(value = TEMPLATE_A,
                style = InjectLink.Style.RELATIVE_PATH)
        public URI getRes2() {
            return res2;
        }

        public void setRes2(URI uri) {
            this.res2 = uri;
        }
    }

    @Test
    public void testProcessLinks() {
        LOG.info("Links");
        MethodProcessor<TestClassD> instance = new MethodProcessor(TestClassD.class);
        TestClassD testClass = new TestClassD();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals(TEMPLATE_A, testClass.res1);
        assertEquals(TEMPLATE_A, testClass.res2.toString());
    }

    private final static String TEMPLATE_B = "widgets/{id}";

    public static class TestClassE {
        private String link;

        private String id;

        public TestClassE(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @InjectLink(value = TEMPLATE_B,
                style = InjectLink.Style.RELATIVE_PATH)
        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }

    @Test
    public void testProcessLinksWithFields() {
        LOG.info("Links from field values");
        MethodProcessor<TestClassE> instance = new MethodProcessor(TestClassE.class);
        TestClassE testClass = new TestClassE("10");
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("widgets/10", testClass.link);
    }

    public static class TestClassF {
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

        @InjectLink(value = TEMPLATE_B,
                style = InjectLink.Style.RELATIVE_PATH)
        public String getThelink() {
            return thelink;
        }

        public void setThelink(String thelink) {
            this.thelink = thelink;
        }
    }

    @Test
    public void testNesting() {
        LOG.info("Nesting");
        MethodProcessor<TestClassF> instance = new MethodProcessor(TestClassF.class);
        TestClassE nested = new TestClassE("10");
        TestClassF testClass = new TestClassF("20", nested);
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("widgets/20", testClass.thelink);
        assertEquals("widgets/10", testClass.nested.link);
    }

    @Test
    public void testArray() {
        LOG.info("Array");
        MethodProcessor<TestClassE[]> instance = new MethodProcessor(TestClassE[].class);
        TestClassE item1 = new TestClassE("10");
        TestClassE item2 = new TestClassE("20");
        TestClassE array[] = { item1, item2 };
        instance.processLinks(array, mockUriInfo, mockRmc);
        assertEquals("widgets/10", array[0].link);
        assertEquals("widgets/20", array[1].link);
    }

    @Test
    public void testCollection() {
        LOG.info("Collection");
        MethodProcessor<List> instance = new MethodProcessor(List.class);
        TestClassE item1 = new TestClassE("10");
        TestClassE item2 = new TestClassE("20");
        List<TestClassE> list = Arrays.asList(item1, item2);
        instance.processLinks(list, mockUriInfo, mockRmc);
        assertEquals("widgets/10", list.get(0).link);
        assertEquals("widgets/20", list.get(1).link);
    }

    public static class TestClassG {
        private String relativePath;

        private String absolutePath;

        private String absolute;

        private String defaultStyle;

        private String id;

        public TestClassG(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @InjectLink(value = TEMPLATE_B,
                style = InjectLink.Style.RELATIVE_PATH)
        public String getRelativePath() {
            return relativePath;
        }

        public void setRelativePath(String relativePath) {
            this.relativePath = relativePath;
        }

        @InjectLink(value = TEMPLATE_B,
                style = InjectLink.Style.ABSOLUTE_PATH)
        public String getAbsolutePath() {
            return absolutePath;
        }

        public void setAbsolutePath(String absolutePath) {
            this.absolutePath = absolutePath;
        }

        @InjectLink(value = TEMPLATE_B,
                style = InjectLink.Style.ABSOLUTE)
        public String getAbsolute() {
            return absolute;
        }

        public void setAbsolute(String absolute) {
            this.absolute = absolute;
        }

        @InjectLink(TEMPLATE_B)
        public String getDefaultStyle() {
            return defaultStyle;
        }

        public void setDefaultStyle(String defaultStyle) {
            this.defaultStyle = defaultStyle;
        }
    }

    @Test
    public void testLinkStyles() {
        LOG.info("Link styles");
        MethodProcessor<TestClassG> instance = new MethodProcessor(TestClassG.class);
        TestClassG testClass = new TestClassG("10");
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("widgets/10", testClass.relativePath);
        assertEquals("/application/resources/widgets/10", testClass.absolutePath);
        assertEquals("/application/resources/widgets/10", testClass.defaultStyle);
        assertEquals("http://example.com/application/resources/widgets/10", testClass.absolute);
    }

    public static class TestClassH {
        private String link;

        public String getId() {
            return "10";
        }

        @InjectLink(TEMPLATE_B)
        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }

    @Test
    public void testComputedProperty() {
        LOG.info("Computed property");
        MethodProcessor<TestClassH> instance = new MethodProcessor(TestClassH.class);
        TestClassH testClass = new TestClassH();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/widgets/10", testClass.link);
    }

    public static class TestClassI {
        private String link;

        public String getId() {
            return "10";
        }

        @InjectLink("widgets/${entity.id}")
        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }

    @Test
    public void testEL() {
        LOG.info("EL link");
        MethodProcessor<TestClassI> instance = new MethodProcessor(TestClassI.class);
        TestClassI testClass = new TestClassI();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/widgets/10", testClass.link);
    }

    public static class TestClassJ {
        private String link;

        public String getId() {
            return "10";
        }

        @InjectLink("widgets/${entity.id}/widget/{id}")
        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }

    @Test
    public void testMixed() {
        LOG.info("Mixed EL and template vars link");
        MethodProcessor<TestClassJ> instance = new MethodProcessor(TestClassJ.class);
        TestClassJ testClass = new TestClassJ();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/widgets/10/widget/10", testClass.link);
    }

    public static class DependentInnerBean {
        public String outerUri;
        public String innerUri;

        public String getId() {
            return "inner";
        }

        @InjectLink("${entity.id}")
        public String getOuterUri() {
            return outerUri;
        }

        public void setOuterUri(String outerUri) {
            this.outerUri = outerUri;
        }

        @InjectLink("${instance.id}")
        public String getInnerUri() {
            return innerUri;
        }

        public void setInnerUri(String innerUri) {
            this.innerUri = innerUri;
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
        MethodProcessor<OuterBean> instance = new MethodProcessor(OuterBean.class);
        OuterBean testClass = new OuterBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/inner", testClass.inner.innerUri);
        assertEquals("/application/resources/outer", testClass.inner.outerUri);
    }

    public static class BoundLinkBean {
        public String uri;

        public String getName() {
            return "name";
        }

        @InjectLink(value = "{id}",
                bindings = { @Binding(name = "id",
                        value = "${instance.name}") })
        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    @Test
    public void testELBinding() {
        LOG.info("EL binding");
        MethodProcessor<BoundLinkBean> instance = new MethodProcessor(BoundLinkBean.class);
        BoundLinkBean testClass = new BoundLinkBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/name", testClass.uri);
    }

    public static class BoundLinkOnLinkBean {
        public Link link;

        public String getName() {
            return "name";
        }

        @InjectLink(value = "{id}",
                bindings = { @Binding(name = "id",
                        value = "${instance.name}") },
                rel = "self")
        public Link getLink() {
            return link;
        }

        public void setLink(Link link) {
            this.link = link;
        }
    }

    @Test
    public void testELBindingOnLink() {
        LOG.info("EL binding");
        MethodProcessor<BoundLinkOnLinkBean> instance = new MethodProcessor(BoundLinkOnLinkBean.class);
        BoundLinkOnLinkBean testClass = new BoundLinkOnLinkBean();
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/name", testClass.link.getUri().toString());
        assertEquals("self", testClass.link.getRel());
    }

    public static class BoundLinkOnLinksBean {
        public List<Link> links;
        public Link[] linksArray;

        public String getName() {
            return "name";
        }

        @InjectLinks({ @InjectLink(value = "{id}",
                bindings = { @Binding(name = "id",
                        value = "${instance.name}") },
                rel = "self"), @InjectLink(value = "{id}",
                bindings = { @Binding(name = "id",
                        value = "${instance.name}") },
                rel = "other"),

        })
        public List<Link> getLinks() {
            return links;
        }

        public void setLinks(List<Link> links) {
            this.links = links;
        }

        @InjectLinks({ @InjectLink(value = "{id}",
                bindings = { @Binding(name = "id",
                        value = "${instance.name}") },
                rel = "self"), @InjectLink(value = "{id}",
                bindings = { @Binding(name = "id",
                        value = "${instance.name}") },
                rel = "other"),

        })
        public Link[] getLinksArray() {
            return linksArray;
        }

        public void setLinksArray(Link[] linksArray) {
            this.linksArray = linksArray;
        }
    }

    @Test
    public void testELBindingOnLinks() {
        LOG.info("EL binding");
        MethodProcessor<BoundLinkOnLinksBean> instance = new MethodProcessor(BoundLinkOnLinksBean.class);
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
        public String uri1;

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

        @InjectLink(value = "{id}",
                condition = "${entity.uri1Enabled}")
        public String getUri1() {
            return uri1;
        }

        public void setUri1(String uri1) {
            this.uri1 = uri1;
        }

        @InjectLink(value = "{id}",
                condition = "${entity.uri2Enabled}")
        public String getUri2() {
            return uri2;
        }

        public void setUri2(String uri2) {
            this.uri2 = uri2;
        }
    }

    @Test
    public void testCondition() {
        LOG.info("Condition");
        MethodProcessor<ConditionalLinkBean> instance = new MethodProcessor(ConditionalLinkBean.class);
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
        public String uri;

        @InjectLink(resource = SubResource.class,
                method = "getB")
        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    @Test
    public void testSubresource() {
        LOG.info("Subresource");
        MethodProcessor<SubResourceBean> instance = new MethodProcessor(SubResourceBean.class);
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

        public String uri;

        @InjectLink(resource = QueryResource.class,
                method = "getB",
                bindings = { @Binding(name = "query",
                        value = "${instance.queryParam}"), @Binding(name = "query2",
                        value = "${instance.queryParam2}") })
        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    @Test
    public void testQueryResource() {
        LOG.info("QueryResource");
        MethodProcessor<QueryResourceBean> instance = new MethodProcessor(QueryResourceBean.class);
        QueryResourceBean testClass = new QueryResourceBean("queryExample", null);
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/a/b?query=queryExample&query2=", testClass.uri);
    }

    @Test
    public void testDoubleQueryResource() {
        LOG.info("QueryResource");
        MethodProcessor<QueryResourceBean> instance = new MethodProcessor(QueryResourceBean.class);
        QueryResourceBean testClass = new QueryResourceBean("queryExample", "queryExample2");
        instance.processLinks(testClass, mockUriInfo, mockRmc);
        assertEquals("/application/resources/a/b?query=queryExample&query2=queryExample2", testClass.uri);
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

        MethodProcessor<TestClassK> instanceK = new MethodProcessor(TestClassK.class);
        TestClassK testClassK = new TestClassK();
        instanceK.processLinks(testClassK, mockUriInfo, mockRmc);

        assertTrue(lf.getCount() == 0);

        MethodProcessor<TestClassL> instanceL = new MethodProcessor(TestClassL.class);
        TestClassL testClassL = new TestClassL();
        instanceL.processLinks(testClassL, mockUriInfo, mockRmc);

        assertTrue(lf.getCount() == 0);

        Logger.getLogger(FieldDescriptor.class.getName()).setFilter(null);

    }

    public static interface TestInterface {
        @InjectLink(value = TEMPLATE_A,
                style = InjectLink.Style.RELATIVE_PATH)
        public String getRes1();

        public void setRes1(String res1);
    }

    @Test
    public void testInterfaceProxy() {
        try {
            LOG.info("Test Interface");
            MethodProcessor<TestInterface> instance = new MethodProcessor(TestInterface.class);
            TestInterface testClass = TestInterface.class.cast(Proxy.newProxyInstance(TestInterface.class.getClassLoader(), new Class[] { TestInterface.class }, new InvocationHandler() {
                private String simpleValue;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String s = method.getName();
                    if (s.equals("hashCode")) { // Need this for map operations
                        return new Integer(0);
                    } else if (method.getName().startsWith("set")) {
                        simpleValue = (String) args[0];
                        return null;
                    } else {
                        return simpleValue;
                    }
                }

            }));
            instance.processLinks(testClass, mockUriInfo, mockRmc);
            assertEquals(TEMPLATE_A, testClass.getRes1());
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

    }
}
