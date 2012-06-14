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
package org.glassfish.jersey.server.internal.inject;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
import org.glassfish.jersey.spi.StringValueReaderProvider;

import org.glassfish.hk2.scopes.Singleton;

import com.sun.hk2.component.InjectionResolver;

/**
 * Module providing support for JAX-RS context injection. Namely, standard injection
 * support for the following set of JAX-RS context annotations is provided by
 * the module:
 * <dl>
 *
 * <dt>{@link javax.ws.rs.core.Context @Context}</dt>
 * <dd>
 * Generic support for the {@code @Context}-based injection is provided so that
 * the {@code @Context} annotation can be used interchangeably with e.g. standard
 * {@code @Inject} dependency injection annotation.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.PathParam @PathParam}</dt>
 * <dd>
 * Support for request URI path parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.QueryParam @QueryParam}</dt>
 * <dd>
 * Support for request URI query parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.MatrixParam @MatrixParam}</dt>
 * <dd>
 * Support for request URI matrix path parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.HeaderParam @HeaderParam}</dt>
 * <dd>
 * Support for request header parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.FormParam @FormParam}</dt>
 * <dd>
 * Support for form parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.CookieParam @CookieParam}</dt>
 * <dd>
 * Support for cookie parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * </dl>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ParameterInjectionModule extends AbstractModule {

    @Override
    public void configure() {
        // String reader providers
        bind(StringValueReaderProvider.class).to(StringReaderProviders.TypeFromStringEnum.class).in(Singleton.class);
        bind(StringValueReaderProvider.class).to(StringReaderProviders.TypeValueOf.class).in(Singleton.class);
        bind(StringValueReaderProvider.class).to(StringReaderProviders.TypeFromString.class).in(Singleton.class);
        bind(StringValueReaderProvider.class).to(StringReaderProviders.StringConstructor.class).in(Singleton.class);
        bind(StringValueReaderProvider.class).to(StringReaderProviders.DateProvider.class).in(Singleton.class);
        bind(StringValueReaderProvider.class).to(JaxbStringReaderProvider.RootElementProvider.class);
        bind().to(StringReaderFactory.class).in(Singleton.class);

        // Parameter injection value extractor providers
        bind(MultivaluedParameterExtractorProvider.class).to(MultivaluedParameterExtractorFactory.class).in(Singleton.class);

        // Parameter injection value providers
        bind(ValueFactoryProvider.class).to(PathParamValueFactoryProvider.class).in(Singleton.class);
        bind(ValueFactoryProvider.class).to(QueryParamValueFactoryProvider.class).in(Singleton.class);
        bind(ValueFactoryProvider.class).to(MatrixParamValueFactoryProvider.class).in(Singleton.class);
        bind(ValueFactoryProvider.class).to(HeaderParamValueFactoryProvider.class).in(Singleton.class);
        bind(ValueFactoryProvider.class).to(FormParamValueFactoryProvider.class).in(Singleton.class);
        bind(ValueFactoryProvider.class).to(CookieParamValueFactoryProvider.class).in(Singleton.class);
        bind(ValueFactoryProvider.class).to(EntityParamValueFactoryProvider.class).in(Singleton.class);
        bind(ValueFactoryProvider.class).to(DelegatedInjectionValueFactoryProvider.class).in(Singleton.class);

        // Injection resolvers
        // @XxxParam
        bind(InjectionResolver.class).to(PathParamValueFactoryProvider.InjectionResolver.class).in(Singleton.class);
        bind(InjectionResolver.class).to(QueryParamValueFactoryProvider.InjectionResolver.class).in(Singleton.class);
        bind(InjectionResolver.class).to(MatrixParamValueFactoryProvider.InjectionResolver.class).in(Singleton.class);
        bind(InjectionResolver.class).to(HeaderParamValueFactoryProvider.InjectionResolver.class).in(Singleton.class);
        bind(InjectionResolver.class).to(FormParamValueFactoryProvider.InjectionResolver.class).in(Singleton.class);
        bind(InjectionResolver.class).to(CookieParamValueFactoryProvider.InjectionResolver.class).in(Singleton.class);
    }
}
