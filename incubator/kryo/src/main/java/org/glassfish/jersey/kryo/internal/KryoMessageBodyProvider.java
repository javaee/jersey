/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.kryo.internal;

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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

/**
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
@Provider
@Consumes("application/x-kryo")
@Produces("application/x-kryo")
public class KryoMessageBodyProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    private final KryoPool kryoPool;

    public KryoMessageBodyProvider() {
        final KryoFactory kryoFactory = new KryoFactory() {
            public Kryo create() {
                final Kryo kryo = new Kryo();
                //TODO: configure kryo instance, customize settings
                //TODO: e.g. looking for Kryo via ContextResolver (like Jackson)
                return kryo;
            }
        };
        kryoPool = new KryoPool.Builder(kryoFactory).softReferences().build();
    }

    //
    // MessageBodyWriter
    //

    @Override
    public long getSize(final Object object, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
                               final Annotation[] annotations, final MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(final Object object, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
            throws IOException, WebApplicationException {
        final Output output = new Output(entityStream);
        kryoPool.run(new KryoCallback() {
            public Object execute(Kryo kryo) {
                kryo.writeObject(output, object);
                return null;
            }
        });
        output.flush();
    }

    //
    // MessageBodyReader
    //

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType,
                              final Annotation[] annotations, final MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(final Class<Object> type, final Type genericType,
                           final Annotation[] annotations, final MediaType mediaType,
                           final MultivaluedMap<String, String> httpHeaders,
                           final InputStream entityStream) throws IOException, WebApplicationException {
        final Input input = new Input(entityStream);

        return kryoPool.run(new KryoCallback() {
            public Object execute(Kryo kryo) {
                return kryo.readObject(input, type);
            }
        });
    }

}
