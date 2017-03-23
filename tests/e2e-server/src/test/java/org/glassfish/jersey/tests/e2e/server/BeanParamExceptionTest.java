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
package org.glassfish.jersey.tests.e2e.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * Tests the ability to catch WebApplicationException thrown in ParamConverter
 * used along with BeanParam annotation.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 **/
public class BeanParamExceptionTest extends JerseyTest {

    private static final String PARAM_NOT_FOUND = "{\"message\":\"This parameter was not found\",\"status\":400}";

    @Override
    protected Application configure() {
        return new ResourceConfig(
                BeanParamController.class,
                ModelObjectParamConverter.class,
                QueryParamErrorMapper.class,
                JacksonJaxbJsonProvider.class);
    }

    @Path("/")
    @Produces("application/json")
    public static class BeanParamController {

        @GET
        @Path("/query")
        public String queryParam(@QueryParam("queryParam") final ModelObject modelObject) {
            return "Query Param: " + modelObject.toString();
        }

        @GET
        @Path("/bean")
        public String beanParam(@BeanParam final BeanParamObject beanParamObject) {
            return "Bean Param: " + beanParamObject.getModelObject().toString();
        }

    }

    @Provider
    public static class ModelObjectParamConverter implements ParamConverter<ModelObject>, ParamConverterProvider {

        @Override
        public ModelObject fromString(final String s) {
            if ("exception".equalsIgnoreCase(s)) {
                throw new BadParameterException("This parameter was not found");
            }
            return new ModelObject(s);
        }

        @Override
        public String toString(ModelObject modelObject) {
            return modelObject.toString();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
            return aClass.getName().equals(ModelObject.class.getName()) ? (ParamConverter<T>) this : null;
        }

    }

    @Provider
    private static class QueryParamErrorMapper implements ExceptionMapper<ParamException.QueryParamException> {

        @Override
        public Response toResponse(final ParamException.QueryParamException e) {
            Response.Status status = Response.Status.BAD_REQUEST;
            final Throwable cause = e.getCause();
            if (cause instanceof BadParameterException) {
                return Response.status(status).entity(new ErrorMessage(status.getStatusCode(), cause.getMessage())).build();
            }
            return null;
        }
    }

    @Test
    public void testMarshallExceptionQuery() {
        Response response = target().path("query").queryParam("queryParam", "exception")
                .request(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(400, response.getStatus());
        assertEquals(PARAM_NOT_FOUND, response.readEntity(String.class));
    }

    @Test
    public void testMarshallExceptionBean() {
        Response response = target().path("bean").queryParam("queryParam", "exception")
                .request(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(400, response.getStatus());
        assertEquals(PARAM_NOT_FOUND, response.readEntity(String.class));
    }

    @Test
    public void testMarshallModelQuery() {
        Response response = target().path("query").queryParam("queryParam", "model")
                .request(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(200, response.getStatus());
        assertEquals("Query Param: model", response.readEntity(String.class));
    }

    @Test
    public void testMarshallModelBean() {
        Response response = target().path("bean").queryParam("queryParam", "model")
                .request(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(200, response.getStatus());
        assertEquals("Bean Param: model", response.readEntity(String.class));
    }

    private static class BadParameterException extends RuntimeException {

        public BadParameterException(final String s) {
            super(s);
        }
    }

    public static class BeanParamObject {

        final ModelObject modelObject;

        public BeanParamObject(@QueryParam("queryParam") final ModelObject modelObject) {
            this.modelObject = modelObject;
        }

        public ModelObject getModelObject() {
            return modelObject;
        }
    }

    public static class ModelObject {

        private final String privateData;

        public ModelObject(final String privateData) {
            this.privateData = privateData;
        }

        @Override
        public String toString() {
            return privateData;
        }
    }

    public static class ErrorMessage {

        private final String message;

        private final int status;

        public ErrorMessage(final int status, final String message) {
            this.message = message;
            this.status = status;
        }

        @JsonProperty
        public String getMessage() {
            return message;
        }

        @JsonProperty
        public int getStatus() {
            return status;
        }
    }

}
