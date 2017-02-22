/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.ext.ParamConverterProvider;

import javax.inject.Singleton;

import org.glassfish.jersey.server.Uri;
import org.glassfish.jersey.server.spi.internal.ValueSupplierProvider;

import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Injection binder providing support for JAX-RS and Jersey injection annotations.
 * Namely, standard injection support for the following set of JAX-RS and Jersey
 * annotations is provided by the binder:
 * <dl>
 *
 * <dt>{@link javax.ws.rs.core.Context @Context}</dt>
 * <dd>
 * Generic support for the {@code @Context}-based injection is provided so that
 * the {@code @Context} annotation can be used interchangeably with e.g. standard
 * {@code @Inject} dependency injection annotation.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.CookieParam @CookieParam}</dt>
 * <dd>
 * Support for cookie parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.FormParam @FormParam}</dt>
 * <dd>
 * Support for form parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.HeaderParam @HeaderParam}</dt>
 * <dd>
 * Support for request header parameter injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link javax.ws.rs.MatrixParam @MatrixParam}</dt>
 * <dd>
 * Support for request URI matrix path parameter injection as defined by the JAX-RS specification.
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
 * <dt>{@link javax.ws.rs.container.Suspended @Suspended}</dt>
 * <dd>
 * Support for {@link javax.ws.rs.container.AsyncResponse} injection as defined by the JAX-RS specification.
 * </dd>
 *
 * <dt>{@link org.glassfish.jersey.server.Uri @Uri}</dt>
 * <dd>
 * Jersey-specific support for {@link javax.ws.rs.client.WebTarget} injection.
 * </dd>
 *
 * </dl>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ParameterInjectionBinder extends AbstractBinder {

    @Override
    public void configure() {
        // Param converter providers
        bind(ParamConverters.AggregatedProvider.class).to(ParamConverterProvider.class).in(Singleton.class);
        bindAsContract(ParamConverterFactory.class).in(Singleton.class);

        // Parameter injection value extractor providers
        bind(MultivaluedParameterExtractorFactory.class).to(MultivaluedParameterExtractorProvider.class).in(Singleton.class);

        // Parameter injection value providers
        bind(AsyncResponseValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(CookieParamValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(DelegatedInjectionValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(EntityParamValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(FormParamValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(HeaderParamValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(MatrixParamValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(PathParamValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(QueryParamValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(WebTargetValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);
        bind(BeanParamValueSupplierProvider.class).to(ValueSupplierProvider.class).in(Singleton.class);

        // Injection resolvers
        bind(CookieParamValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<CookieParam>>() {
        }).in(Singleton.class);
        bind(FormParamValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<FormParam>>() {
        }).in(Singleton.class);
        bind(HeaderParamValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<HeaderParam>>() {
        }).in(Singleton.class);
        bind(MatrixParamValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<MatrixParam>>() {
        }).in(Singleton.class);
        bind(QueryParamValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<QueryParam>>() {
        }).in(Singleton.class);
        bind(PathParamValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<PathParam>>() {
        }).in(Singleton.class);
        bind(AsyncResponseValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<Suspended>>() {
        }).in(Singleton.class);
        bind(WebTargetValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<Uri>>() {
        }).in(Singleton.class);
        bind(BeanParamValueSupplierProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<BeanParam>>() {
        }).in(Singleton.class);

    }
}
