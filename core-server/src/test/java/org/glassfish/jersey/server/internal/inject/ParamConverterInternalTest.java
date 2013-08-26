/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests {@link ParamConverter param converters}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ParamConverterInternalTest extends AbstractTest {
    @Path("/")
    public static class BadDateResource {
        @GET
        public String doGet(@QueryParam("d") Date d) {
            return "DATE";
        }
    }

    @Test
    public void testBadDateResource() throws ExecutionException, InterruptedException {
        initiateWebApplication(BadDateResource.class);

        final ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").queryParam("d",
                "123").build().toString());

        assertEquals(404, responseContext.getStatus());
    }


    @Path("/")
    public static class BadEnumResource {
        public enum ABC {
            A, B, C;
        }

        ;

        @GET
        public String doGet(@QueryParam("d") ABC d) {
            return "ENUM";
        }
    }

    @Test
    public void testBadEnumResource() throws ExecutionException, InterruptedException {
        initiateWebApplication(BadEnumResource.class);

        final ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").queryParam("d",
                "123").build().toString());

        assertEquals(404, responseContext.getStatus());
    }

    public static class URIStringReaderProvider implements ParamConverterProvider {
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType != URI.class) return null;

            return (ParamConverter<T>) new ParamConverter<URI>() {
                public URI fromString(String value) {
                    return URI.create(value);
                }

                @Override
                public String toString(URI value) throws IllegalArgumentException {
                    return value.toString();
                }
            };
        }
    }

    @Path("/")
    public static class BadURIResource {
        @GET
        public String doGet(@QueryParam("d") URI d) {
            return "URI";
        }
    }

    @Test
    public void testBadURIResource() throws ExecutionException, InterruptedException {
        initiateWebApplication(BadURIResource.class, URIStringReaderProvider.class);
        final ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").queryParam("d",
                "::::123").build().toString());
        assertEquals(404, responseContext.getStatus());
    }

    public static class ListOfStringReaderProvider implements ParamConverterProvider {


        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType != List.class) return null;

            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                if (parameterizedType.getActualTypeArguments().length != 1) return null;

                if (parameterizedType.getActualTypeArguments()[0] != String.class) return null;
            } else {
                return null;
            }

            return (ParamConverter<T>) new ParamConverter<List<String>>() {
                @Override
                public List<String> fromString(String value) {
                    return Arrays.asList(value.split(","));
                }

                @Override
                public String toString(List<String> value) throws IllegalArgumentException {
                    return value.toString();
                }
            };

        }
    }

    @Path("/")
    public static class ListOfStringResource {
        @GET
        public String doGet(@QueryParam("l") List<List<String>> l) {
            return l.toString();
        }
    }

    @Test
    public void testListOfStringReaderProvider() throws ExecutionException, InterruptedException {
        initiateWebApplication(ListOfStringResource.class, ListOfStringReaderProvider.class);
        final ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").queryParam("l", "1,2," +
                "3").build().toString());

        String s = (String) responseContext.getEntity();

        assertEquals(Collections.singletonList(Arrays.asList("1", "2", "3")).toString(), s);
    }

    @Test()
    public void testEagerConverter() throws Exception {
        try {
            new ApplicationHandler(new ResourceConfig(MyEagerParamProvider.class, Resource.class));
            fail("ExtractorException expected.");
        } catch (ExtractorException expected) {
            // ok
        }
    }

    @Test
    public void testLazyConverter() throws Exception {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(MyLazyParamProvider.class, Resource.class));
        ContainerResponse response = application.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(400, response.getStatus());
    }

    /**
     * This test verifies that the DateProvider is used for date string conversion instead of
     * string constructor that would be invoking deprecated Date(String) constructor.
     */
    @Test
    public void testDateParamConverterIsChosenForDateString() {
        initiateWebApplication();
        final ParamConverter<Date> converter =
                new ParamConverters.AggregatedProvider(app().getServiceLocator()).getConverter(Date.class, Date.class, null);

        assertEquals("Unexpected date converter provider class",
                ParamConverters.DateProvider.class, converter.getClass().getEnclosingClass());
    }

    @Path("resource")
    public static class Resource {
        @GET
        public String wrongDefaultValue(@HeaderParam("header") @DefaultValue("fail") MyBean header) {
            return "a";
        }
    }

    public static class MyEagerParamProvider implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType != MyBean.class) {
                return null;
            }

            return (ParamConverter<T>) new MyEagerParamConverter();
        }
    }

    public static class MyLazyParamProvider implements ParamConverterProvider {
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType != MyBean.class) {
                return null;
            }

            return (ParamConverter<T>) new MyLazyParamConverter();
        }
    }

    public static class MyAbstractParamConverter implements ParamConverter<MyBean> {
        @Override
        public MyBean fromString(String value) throws IllegalArgumentException {
            if ("fail".equals(value)) {
                throw new RuntimeException("fail");
            }
            final MyBean myBean = new MyBean();
            myBean.setValue("*" + value + "*");
            return myBean;
        }

        @Override
        public String toString(MyBean bean) throws IllegalArgumentException {
            return "*:" + bean.getValue().toString() + ":*";
        }
    }

    public static class MyEagerParamConverter extends MyAbstractParamConverter {
    }

    @ParamConverter.Lazy
    public static class MyLazyParamConverter extends MyAbstractParamConverter {
    }


    public static class MyBean {
        private String value;

        public MyBean() {
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "MyBean{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }

}
