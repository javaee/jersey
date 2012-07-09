/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.media.json;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.inject.Singleton;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.media.json.internal.entity.JsonWithPaddingProvider;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

/**
 * Module with JAX-RS Jackson JSON providers.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JsonJacksonModule extends AbstractModule {

    private static final Collection<Class<?>> PROVIDERS = Collections.unmodifiableList(Arrays.asList(new Class<?>[]{
                JacksonJsonProvider.class,
                JacksonJaxbJsonProvider.class,
                JsonWithPaddingProvider.class
            }));

    /**
     * Get providers used for serialization and de-serialization of entities
     * to/from JSON media type.
     *
     * @return {@link Collection} of providers.
     */
    public static Collection<Class<?>> getProviders() {
        return PROVIDERS;
    }

    @Override
    protected void configure() {
        bindSingletonReaderWriterProvider(JacksonJsonProvider.class);
        bindSingletonReaderWriterProvider(JacksonJaxbJsonProvider.class);
        bindSingletonReaderWriterProvider(JsonWithPaddingProvider.class);
    }

    private <T extends MessageBodyReader<?> & MessageBodyWriter<?>> void bindSingletonReaderWriterProvider(Class<T> provider) {
        bind(BuilderHelper.link(provider).to(MessageBodyReader.class).to(MessageBodyWriter.class).in(Singleton.class).build());
    }
}
