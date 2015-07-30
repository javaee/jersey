/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey1107;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * @author Michal Gajdos
 */
public class Jersey1107 extends Application {

    /**
     * This MessageBodyWriter does not support the "exception/nullpointerexception" media type required by the
     * {@code Resource#getNpe()} method which should result in an empty {@code MessageBodyWriter} and therefore
     * an NPE in {@code ApplicationHandler}.
     *
     * @see org.glassfish.jersey.tests.integration.jersey1107.Jersey1107.Resource#getNpe()
     */
    @Provider
    @Produces({"exception/ioexception", "exception/webapplicationexception"})
    public static class ExceptionThrower implements MessageBodyWriter<Exception> {

        @Override
        public boolean isWriteable(
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
            return IOException.class.isAssignableFrom(type) || RuntimeException.class.isAssignableFrom(type);
        }

        @Override
        public long getSize(
                Exception t,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(
                Exception e,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException, WebApplicationException {
            // Cannot write anything into the entityStream to ensure the ContainerResponseWriter#writeResponseStatusAndHeaders
            // in ApplicationHandler#writeResponse is not invoked.

            // Simply throw the given exception.
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw (RuntimeException) e;
            }
        }

    }

    @Path("/")
    public static class Resource {

        @GET
        @Path("/ioe")
        @Produces("exception/ioexception")
        public IOException getIoe() {
            return new IOException();
        }

        @GET
        @Path("/wae")
        @Produces("exception/webapplicationexception")
        public WebApplicationException getWae() {
            return new WebApplicationException();
        }

        @GET
        @Path("/npe")
        @Produces("exception/nullpointerexception")
        public NullPointerException getNpe() {
            return new NullPointerException("This message should never get to the client!");
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>() {{
            add(Resource.class);
            add(ExceptionThrower.class);
        }};
    }

}
