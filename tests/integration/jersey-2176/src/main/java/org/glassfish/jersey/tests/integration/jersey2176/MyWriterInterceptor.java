/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey2176;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.message.MessageUtils;


/**
 * This just set new context output stream and test a clone method on set output stream instance is called.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class MyWriterInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo(final WriterInterceptorContext context) throws IOException {
        final boolean fail = context.getHeaders().containsKey(Issue2176ReproducerResource.X_FAIL_HEADER);
        final boolean responseEntity = context.getHeaders().containsKey(Issue2176ReproducerResource.X_RESPONSE_ENTITY_HEADER);

        if (responseEntity) {
            context.setOutputStream(
                    new MyOutputStream(context.getOutputStream(), MessageUtils.getCharset(context.getMediaType())));
        }
        context.proceed();
        if (fail) {
            throw new IllegalStateException("From MyWriterInterceptor");
        }
    }

    private static class MyOutputStream extends OutputStream {
        private final OutputStream delegate;
        final Charset charset;
        private final ByteArrayOutputStream localStream;

        private MyOutputStream(final OutputStream delegate, final Charset charset) throws IOException {
            this.delegate = delegate;
            this.charset = charset;

            this.localStream = new ByteArrayOutputStream();
            localStream.write("[INTERCEPTOR]".getBytes(charset));
        }

        @Override
        public void write(final int b) throws IOException {
            localStream.write(b);
        }

        @Override
        public void flush() throws IOException {
            delegate.write(localStream.toByteArray());
            localStream.reset();
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            localStream.write("[/INTERCEPTOR]".getBytes(charset));

            delegate.write(localStream.toByteArray());

            delegate.close();
            localStream.close();
        }
    }

}
