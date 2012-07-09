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

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
import org.glassfish.jersey.spi.StringValueReaderProvider;

import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.utilities.BuilderHelper;

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
 * <dt>{@link javax.ws.rs.Uri @Uri}</dt>
 * <dd>
 * Support for {@link javax.ws.rs.client.WebTarget} injection as defined by the JAX-RS specification.
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
        bind(BuilderHelper.link(StringReaderProviders.AggregatedProvider.class).to(StringValueReaderProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(StringReaderFactory.class).in(Singleton.class).build());

        // Parameter injection value extractor providers
        bind(BuilderHelper.link(MultivaluedParameterExtractorFactory.class).to(MultivaluedParameterExtractorProvider.class).in(Singleton.class).build());

        // Parameter injection value providers
        bind(BuilderHelper.link(PathParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(QueryParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(MatrixParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(HeaderParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(FormParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(CookieParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(EntityParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(DelegatedInjectionValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());
        bind(BuilderHelper.link(WebTargetValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class).build());

        // Injection resolvers
        // @XxxParam
        bind(BuilderHelper.link(PathParamValueFactoryProvider.InjectionResolver.class).to(InjectionResolver.class).in(Singleton.class).build());
        bind(BuilderHelper.link(QueryParamValueFactoryProvider.InjectionResolver.class).to(InjectionResolver.class).in(Singleton.class).build());
        bind(BuilderHelper.link(MatrixParamValueFactoryProvider.InjectionResolver.class).to(InjectionResolver.class).in(Singleton.class).build());
        bind(BuilderHelper.link(HeaderParamValueFactoryProvider.InjectionResolver.class).to(InjectionResolver.class).in(Singleton.class).build());
        bind(BuilderHelper.link(FormParamValueFactoryProvider.InjectionResolver.class).to(InjectionResolver.class).in(Singleton.class).build());
        bind(BuilderHelper.link(CookieParamValueFactoryProvider.InjectionResolver.class).to(InjectionResolver.class).in(Singleton.class).build());
        bind(BuilderHelper.link(WebTargetValueFactoryProvider.InjectionResolver.class).to(InjectionResolver.class).in(Singleton.class).build());
    }
}
