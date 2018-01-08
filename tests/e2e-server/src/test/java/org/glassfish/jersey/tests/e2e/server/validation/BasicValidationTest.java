/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Gajdos
 */
public class BasicValidationTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        final ResourceConfig resourceConfig = new ResourceConfig(BasicResource.class);

        resourceConfig.register(ContactBeanProvider.class);
        resourceConfig.register(MoxyXmlFeature.class);

        resourceConfig.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        return resourceConfig;
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        super.configureClient(config);
        config.register(ContactBeanProvider.class);
        config.register(MoxyXmlFeature.class);
    }

    @Consumes("application/contactBean")
    @Produces("application/contactBean")
    @Provider
    public static class ContactBeanProvider implements MessageBodyReader<ContactBean>, MessageBodyWriter<ContactBean> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.equals(ContactBean.class);
        }

        @Override
        public ContactBean readFrom(Class<ContactBean> type,
                                    Type genericType,
                                    Annotation[] annotations,
                                    MediaType mediaType,
                                    MultivaluedMap<String, String> httpHeaders,
                                    InputStream entityStream) throws IOException, WebApplicationException {
            try {
                final ObjectInputStream objectInputStream = new ObjectInputStream(entityStream);
                return (ContactBean) objectInputStream.readObject();
            } catch (Exception e) {
                // do nothing.
            }
            return null;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.equals(ContactBean.class);
        }

        @Override
        public long getSize(ContactBean contactBean,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(ContactBean contactBean,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
            try {
                new ObjectOutputStream(entityStream).writeObject(contactBean);
            } catch (Exception e) {
                // do nothing.
            }
        }
    }

    private static final class ParamBean {

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

        public ParamBean() {
        }

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

        @Override
        public String toString() {
            return "Bean{"
                    + "cookie='" + cookie + '\''
                    + ", formParam='" + formParam + '\''
                    + ", headerParam='" + headerParam + '\''
                    + ", matrixParam='" + matrixParam + '\''
                    + ", pathParam='" + pathParam + '\''
                    + ", queryParam='" + queryParam + '\''
                    + '}';
        }
    }

    private ParamBean getDefaultParamBean() {
        final ParamBean paramBean = new ParamBean();
        paramBean.setCookie("cookieParam");
        paramBean.setFormParam("formParam");
        paramBean.setHeaderParam("headerParam");
        paramBean.setMatrixParam("matrixParam");
        paramBean.setPathParam("pathParam");
        paramBean.setQueryParam("queryParam");
        return paramBean;
    }

    private Response testInputParams(final String path, final ParamBean paramBean) throws Exception {
        final Form form = new Form();
        form.asMap().put("form", Arrays.asList(paramBean.getFormParam()));

        WebTarget target = target("beanvalidation").path(path);

        if (paramBean.getPathParam() != null) {
            target = target.path(paramBean.getPathParam());
        } else {
            target = target.path("/");
        }

        Invocation.Builder request = target
                .matrixParam("matrix", paramBean.getMatrixParam())
                .queryParam("query", paramBean.getQueryParam())
                .request();

        if (paramBean.getHeaderParam() != null) {
            request = request.header("header", paramBean.getHeaderParam());
        }

        if (paramBean.getCookie() != null) {
            request = request.cookie("cookie", paramBean.getCookie());
        }

        return request.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    @Test
    public void testInputParamsBasicConstraintsPositive() throws Exception {
        final Response response = testInputParams("basicParam", getDefaultParamBean());

        assertEquals(200, response.getStatus());
        assertEquals("pathParam_matrixParam_queryParam_headerParam_cookieParam_formParam", response.readEntity(String.class));
    }

    @Test
    public void testInputParamsBasicConstraintsNegative() throws Exception {
        final Response response = testInputParams("basicParam", new ParamBean());

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("arg1")); // @MatrixParam
        assertTrue(message.contains("arg2")); // @QueryParam
        assertTrue(message.contains("arg3")); // @HeaderParam
        assertTrue(message.contains("arg4")); // @CookieParam
        assertTrue(message.contains("arg5")); // @FormParam

        assertFalse(message.contains("arg0")); // @PathParam
        assertFalse(message.contains("arg6")); // @Context
    }

    @Test
    public void testInputParamsDefaultBasicConstraintsPositive() throws Exception {
        final Response response = testInputParams("basicDefaultParam", new ParamBean());

        assertEquals(200, response.getStatus());
        assertEquals("_matrixParam_queryParam_headerParam_cookieParam_formParam", response.readEntity(String.class));
    }

    @Test
    public void testInputParamsCustomConstraintsPositive() throws Exception {
        final Response response = testInputParams("customParam", getDefaultParamBean());

        assertEquals(200, response.getStatus());
        assertEquals("pathParam_matrixParam_queryParam_headerParam_cookieParam_formParam", response.readEntity(String.class));
    }

    @Test
    public void testInputParamsCustomConstraintsNegative() throws Exception {
        final Response response = testInputParams("customParam", new ParamBean());

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("arg0")); // @PathParam
        assertTrue(message.contains("arg1")); // @MatrixParam
        assertTrue(message.contains("arg2")); // @QueryParam
        assertTrue(message.contains("arg3")); // @HeaderParam
        assertTrue(message.contains("arg4")); // @CookieParam
        assertTrue(message.contains("arg5")); // @FormParam
    }

    @Test
    public void testInputParamsMixedConstraintsPositive() throws Exception {
        final Response response = testInputParams("mixedParam", getDefaultParamBean());

        assertEquals(200, response.getStatus());
        assertEquals("pathParam_matrixParam_queryParam_headerParam_cookieParam_formParam", response.readEntity(String.class));
    }

    @Test
    public void testInputParamsMixedConstraintsNegative() throws Exception {
        final Response response = testInputParams("mixedParam", new ParamBean());

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("arg0")); // @PathParam
        assertTrue(message.contains("arg1")); // @MatrixParam
        assertTrue(message.contains("arg2")); // @QueryParam
        assertTrue(message.contains("arg3")); // @HeaderParam
        assertTrue(message.contains("arg4")); // @CookieParam
        assertTrue(message.contains("arg5")); // @FormParam
    }

    @Test
    public void testInputParamsMixedConstraintsNegativeTooLong() throws Exception {
        final ParamBean defaultParamBean = getDefaultParamBean();
        final String formParam = defaultParamBean.getFormParam();
        defaultParamBean.setFormParam(formParam + formParam);

        final Response response = testInputParams("mixedParam", defaultParamBean);

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertFalse(message.contains("arg0")); // @PathParam
        assertFalse(message.contains("arg1")); // @MatrixParam
        assertFalse(message.contains("arg2")); // @QueryParam
        assertFalse(message.contains("arg3")); // @HeaderParam
        assertFalse(message.contains("arg4")); // @CookieParam
        assertTrue(message.contains("arg5")); // @FormParam
    }

    @Test
    public void testInputParamsMultipleConstraintsPositive() throws Exception {
        final Response response = testInputParams("multipleParam", getDefaultParamBean());

        assertEquals(200, response.getStatus());
        assertEquals("pathParam_matrixParam_queryParam_headerParam_cookieParam_formParam", response.readEntity(String.class));
    }

    @Test
    public void testInputParamsMultipleConstraintsNegative() throws Exception {
        final Response response = testInputParams("multipleParam", new ParamBean());

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("arg0")); // @PathParam
        assertTrue(message.contains("arg1")); // @MatrixParam
        assertTrue(message.contains("arg2")); // @QueryParam
        assertTrue(message.contains("arg3")); // @HeaderParam
        assertTrue(message.contains("arg4")); // @CookieParam
        assertTrue(message.contains("arg5")); // @FormParam
    }

    @Test
    public void testInputParamsMultipleConstraintsNegativeTooLong() throws Exception {
        final ParamBean defaultParamBean = getDefaultParamBean();
        final String formParam = defaultParamBean.getFormParam();
        defaultParamBean.setFormParam(formParam + formParam);

        final Response response = testInputParams("mixedParam", defaultParamBean);

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertFalse(message.contains("arg0")); // @PathParam
        assertFalse(message.contains("arg1")); // @MatrixParam
        assertFalse(message.contains("arg2")); // @QueryParam
        assertFalse(message.contains("arg3")); // @HeaderParam
        assertFalse(message.contains("arg4")); // @CookieParam
        assertTrue(message.contains("arg5")); // @FormParam
    }

    private Response testBean(final String path, final ContactBean contactBean) {
        return target("beanvalidation")
                .path(path)
                .request("application/contactBean").post(Entity.entity(contactBean, "application/contactBean"));
    }

    @Test
    public void testEmptyBeanParamPositive() throws Exception {
        final ContactBean contactBean = new ContactBean();
        final Response response = testBean("emptyBeanParam", contactBean);

        assertEquals(contactBean, response.readEntity(ContactBean.class));
    }

    @Test
    public void testEmptyBeanParamNegative() throws Exception {
        final Response response = testBean("emptyBeanParam", null);

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("arg0"));
    }

    @Test
    public void testValidBeanParamPositive() throws Exception {
        final ContactBean contactBean = new ContactBean();
        contactBean.setName("Jersey");
        contactBean.setEmail("jersey@example.com");
        final Response response = testBean("validBeanParam", contactBean);

        assertEquals(contactBean, response.readEntity(ContactBean.class));
    }

    @Test
    public void testValidBeanParamNegative() throws Exception {
        final ContactBean contactBean = new ContactBean();
        // Add value to pass @OneContact constraint but fails on @Pattern constraint defined on getter.
        contactBean.setPhone("12");
        final Response response = testBean("validBeanParam", contactBean);

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("arg0"));
    }

    @Test
    public void testCustomBeanParamPositive() throws Exception {
        final ContactBean contactBean = new ContactBean();
        contactBean.setEmail("jersey@example.com");

        final Response response = testBean("customBeanParam", contactBean);

        assertEquals(contactBean, response.readEntity(ContactBean.class));
    }

    @Test
    public void testCustomBeanParamNegative() throws Exception {
        final ContactBean contactBean = new ContactBean();
        contactBean.setEmail("jersey@example.com");
        contactBean.setPhone("134539");

        final Response response = testBean("customBeanParam", contactBean);

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("arg0"));
    }

    @Test
    public void testEmptyBeanResponsePositive() throws Exception {
        final ContactBean contactBean = new ContactBean();
        final Response response = testBean("emptyBeanResponse", contactBean);

        assertEquals(contactBean, response.readEntity(ContactBean.class));
    }

    @Test
    public void testEmptyBeanResponseNegative() throws Exception {
        final Response response = testBean("emptyBeanResponse", null);

        assertEquals(500, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("return value"));
    }

    @Test
    public void testValidBeanResponsePositive() throws Exception {
        final ContactBean contactBean = new ContactBean();
        contactBean.setName("Jersey");
        contactBean.setEmail("jersey@example.com");
        final Response response = testBean("validBeanResponse", contactBean);

        assertEquals(contactBean, response.readEntity(ContactBean.class));
    }

    @Test
    public void testValidBeanResponseNegative() throws Exception {
        final ContactBean contactBean = new ContactBean();
        final Response response = testBean("validBeanResponse", contactBean);

        assertEquals(500, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("return value"));
    }

    @Test
    public void testValidBeanWrappedInResponsePositive() throws Exception {
        final ContactBean contactBean = new ContactBean();
        contactBean.setName("Jersey");
        contactBean.setEmail("jersey@example.com");
        final Response response = testBean("validBeanWrappedInResponse", contactBean);

        assertEquals(contactBean, response.readEntity(ContactBean.class));
    }

    @Test
    public void testValidBeanWrappedInResponseNegative() throws Exception {
        final ContactBean contactBean = new ContactBean();
        final Response response = testBean("validBeanWrappedInResponse", contactBean);

        assertEquals(500, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("return value"));
    }

    @Test
    public void testCustomBeanResponsePositive() throws Exception {
        final ContactBean contactBean = new ContactBean();
        contactBean.setEmail("jersey@example.com");
        final Response response = testBean("customBeanResponse", contactBean);

        assertEquals(contactBean, response.readEntity(ContactBean.class));
    }

    @Test
    public void testCustomBeanResponseNegative() throws Exception {
        final ContactBean contactBean = new ContactBean();
        contactBean.setEmail("jersey@example.com");
        contactBean.setPhone("134539");

        final Response response = testBean("customBeanResponse", contactBean);

        assertEquals(500, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("return value"));
    }

    @Test
    public void testInvalidContextValidation() throws Exception {
        final Response response = target("beanvalidation").path("invalidContext").request().get();
        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("arg0"));
    }

    private Response testSubResource(final String path, final Form form) throws Exception {
        return target("beanvalidation")
                .path("sub")
                .path(path)
                .request()
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    private void testSubResourcePositive(final String path) throws Exception {
        testSubResource(path, false);
    }

    private void testSubResource(final String path, final boolean omitEmail) throws Exception {
        final Form form = new Form();
        form.asMap().put("firstName", Arrays.asList("Jersey"));
        form.asMap().put("lastName", Arrays.asList("JAX-RS"));
        if (!omitEmail) {
            form.asMap().put("email", Arrays.asList("jersey@example.com"));
        }

        final ContactBean contactBean = new ContactBean();
        contactBean.setName("Jersey JAX-RS");
        contactBean.setEmail("jersey@example.com");

        final Response response = testSubResource(path, form);

        if (omitEmail) {
            assertEquals(400, response.getStatus());

            final String message = response.readEntity(String.class);
            assertTrue(message.contains("email"));
        } else {
            assertEquals(200, response.getStatus());
            assertEquals(contactBean, response.readEntity(ContactBean.class));
        }
    }

    private void testSubResourceNegative(final String path) throws Exception {
        final Form form = new Form();
        form.asMap().put("foo", Arrays.asList("bar"));

        final Response response = testSubResource(path, form);

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("firstName"));
        assertTrue(message.contains("lastName"));
        assertTrue(message.contains("email"));
    }

    @Test
    public void testSubResourceValidResourceContextPositive() throws Exception {
        testSubResourcePositive("validResourceContextInstance");
    }

    @Test
    public void testSubResourceNullResourceContextPositive() throws Exception {
        testSubResourcePositive("nullResourceContextInstance");
    }

    @Test
    public void testSubResourceNullResourceContextClassPositive() throws Exception {
        testSubResourcePositive("nullResourceContextClass");
    }

    @Test
    public void testSubResourceValidResourceContextNegative() throws Exception {
        testSubResourceNegative("validResourceContextInstance");
    }

    @Test
    public void testSubResourceNullResourceContextNegative() throws Exception {
        testSubResourceNegative("nullResourceContextInstance");
    }

    @Test
    public void testSubResourceEmptyNames() throws Exception {
        final Form form = new Form();
        form.asMap().put("firstName", Arrays.asList(""));
        form.asMap().put("lastName", Arrays.asList(""));

        final Response response = testSubResource("nullResourceContextInstance", form);

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("at least one of the names is empty"));
    }

    @Test
    public void testSubResourcePropertyNegative() throws Exception {
        testSubResource("validResourceContextInstance", true);
    }

    @Test
    public void testWrongSubResourceNegative() throws Exception {
        final Response response = target("beanvalidation")
                .path("sub/wrong")
                .request()
                .get();

        assertEquals(400, response.getStatus());

        final String message = response.readEntity(String.class);
        assertTrue(message.contains("resourceContext"));
    }
}
