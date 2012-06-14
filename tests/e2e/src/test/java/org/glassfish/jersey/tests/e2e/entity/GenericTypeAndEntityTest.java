/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class GenericTypeAndEntityTest extends AbstractTypeTester {
    @Provider
    public static class ListIntegerWriter implements MessageBodyWriter<List<Integer>> {
        private final Type t;

        public ListIntegerWriter() {
            List<Integer> l = new ArrayList<Integer>();
            GenericEntity<List<Integer>> ge = new GenericEntity<List<Integer>>(l) {};
            this.t = ge.getType();
        }

        public boolean isWriteable(Class<?> c, Type t, Annotation[] as, MediaType mt) {
            return this.t.equals(t);
        }

        public long getSize(List<Integer> l, Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
            return -1;
        }

        public void writeTo(List<Integer> l, Class<?> c, Type t, Annotation[] as,
                MediaType mt, MultivaluedMap<String, Object> hs,
                OutputStream out) throws IOException, WebApplicationException {
            StringBuffer sb = new StringBuffer();
            for (Integer i : l) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(i);
            }
            out.write(sb.toString().getBytes());
        }

    }

    @Path("ListResource")
    public static class ListResource {
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
    }

    @Path("ListResourceWithMediaType")
    public static class ListResourceWithMediaType {
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
    }

    @Test
    public void testGenericType() {
        _genericTest(ListResource.class);
    }

    @Test
    public void testGenericTypeWithMediaType() {
        _genericTest(ListResourceWithMediaType.class);
    }

    private void _genericTest(Class resourceClass) {
        WebTarget target = target(resourceClass.getSimpleName());

        _testPath(target, "type");
        _testPath(target, "genericEntity");
        _testPath(target, "object");
        _testPath(target, "response");
    }

    private void _testPath(WebTarget target, String path) {
        assertEquals("1, 2, 3, 4", target.path(path).request().get(String.class));
    }
}
