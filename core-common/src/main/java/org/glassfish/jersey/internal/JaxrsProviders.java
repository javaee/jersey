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
package org.glassfish.jersey.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Jersey implementation of JAX-RS {@link javax.ws.rs.ext.Providers} contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JaxrsProviders implements javax.ws.rs.ext.Providers {

    /**
     * Jersey binder registering {@link javax.ws.rs.ext.Providers JAX-RS Providers} injection bindings.
     */
    public static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(JaxrsProviders.class).to(javax.ws.rs.ext.Providers.class).in(PerLookup.class);
        }
    }

    @Inject
    private Provider<MessageBodyWorkers> workers;
    @Inject
    private Provider<ContextResolvers> resolvers;
    @Inject
    private Provider<ExceptionMappers> mappers;

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType) {
        return workers.get().getMessageBodyReader(type, genericType, annotations, mediaType);
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType) {
        return workers.get().getMessageBodyWriter(type, genericType, annotations, mediaType);
    }

    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        // exception mappers are not supported on the client side
        final ExceptionMappers actualMappers = mappers.get();
        return (actualMappers != null) ? actualMappers.find(type) : null;
    }

    @Override
    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        return resolvers.get().resolve(contextType, mediaType);
    }
}
