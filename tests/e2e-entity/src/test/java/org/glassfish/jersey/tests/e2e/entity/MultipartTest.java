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

package org.glassfish.jersey.tests.e2e.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Martin Matula
 */
public class MultipartTest extends JerseyTest {

    @SuppressWarnings("UnusedDeclaration")
    public static class MyObject {

        private String value;

        public MyObject() {
        }

        public MyObject(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Path("/")
    public static class MultipartResource {

        @POST
        @Path("filename")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String filename(final FormDataMultiPart entity) {
            return entity.getField("text").getValue() + entity.getField("file").getContentDisposition().getFileName();
        }

        @POST
        @Path("mbr")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public Response mbr(final FormDataMultiPart entity) {
            entity.getField("text").getValueAs(MultipartResource.class);

            return Response.ok("ko").build();
        }

        @POST
        @Path("listAsParameter")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String process(@FormDataParam(value = "object") final MyObject object,
                              @FormDataParam(value = "list") final List<MyObject> list) {
            String value = object.value;

            for (final MyObject obj : list) {
                value += "_" + obj.value;
            }

            return value;
        }
    }

    public static class MessageBodyProvider
            implements MessageBodyReader<MultipartResource>, MessageBodyWriter<MultipartResource> {

        @Override
        public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                  final MediaType mediaType) {
            return true;
        }

        @Override
        public MultipartResource readFrom(final Class<MultipartResource> type, final Type genericType,
                                          final Annotation[] annotations, final MediaType mediaType,
                                          final MultivaluedMap<String, String> httpHeaders,
                                          final InputStream entityStream) throws IOException, WebApplicationException {
            throw new IOException("IOE");
        }

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(final MultipartResource multipartResource, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final MultipartResource multipartResource, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType,
                            final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
                throws IOException, WebApplicationException {

            throw new IOException("IOE");
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(MultipartResource.class, MessageBodyProvider.class)
                .register(MultiPartFeature.class)
                .register(JacksonFeature.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(MultiPartFeature.class);
        config.register(JacksonFeature.class);
    }

    @Test
    public void testFileNameInternetExplorer() throws Exception {
        final InputStream entity = getClass().getResourceAsStream("multipart-testcase.txt");
        final Response response = target("filename")
                .request()
                .header("User-Agent", "Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US)")
                .post(Entity.entity(entity, "multipart/form-data; boundary=---------------------------7dc941520888"));

        assertThat(response.readEntity(String.class), equalTo("bhhklbpom.xml"));
    }

    @Test
    public void testFileName() throws Exception {
        final InputStream entity = getClass().getResourceAsStream("multipart-testcase.txt");
        final Response response = target("filename")
                .request()
                .post(Entity.entity(entity, "multipart/form-data; boundary=---------------------------7dc941520888"));

        assertThat(response.readEntity(String.class), equalTo("bhhklbC:javaprojectsmultipart-testcasepom.xml"));
    }

    @Test
    public void testMbrExceptionServer() throws Exception {
        final InputStream entity = getClass().getResourceAsStream("multipart-testcase.txt");
        final Response response = target("mbr")
                .request()
                .post(Entity.entity(entity, "multipart/form-data; boundary=---------------------------7dc941520888"));

        assertThat(response.getStatus(), equalTo(500));
    }

    /**
     * Test that injection of a list (specific type) works.
     */
    @Test
    public void testSpecificListAsParameter() throws Exception {
        final MyObject object = new MyObject("object");
        final List<MyObject> list = Arrays.asList(new MyObject("list1"), new MyObject("list2"));

        final FormDataMultiPart mp = new FormDataMultiPart();
        mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("object").fileName("object").build(),
                object, MediaType.APPLICATION_JSON_TYPE));
        mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("list").fileName("list").build(),
                list, MediaType.APPLICATION_JSON_TYPE));

        final Response response = target("listAsParameter")
                .request().post(Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE));

        assertThat(response.readEntity(String.class), is("object_list1_list2"));
    }

    @Test
    public void testEmptyEntity() throws Exception {
        final Response response = target("filename")
                .request()
                .post(Entity.entity(null, MediaType.MULTIPART_FORM_DATA_TYPE));

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testEmptyEntityWithoutContentType() throws Exception {
        final Response response = target("filename")
                .request()
                .post(null);

        assertThat(response.getStatus(), is(400));
    }

}
