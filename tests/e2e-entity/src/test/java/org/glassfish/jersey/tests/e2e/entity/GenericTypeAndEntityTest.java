/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 *
 * @author Paul Sandoz
 * @author Martin Matula
 */
public class GenericTypeAndEntityTest extends AbstractTypeTester {

    @Provider
    @SuppressWarnings("UnusedDeclaration")
    public static class ListIntegerWriter implements MessageBodyReader<List<Integer>>, MessageBodyWriter<List<Integer>> {

        private final Type t;

        public ListIntegerWriter() {
            final List<Integer> l = new ArrayList<>();
            final GenericEntity<List<Integer>> ge = new GenericEntity<List<Integer>>(l) {};
            this.t = ge.getType();
        }

        public boolean isWriteable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return this.t.equals(t);
        }

        public long getSize(final List<Integer> l, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        public void writeTo(final List<Integer> l, final Class<?> c, final Type t, final Annotation[] as,
                            final MediaType mt, final MultivaluedMap<String, Object> hs,
                            final OutputStream out) throws IOException, WebApplicationException {
            final StringBuilder sb = new StringBuilder();
            for (final Integer i : l) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(i);
            }
            out.write(sb.toString().getBytes());
        }

        @Override
        public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                  final MediaType mediaType) {
            return this.t.equals(genericType);
        }

        @Override
        public List<Integer> readFrom(final Class<List<Integer>> type, final Type genericType, final Annotation[] annotations,
                                      final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                                      final InputStream entityStream) throws IOException, WebApplicationException {
            return Arrays.stream(ReaderWriter.readFromAsString(entityStream, mediaType).split(","))
                         .map(input -> Integer.valueOf(input.trim()))
                         .collect(Collectors.toList());
        }
    }

    public static class GenericListResource<T> {

        @POST
        @Path("genericType")
        public List<T> post(final List<T> post) {
            return post;
        }
    }

    @Path("ListResource")
    public static class ListResource extends GenericListResource<Integer> {

        @GET
        @Path("type")
        public List<Integer> type() {
            return Arrays.asList(1, 2, 3, 4);
        }

        @GET
        @Path("genericEntity")
        public GenericEntity<List<Integer>> genericEntity() {
            return new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {};
        }

        @GET
        @Path("object")
        public Object object() {
            return new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {};
        }

        @GET
        @Path("response")
        public Response response() {
            return Response.ok(new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {}).build();
        }

        @GET
        @Path("wrongGenericEntity")
        public GenericEntity<List<Integer>> wrongGenericEntity() {
            // wrongly constructed generic entity: generic type of the generic entity
            // is not generic but just a List interface type. In this case
            // the return generic type will be used
            return new GenericEntity<>(Arrays.asList(1, 2, 3, 4), List.class);
        }
    }

    public static class GenericListMediaTypeResource<T> {

        @POST
        @Path("genericType")
        @Produces("text/plain")
        public List<T> post(final List<T> post) {
            return post;
        }
    }

    @Path("ListResourceWithMediaType")
    public static class ListResourceWithMediaType extends GenericListMediaTypeResource<Integer> {

        @GET
        @Path("type")
        @Produces("text/plain")
        public List<Integer> type() {
            return Arrays.asList(1, 2, 3, 4);
        }

        @GET
        @Path("genericEntity")
        @Produces("text/plain")
        public GenericEntity<List<Integer>> genericEntity() {
            return new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {};
        }

        @GET
        @Path("object")
        @Produces("text/plain")
        public Object object() {
            return new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {};
        }

        @GET
        @Path("response")
        @Produces("text/plain")
        public Response response() {
            return Response.ok(new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {}).build();
        }

        @GET
        @Path("wrongGenericEntity")
        @Produces("text/plain")
        public GenericEntity<List<Integer>> wrongGenericEntity() {
            // wrongly constructed generic entity: generic type of the generic entity
            // is not generic but just a List interface type. In this case
            // the return generic type will be used
            return new GenericEntity<>(Arrays.asList(1, 2, 3, 4), List.class);
        }
    }

    @Test
    public void testGenericType() {
        _genericTest(ListResource.class);
    }

    @Test
    public void testGenericTypeWithMediaType() {
        _genericTest(ListResourceWithMediaType.class);
    }

    private void _genericTest(final Class resourceClass) {
        final WebTarget target = target(resourceClass.getSimpleName());

        _testPath(target, "type");
        _testPath(target, "genericEntity");
        _testPath(target, "object");
        _testPath(target, "response");
        _testPath(target, "wrongGenericEntity");

        _testPathPost(target, "genericType");
    }

    private void _testPath(final WebTarget target, final String path) {
        assertEquals("1, 2, 3, 4", target.path(path).request().get(String.class));
    }

    private void _testPathPost(final WebTarget target, final String path) {
        assertEquals("1, 2, 3, 4", target.path(path).request().post(Entity.text("1, 2, 3, 4"), String.class));
    }

    @Provider
    public static class MapStringReader implements MessageBodyReader<Map<String, String>>,
                                                   MessageBodyWriter<Map<String, String>> {

        private final Type mapStringType;

        public MapStringReader() {
            final ParameterizedType iface = (ParameterizedType) this.getClass().getGenericInterfaces()[0];
            mapStringType = iface.getActualTypeArguments()[0];
        }

        public boolean isReadable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return Map.class == c && mapStringType.equals(t);
        }

        public Map<String, String> readFrom(final Class<Map<String, String>> c, final Type t, final Annotation[] as,
                                            final MediaType mt, final MultivaluedMap<String, String> headers,
                                            final InputStream in) throws IOException {
            final String[] v = ReaderWriter.readFromAsString(in, mt).split(",");
            final Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i < v.length; i = i + 2) {
                m.put(v[i], v[i + 1]);
            }
            return m;
        }

        @Override
        public boolean isWriteable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return Map.class.isAssignableFrom(c) && mapStringType.equals(t);
        }

        @Override
        public long getSize(final Map<String, String> t, final Class<?> type, final Type genericType, final Annotation[] as,
                            final MediaType mt) {
            return -1;
        }

        @Override
        public void writeTo(final Map<String, String> t, final Class<?> c, final Type genericType, final Annotation[] as,
                            final MediaType mt, final MultivaluedMap<String, Object> hs,
                            final OutputStream out) throws IOException, WebApplicationException {
            final StringBuilder sb = new StringBuilder();
            for (final Map.Entry<String, String> e : t.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(e.getKey()).append(',').append(e.getValue());
            }
            out.write(sb.toString().getBytes());
        }
    }

    public static class GenericMapResource<K, V> {

        @POST
        public Map<K, V> post(final Map<K, V> m) {
            return m;
        }
    }

    @Path("/MapResource")
    public static class MapResource extends GenericMapResource<String, String> {

    }

    @Test
    public void testGenericMap() throws Exception {
        assertThat(target("/MapResource").request().post(Entity.text("a,b,c,d"), String.class), is("a,b,c,d"));
    }

    @Provider
    public static class MapListStringReader implements MessageBodyReader<Map<String, List<String>>>,
                                                       MessageBodyWriter<Map<String, List<String>>> {

        private final Type mapListStringType;

        public MapListStringReader() {
            final ParameterizedType iface = (ParameterizedType) this.getClass().getGenericInterfaces()[0];
            mapListStringType = iface.getActualTypeArguments()[0];
        }

        public boolean isReadable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return Map.class == c && mapListStringType.equals(t);
        }

        public Map<String, List<String>> readFrom(final Class<Map<String, List<String>>> c, final Type t,
                                                  final Annotation[] as, final MediaType mt, final MultivaluedMap<String,
                String> headers, final InputStream in) throws IOException {
            try {
                final JSONObject o = new JSONObject(ReaderWriter.readFromAsString(in, mt));

                final Map<String, List<String>> m = new LinkedHashMap<>();
                final Iterator keys = o.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    final List<String> l = new ArrayList<>();
                    m.put(key, l);
                    final JSONArray a = o.getJSONArray(key);
                    for (int i = 0; i < a.length(); i++) {
                        l.add(a.getString(i));
                    }
                }
                return m;
            } catch (final JSONException ex) {
                throw new IOException(ex);
            }
        }

        @Override
        public boolean isWriteable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return Map.class.isAssignableFrom(c) && mapListStringType.equals(t);
        }

        @Override
        public long getSize(final Map<String, List<String>> t, final Class<?> type, final Type genericType,
                            final Annotation[] as, final MediaType mt) {
            return -1;
        }

        @Override
        public void writeTo(final Map<String, List<String>> t, final Class<?> c, final Type genericType, final Annotation[] as,
                            final MediaType mt, final MultivaluedMap<String, Object> hs,
                            final OutputStream out) throws IOException, WebApplicationException {
            try {
                final JSONObject o = new JSONObject();
                for (final Map.Entry<String, List<String>> e : t.entrySet()) {
                    o.put(e.getKey(), e.getValue());
                }
                out.write(o.toString().getBytes());
            } catch (final JSONException ex) {
                throw new IOException(ex);
            }
        }
    }

    public static class GenericMapListResource<K, V> {

        @POST
        public Map<K, List<V>> post(final Map<K, List<V>> m) {
            return m;
        }
    }

    @Path("/MapListResource")
    public static class MapListResource extends GenericMapListResource<String, String> {

    }

    @Test
    public void testGenericMapList() throws Exception {
        final String json = "{\"a\":[\"1\",\"2\"],\"b\":[\"1\",\"2\"]}";
        assertThat(target("/MapListResource").request().post(Entity.text(json), String.class), is(json));
    }
}
