/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.performance.mbw.parametrized;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.message.MessageUtils;

/**
 * Custom message body worker.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Produces("application/person")
@Consumes("application/person")
public class PersonProvider implements MessageBodyWriter<Person>, MessageBodyReader<Person> {

    @Override
    public boolean isWriteable(final Class<?> type, final Type type1, final Annotation[] antns, final MediaType mt) {
        return type == Person.class;
    }

    @Override
    public long getSize(final Person t, final Class<?> type, final Type type1, final Annotation[] antns, final MediaType mt) {
        return getByteRepresentation(t, MessageUtils.getCharset(mt)).length;
    }

    @Override
    public void writeTo(final Person t, final Class<?> type, final Type type1, final Annotation[] antns, final MediaType mt,
                        final MultivaluedMap<String, Object> mm, final OutputStream out)
            throws IOException, WebApplicationException {
        out.write(getByteRepresentation(t, MessageUtils.getCharset(mt)));
    }

    @Override
    public boolean isReadable(final Class<?> type, final Type type1, final Annotation[] antns, final MediaType mt) {
        return type == Person.class;
    }

    @Override
    public Person readFrom(final Class<Person> type, final Type type1, final Annotation[] antns, final MediaType mt,
                           final MultivaluedMap<String, String> mm, final InputStream in)
            throws IOException, WebApplicationException {
        final Person result = new Person();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        final String nameLine = reader.readLine();
        result.name = nameLine.substring(nameLine.indexOf(": ") + 2);
        final String ageLine = reader.readLine();
        result.age = Integer.parseInt(ageLine.substring(ageLine.indexOf(": ") + 2));
        final String addressLine = reader.readLine();
        result.address = addressLine.substring(addressLine.indexOf(": ") + 2);

        return result;
    }

    private byte[] getByteRepresentation(final Person t, final Charset charset) {
        return String.format("name: %s\nage: %d\naddress: %s", t.name, t.age, t.address).getBytes(charset);
    }
}
