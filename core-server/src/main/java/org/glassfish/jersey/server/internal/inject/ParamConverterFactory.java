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
package org.glassfish.jersey.server.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * An aggregate {@link ParamConverterProvider param converter provider} that loads all
 * the registered {@link ParamConverterProvider} implementations.
 * <p />
 * When invoked, the provider iterates through the registered implementations until
 * it finds the first implementation that returns a non-null {@link ParamConverter param converter},
 * which is subsequently returned from the factory. In case no non-null string reader
 * instance is found, {@code null} is returned from the factory. {@link org.glassfish.jersey.internal.inject.Custom Custom}
 * providers are iterated first, so that user registered providers are preferred against internal jersey providers.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
@Singleton
public class ParamConverterFactory implements ParamConverterProvider {

    private final List<ParamConverterProvider> converterProviders;

    @Inject
    ParamConverterFactory(ServiceLocator locator) {
        converterProviders = new ArrayList<>();
        final Set<ParamConverterProvider> customProviders = Providers.getCustomProviders(locator, ParamConverterProvider.class);
        converterProviders.addAll(customProviders);

        final Set<ParamConverterProvider> providers = Providers.getProviders(locator, ParamConverterProvider.class);
        providers.removeAll(customProviders);
        converterProviders.addAll(providers);

    }

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        for (ParamConverterProvider provider : converterProviders) {
            @SuppressWarnings("unchecked")
            ParamConverter<T> converter = provider.getConverter(rawType, genericType, annotations);
            if (converter != null) {
                return converter;
            }
        }
        return null;

    }
}
