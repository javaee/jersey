/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.container;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class ResponseWriterMetadataTest extends JerseyContainerTest {

    public static class ValueHolder {

        private String value;

        public ValueHolder(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Provider
    @Produces("text/plain")
    public static class ValueHolderWriter implements MessageBodyWriter<ValueHolder> {

        public boolean isWriteable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mediaType) {
            return ValueHolder.class == c;
        }

        public long getSize(final ValueHolder s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        public void writeTo(final ValueHolder s,
                            final Class<?> c,
                            final Type t,
                            final Annotation[] as,
                            final MediaType mt,
                            final MultivaluedMap<String, Object> headers,
                            final OutputStream out) throws IOException, WebApplicationException {

            headers.add("X-BEFORE-WRITE", "foo");
            out.write(s.value.getBytes());
            headers.add("X-AFTER-WRITE", "bar");
        }
    }

    @Path("/")
    public static class Resource {

        @GET
        public ValueHolder get() {
            return new ValueHolder("one");
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, ValueHolderWriter.class)
                .property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 1);
    }

    @Test
    public void testResponse() {
        final Response response = target().request().get();

        assertThat(response.readEntity(String.class), is("one"));
        assertThat(response.getHeaderString("X-BEFORE-WRITE"), is("foo"));

        if (factory instanceof InMemoryTestContainerFactory) {
            assertThat(response.getHeaderString("X-AFTER-WRITE"), is("bar"));
        } else {
            assertThat(response.getHeaderString("X-AFTER-WRITE"), nullValue());
        }
    }
}
