/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.linking.internal;

import org.glassfish.jersey.media.linking.LinkHeader;
import org.glassfish.jersey.media.linking.internal.LinkProcessor;
import org.glassfish.jersey.media.linking.LinkHeader.Extension;
import org.glassfish.jersey.media.linking.LinkHeaders;
import org.glassfish.jersey.media.linking.Ref;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import junit.framework.TestCase;

/**
 *
 * @author mh124079
 */
public class LinkProcessorTest extends TestCase {

    UriInfo mockUriInfo;

    public LinkProcessorTest(String name) {
        super(name);
        mockUriInfo = new UriInfo() {

            private final static String baseURI = "http://example.com/application/resources";

            public String getPath() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public String getPath(boolean decode) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public List<PathSegment> getPathSegments() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public List<PathSegment> getPathSegments(boolean decode) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public URI getRequestUri() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public UriBuilder getRequestUriBuilder() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public URI getAbsolutePath() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public UriBuilder getAbsolutePathBuilder() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public URI getBaseUri() {
                return getBaseUriBuilder().build();
            }

            public UriBuilder getBaseUriBuilder() {
                return UriBuilder.fromUri(baseURI);
            }

            public MultivaluedMap<String, String> getPathParameters() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public MultivaluedMap<String, String> getPathParameters(boolean decode) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public MultivaluedMap<String, String> getQueryParameters() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public List<String> getMatchedURIs() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public List<String> getMatchedURIs(boolean decode) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public List<Object> getMatchedResources() {
                Object dummyResource = new Object(){};
                return Collections.singletonList(dummyResource);
            }

            @Override
            public URI resolve(URI uri) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public URI relativize(URI uri) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };
    }

    @LinkHeader(@Ref(value="A"))
    public static class EntityA {
    }

    public void testLiteral() {
        System.out.println("Literal");
        LinkProcessor<EntityA> instance = new LinkProcessor(EntityA.class);
        EntityA testClass = new EntityA();
        List<String> headerValues = instance.getLinkHeaderValues(testClass, mockUriInfo);
        assertEquals(1, headerValues.size());
        String headerValue = headerValues.get(0);
        assertEquals("</application/resources/A>", headerValue);
    }

    @LinkHeader(@Ref(value="${entity.id}"))
    public static class EntityB {
        public String getId() {
            return "B";
        }
    }

    public void testEL() {
        System.out.println("EL");
        LinkProcessor<EntityB> instance = new LinkProcessor(EntityB.class);
        EntityB testClass = new EntityB();
        List<String> headerValues = instance.getLinkHeaderValues(testClass, mockUriInfo);
        assertEquals(1, headerValues.size());
        String headerValue = headerValues.get(0);
        assertEquals("</application/resources/B>", headerValue);
    }

    @LinkHeader(@Ref(value="{id}"))
    public static class EntityC {
        public String getId() {
            return "C";
        }
    }

    public void testTemplateLiteral() {
        System.out.println("Template Literal");
        LinkProcessor<EntityC> instance = new LinkProcessor(EntityC.class);
        EntityC testClass = new EntityC();
        List<String> headerValues = instance.getLinkHeaderValues(testClass, mockUriInfo);
        assertEquals(1, headerValues.size());
        String headerValue = headerValues.get(0);
        assertEquals("</application/resources/C>", headerValue);
    }

    @LinkHeaders({
        @LinkHeader(@Ref(value="A")),
        @LinkHeader(@Ref(value="B"))
    })
    public static class EntityD {
    }

    public void testMultiple() {
        System.out.println("Multiple Literal");
        LinkProcessor<EntityD> instance = new LinkProcessor(EntityD.class);
        EntityD testClass = new EntityD();
        List<String> headerValues = instance.getLinkHeaderValues(testClass, mockUriInfo);
        assertEquals(2, headerValues.size());
        // not sure if annotation order is supposed to be preserved but it seems
        // to work as expected
        String headerValue = headerValues.get(0);
        assertEquals("</application/resources/A>", headerValue);
        headerValue = headerValues.get(1);
        assertEquals("</application/resources/B>", headerValue);
    }

    @LinkHeader(
        value=@Ref(value="E"),
        rel="relE",
        rev="revE",
        type="type/e",
        title="titleE",
        anchor="anchorE",
        media="mediaE",
        hreflang="en-E",
        extensions={
            @Extension(name="e1", value="v1"),
            @Extension(name="e2", value="v2")
        }
    )
    public static class EntityE {
    }

    public void testParameters() {
        System.out.println("Parameters");
        LinkProcessor<EntityE> instance = new LinkProcessor(EntityE.class);
        EntityE testClass = new EntityE();
        List<String> headerValues = instance.getLinkHeaderValues(testClass, mockUriInfo);
        assertEquals(1, headerValues.size());
        String headerValue = headerValues.get(0);
        assertTrue(headerValue.contains("</application/resources/E>"));
        assertTrue(headerValue.contains("; rel=\"relE\""));
        assertTrue(headerValue.contains("; rev=\"revE\""));
        assertTrue(headerValue.contains("; type=\"type/e\""));
        assertTrue(headerValue.contains("; title=\"titleE\""));
        assertTrue(headerValue.contains("; anchor=\"anchorE\""));
        assertTrue(headerValue.contains("; media=\"mediaE\""));
        assertTrue(headerValue.contains("; hreflang=\"en-E\""));
        assertTrue(headerValue.contains("; e1=\"v1\""));
        assertTrue(headerValue.contains("; e2=\"v2\""));
    }

    @LinkHeaders({
        @LinkHeader(@Ref(value="${entity.id1}", condition="${entity.id1Enabled}")),
        @LinkHeader(@Ref(value="${entity.id2}", condition="${entity.id2Enabled}"))
    })
    public static class EntityF {
        public boolean isId1Enabled() {
            return true;
        }
        public String getId1() {
            return "1";
        }
        public boolean isId2Enabled() {
            return false;
        }
        public String getId2() {
            return "2";
        }
    }

    public void testConditional() {
        System.out.println("EL");
        LinkProcessor<EntityF> instance = new LinkProcessor(EntityF.class);
        EntityF testClass = new EntityF();
        List<String> headerValues = instance.getLinkHeaderValues(testClass, mockUriInfo);
        assertEquals(1, headerValues.size());
        String headerValue = headerValues.get(0);
        assertEquals("</application/resources/1>", headerValue);
    }

}
