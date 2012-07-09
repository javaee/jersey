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
import org.glassfish.jersey.media.json.internal.entity.JsonArrayProvider;
import org.glassfish.jersey.media.json.internal.entity.JsonJaxbElementProvider;
import org.glassfish.jersey.media.json.internal.entity.JsonListElementProvider;
import org.glassfish.jersey.media.json.internal.entity.JsonObjectProvider;
import org.glassfish.jersey.media.json.internal.entity.JsonRootElementProvider;
import org.glassfish.jersey.media.json.internal.entity.JsonWithPaddingProvider;

/**
 * Module with JAX-RS JAXB JSON providers.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JsonJaxbModule extends AbstractModule {

    private static final Collection<Class<?>> PROVIDERS = Collections.unmodifiableList(Arrays.asList(new Class<?>[]{
                JsonRootElementProvider.App.class,
                JsonRootElementProvider.General.class,
                JsonJaxbElementProvider.App.class,
                JsonJaxbElementProvider.General.class,
                JsonListElementProvider.App.class,
                JsonListElementProvider.General.class,
                JsonArrayProvider.App.class,
                JsonArrayProvider.General.class,
                JsonObjectProvider.App.class,
                JsonObjectProvider.General.class,
                JsonWithPaddingProvider.class
            }));

    public static Collection<Class<?>> getProviders() {
        return PROVIDERS;
    }

    @Override
    protected void configure() {
        bindSingletonReaderWriterProvider(JsonRootElementProvider.App.class);
        bindSingletonReaderWriterProvider(JsonRootElementProvider.General.class);

        bindSingletonReaderWriterProvider(JsonJaxbElementProvider.App.class);
        bindSingletonReaderWriterProvider(JsonJaxbElementProvider.General.class);

        bindSingletonReaderWriterProvider(JsonListElementProvider.App.class);
        bindSingletonReaderWriterProvider(JsonListElementProvider.General.class);

        bindSingletonReaderWriterProvider(JsonArrayProvider.App.class);
        bindSingletonReaderWriterProvider(JsonArrayProvider.General.class);

        bindSingletonReaderWriterProvider(JsonObjectProvider.App.class);
        bindSingletonReaderWriterProvider(JsonObjectProvider.General.class);

        bind(BuilderHelper.link(JsonWithPaddingProvider.class).to(MessageBodyWriter.class).in(Singleton.class).build());
    }

    private <T extends MessageBodyReader<?> & MessageBodyWriter<?>> void bindSingletonReaderWriterProvider(Class<T> provider) {
        bind(BuilderHelper.link(provider).to(MessageBodyReader.class).to(MessageBodyWriter.class).in(Singleton.class).build());
    }
}
