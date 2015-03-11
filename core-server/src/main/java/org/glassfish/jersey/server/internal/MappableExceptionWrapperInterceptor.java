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

package org.glassfish.jersey.server.internal;

import java.io.IOException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;
import javax.inject.Singleton;

import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.glassfish.jersey.server.internal.process.MappableException;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Interceptor that wraps specific exceptions types thrown by wrapped interceptors and by message
 * body readers and writers into a mappable exception.
 * It must have the lowest priority in order to wrap all other interceptors.
 *
 * @author Miroslav Fuksa
 */
@Priority(10)
@Singleton
public class MappableExceptionWrapperInterceptor implements ReaderInterceptor, WriterInterceptor {

    @Override
    public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
        try {
            return context.proceed();
        } catch (final WebApplicationException | MappableException | MessageBodyProviderNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            throw new MappableException(e);
        }

    }

    @Override
    public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
        try {
            context.proceed();
        } catch (final WebApplicationException | MappableException e) {
            throw e;
        } catch (final MessageBodyProviderNotFoundException nfe) {
            throw new InternalServerErrorException(nfe);
        } catch (final Exception e) {
            throw new MappableException(e);
        }

    }

    /**
     * Binder registering the {@link MappableExceptionWrapperInterceptor Exception Wrapper Interceptor}
     * (used on the client side).
     *
     */
    public static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(MappableExceptionWrapperInterceptor.class)
                    .to(ReaderInterceptor.class)
                    .to(WriterInterceptor.class)
                    .in(Singleton.class);
        }
    }
}
