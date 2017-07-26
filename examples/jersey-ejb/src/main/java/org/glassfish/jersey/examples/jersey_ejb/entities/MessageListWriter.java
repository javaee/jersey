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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.jersey_ejb.entities;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import javax.ejb.Stateless;

import org.glassfish.jersey.message.MessageUtils;

/**
 * A simple HTML message body writer to serialize list of message beans.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Stateless
@Provider
public class MessageListWriter implements MessageBodyWriter<List<Message>> {

    @Context
    private javax.inject.Provider<UriInfo> ui;

    @Override
    public boolean isWriteable(final Class<?> clazz, final Type type, final Annotation[] annotation, final MediaType mediaType) {
        return verifyGenericType(type);
    }

    private boolean verifyGenericType(final Type genericType) {
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }

        final ParameterizedType pt = (ParameterizedType) genericType;

        if (pt.getActualTypeArguments().length > 1) {
            return false;
        }

        if (!(pt.getActualTypeArguments()[0] instanceof Class)) {
            return false;
        }

        final Class listClass = (Class) pt.getActualTypeArguments()[0];
        return listClass == Message.class;
    }

    @Override
    public long getSize(final List<Message> messages,
                        final Class<?> clazz,
                        final Type type,
                        final Annotation[] annotation,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final List<Message> messages,
                        final Class<?> clazz,
                        final Type type,
                        final Annotation[] annotation,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> arg5,
                        final OutputStream ostream) throws IOException, WebApplicationException {
        for (final Message m : messages) {
            ostream.write(m.toString().getBytes(MessageUtils.getCharset(mediaType)));
            final URI mUri = ui.get().getAbsolutePathBuilder().path(Integer.toString(m.getUniqueId())).build();
            ostream.write((" <a href='" + mUri.toASCIIString() + "'>link</a><br />").getBytes());
        }
    }
}
