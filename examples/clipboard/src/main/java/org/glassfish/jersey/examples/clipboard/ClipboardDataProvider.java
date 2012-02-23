/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.clipboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class ClipboardDataProvider implements MessageBodyWriter, MessageBodyReader {

    @Provider
    @Consumes("text/plain")
    @Produces("text/plain")
    public static class TextPlain extends ClipboardDataProvider {

        @Override
        public void writeTo(Object t, Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
            System.out.println("****writeTo on " + this.getClass().getName() + " invoked");
            entityStream.write(t.toString().getBytes());
        }

        @Override
        public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            System.out.println("****readFrom on " + this.getClass().getName() + " invoked");
            return new ClipboardData(readStringFromStream(entityStream));
        }
    }

    @Provider
    @Consumes("application/json")
    @Produces("application/json")
    public static class ApplicationJson extends ClipboardDataProvider {

        private static final String JsonOpenning = "{\"content\":\"";
        private static final String JsonClosing = "\"}";

        @Override
        public void writeTo(Object t, Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(String.format("%s%s%s", JsonOpenning, t.toString(), JsonClosing).getBytes());
        }

        @Override
        public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            final String jsonExpression = readStringFromStream(entityStream);
            return new ClipboardData(jsonExpression.replace(JsonOpenning, "").replace(JsonClosing, ""));
        }
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isKnownType(type);
    }

    private boolean isKnownType(Class<?> type) {
        return type.isAssignableFrom(ClipboardData.class);
    }

    @Override
    public long getSize(Object t, Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isKnownType(type);
    }

    private static String readStringFromStream(InputStream entityStream) throws IOException {
        StringBuilder result = new StringBuilder();
        byte[] buf = new byte[2048];
        int i;
        while ((i = entityStream.read(buf)) != -1) {
            result.append(new String(buf, 0, i));
        }
        return result.toString();
    }
}
