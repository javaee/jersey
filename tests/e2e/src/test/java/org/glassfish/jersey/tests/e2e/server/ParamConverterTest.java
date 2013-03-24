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
package org.glassfish.jersey.tests.e2e.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests {@link ParamConverter param converters} as e2e test.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ParamConverterTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, MyParamProvider.class, MyStringParamProvider.class);
    }

    @Test
    public void testMyBeanParam() {
        Form form = new Form();
        form.param("form", "formParam");
        final Response response = target().path("resource/myBean").path("pathParam").matrixParam("matrix",
                "matrixParam").queryParam
                ("query", "queryParam").request().header("header",
                "headerParam").cookie("cookie", "cookieParam").post(Entity.entity(form,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        final String str = response.readEntity(String.class);
        Assert.assertEquals("*pathParam*_*matrixParam*_*queryParam*_*headerParam*_*cookieParam*_*formParam*", str);

    }

    @Test
    public void testStringParam() {
        Form form = new Form();
        form.param("form", "formParam");
        final Response response = target().path("resource/string").path("pathParam").matrixParam("matrix",
                "matrixParam").queryParam
                ("query", "queryParam").request().header("header",
                "headerParam").cookie("cookie", "cookieParam").post(Entity.entity(form,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        final String str = response.readEntity(String.class);
        Assert.assertEquals("-pathParam-_-matrixParam-_-queryParam-_-headerParam-_-cookieParam-_-formParam-", str);

    }

    @Test
    @Ignore("TODO: ParamConversion not yet implemented in the ResponseBuilder (JERSEY-1385).")
    // TODO: JERSEY-1385: after clarifying with spec the ResponseBuilder paramconversion should be finished (or removed)
    public void testStringParamInResponse() {
        final Response response = target().path("resource/response").request().get();
        Assert.assertEquals("-:res-head:-", response.getHeaderString("response-header"));

    }

    @Path("resource")
    public static class Resource {
        @POST
        @Path("myBean/{path}")
        public String postMyBean(@PathParam("path") MyBean pathParam, @MatrixParam("matrix") MyBean matrix,
                                 @QueryParam("query") MyBean query, @HeaderParam("header") MyBean header,
                                 @CookieParam("cookie") MyBean cookie, @FormParam("form") MyBean form) {
            return pathParam.getValue() + "_" + matrix.getValue() + "_" + query.getValue() + "_" + header.getValue() + "_" +
                    cookie.getValue() + "_" + form.getValue();
        }


        @POST
        @Path("string/{path}")
        public String postString(@PathParam("path") String pathParam, @MatrixParam("matrix") String matrix,
                                 @QueryParam("query") String query, @HeaderParam("header") String header,
                                 @CookieParam("cookie") String cookie, @FormParam("form") String form) {
            return pathParam + "_" + matrix + "_" + query + "_" + header + "_" +
                    cookie + "_" + form;
        }

        @GET
        @Path("q")
        public String get(@QueryParam("query") String query) {
            return query;
        }


        @GET
        @Path("response")
        public Response getResponse() {
            return Response.ok().header("response-header", "res-head").entity("anything").build();
        }
    }


    public static class MyParamProvider implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType != MyBean.class) {
                return null;
            }

            return (ParamConverter<T>) new ParamConverter<MyBean>() {

                @Override
                public MyBean fromString(String value) throws IllegalArgumentException {
                    final MyBean myBean = new MyBean();
                    myBean.setValue("*" + value + "*");
                    return myBean;
                }

                @Override
                public String toString(MyBean bean) throws IllegalArgumentException {
                    return "*:" + bean.getValue().toString() + ":*";
                }

            };
        }
    }

    public static class MyStringParamProvider implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType != String.class) {
                return null;
            }

            return (ParamConverter<T>) new ParamConverter<String>() {

                @Override
                public String fromString(String value) throws IllegalArgumentException {
                    return "-" + value + "-";
                }

                @Override
                public String toString(String str) throws IllegalArgumentException {
                    return "-:" + str + ":-";
                }

            };
        }
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
