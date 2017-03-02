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

import java.util.function.Function;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.ParamConverterProvider;

import javax.inject.Provider;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.Uri;
import org.glassfish.jersey.server.internal.process.AsyncContext;
import org.glassfish.jersey.server.spi.internal.ValueSupplierProvider;

/**
 * Injection binder providing support for JAX-RS and Jersey injection annotations.
 * Namely, standard injection support for the following set of JAX-RS and Jersey
 * annotations is provided by the binder:
 * <dl>
 * <p>
 * <dt>{@link javax.ws.rs.core.Context @Context}</dt>
 * <dd>
 * Generic support for the {@code @Context}-based injection is provided so that
 * the {@code @Context} annotation can be used interchangeably with e.g. standard
 * {@code @Inject} dependency injection annotation.
 * </dd>
 * <p>
 * <dt>{@link javax.ws.rs.CookieParam @CookieParam}</dt>
 * <dd>
 * Support for cookie parameter injection as defined by the JAX-RS specification.
 * </dd>
 * <p>
 * <dt>{@link javax.ws.rs.FormParam @FormParam}</dt>
 * <dd>
 * Support for form parameter injection as defined by the JAX-RS specification.
 * </dd>
 * <p>
 * <dt>{@link javax.ws.rs.HeaderParam @HeaderParam}</dt>
 * <dd>
 * Support for request header parameter injection as defined by the JAX-RS specification.
 * </dd>
 * <p>
 * <dt>{@link javax.ws.rs.MatrixParam @MatrixParam}</dt>
 * <dd>
 * Support for request URI matrix path parameter injection as defined by the JAX-RS specification.
 * </dd>
 * <p>
 * <dt>{@link javax.ws.rs.PathParam @PathParam}</dt>
 * <dd>
 * Support for request URI path parameter injection as defined by the JAX-RS specification.
 * </dd>
 * <p>
 * <dt>{@link javax.ws.rs.QueryParam @QueryParam}</dt>
 * <dd>
 * Support for request URI query parameter injection as defined by the JAX-RS specification.
 * </dd>
 * <p>
 * <dt>{@link javax.ws.rs.container.Suspended @Suspended}</dt>
 * <dd>
 * Support for {@link javax.ws.rs.container.AsyncResponse} injection as defined by the JAX-RS specification.
 * </dd>
 * <p>
 * <dt>{@link org.glassfish.jersey.server.Uri @Uri}</dt>
 * <dd>
 * Jersey-specific support for {@link javax.ws.rs.client.WebTarget} injection.
 * </dd>
 * <p>
 * </dl>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ParameterInjectionBinder extends AbstractBinder {

    private final InjectionManager injectionManager;

    public ParameterInjectionBinder(InjectionManager injectionManager) {
        this.injectionManager = injectionManager;
    }

    @Override
    public void configure() {
        // Param converter providers
        // TODO: Replace by non-di version
        bind(new ParamConverters.AggregatedProvider()).to(ParamConverterProvider.class);

        Provider<ContainerRequest> requestProvider = Injections.getProvider(injectionManager, ContainerRequest.class);
        Provider<AsyncContext> asyncContextProvider = Injections.getProvider(injectionManager, AsyncContext.class);
        Function<Class<? extends Configuration>, Configuration> clientConfigProvider = clientConfigClass -> Injections
                .getOrCreate(injectionManager, clientConfigClass);

        // Param Converters must be initialized Lazy and created at the time of the call on extractor
        LazyValue<ParamConverterFactory> lazyParamConverterFactory = Values.lazy((Value<ParamConverterFactory>)
                () -> new ParamConverterFactory(
                    Providers.getProviders(injectionManager, ParamConverterProvider.class),
                    Providers.getCustomProviders(injectionManager, ParamConverterProvider.class)));

        LazyValue<Configuration> lazyConfiguration = Values
                .lazy((Value<Configuration>) () -> injectionManager.getInstance(Configuration.class));

        MultivaluedParameterExtractorFactory paramExtractor = new MultivaluedParameterExtractorFactory(lazyParamConverterFactory);
        bind(paramExtractor).to(MultivaluedParameterExtractorProvider.class);

        // Parameter injection value providers
        AsyncResponseValueSupplierProvider asyncSupplier = new AsyncResponseValueSupplierProvider(asyncContextProvider);
        bindValueSupplier(asyncSupplier);
        CookieParamValueSupplierProvider cookieSupplier = new CookieParamValueSupplierProvider(paramExtractor, requestProvider);
        bindValueSupplier(cookieSupplier);
        EntityParamValueSupplierProvider entitySupplier = new EntityParamValueSupplierProvider(paramExtractor, requestProvider);
        bindValueSupplier(entitySupplier);
        FormParamValueSupplierProvider formSupplier = new FormParamValueSupplierProvider(paramExtractor, requestProvider);
        bindValueSupplier(formSupplier);
        HeaderParamValueSupplierProvider headerSupplier = new HeaderParamValueSupplierProvider(paramExtractor, requestProvider);
        bindValueSupplier(headerSupplier);
        MatrixParamValueSupplierProvider matrixSupplier = new MatrixParamValueSupplierProvider(paramExtractor, requestProvider);
        bindValueSupplier(matrixSupplier);
        PathParamValueSupplierProvider pathSupplier = new PathParamValueSupplierProvider(paramExtractor, requestProvider);
        bindValueSupplier(pathSupplier);
        QueryParamValueSupplierProvider querySupplier = new QueryParamValueSupplierProvider(paramExtractor, requestProvider);
        bindValueSupplier(querySupplier);
        WebTargetValueSupplierProvider webTargetSupplier = new WebTargetValueSupplierProvider(requestProvider, lazyConfiguration,
                clientConfigProvider);
        bindValueSupplier(webTargetSupplier);
        BeanParamValueSupplierProvider beanSupplier = new BeanParamValueSupplierProvider(paramExtractor, requestProvider,
                injectionManager);
        bindValueSupplier(beanSupplier);

        // Register InjectionResolvers with param providers
        // TODO: RENAME INJECTION RESOLVER
        bind(new ParamInjectionResolver<>(cookieSupplier, CookieParam.class));
        bind(new ParamInjectionResolver<>(formSupplier, FormParam.class));
        bind(new ParamInjectionResolver<>(headerSupplier, HeaderParam.class));
        bind(new ParamInjectionResolver<>(matrixSupplier, MatrixParam.class));
        bind(new ParamInjectionResolver<>(querySupplier, QueryParam.class));
        bind(new ParamInjectionResolver<>(pathSupplier, PathParam.class));
        bind(new ParamInjectionResolver<>(asyncSupplier, Suspended.class));
        bind(new ParamInjectionResolver<>(webTargetSupplier, Uri.class));
        bind(new ParamInjectionResolver<>(beanSupplier, BeanParam.class));

        // Delegated value supplier for Context InjectionResolver which is implemented directly in DI provider
        ContextInjectionResolver contextInjectionResolver = injectionManager.getInstance(ContextInjectionResolver.class);
        bind(new DelegatedInjectionValueSupplierProvider(contextInjectionResolver,
                injectionManager::createForeignDescriptor)).to(ValueSupplierProvider.class);
    }

    private <T extends ValueSupplierProvider> void bindValueSupplier(T supplier) {
        bind(supplier).to(ValueSupplierProvider.class);
    }
}
