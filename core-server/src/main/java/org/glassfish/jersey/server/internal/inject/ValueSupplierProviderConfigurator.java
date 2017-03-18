/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

import javax.inject.Provider;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ServerBootstrapBag;
import org.glassfish.jersey.server.Uri;
import org.glassfish.jersey.server.internal.process.AsyncContext;
import org.glassfish.jersey.server.spi.internal.ValueSupplierProvider;

/**
 * Configurator which initializes and register {@link ValueSupplierProvider} instances into {@link InjectionManager} and
 * {@link BootstrapBag}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class ValueSupplierProviderConfigurator implements BootstrapConfigurator {

    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        ServerBootstrapBag serverBag = (ServerBootstrapBag) bootstrapBag;

        Provider<ContainerRequest> requestProvider = () -> injectionManager.getInstance(ContainerRequest.class);
        Provider<AsyncContext> asyncContextProvider = () -> injectionManager.getInstance(AsyncContext.class);

        Function<Class<? extends Configuration>, Configuration> clientConfigProvider =
                clientConfigClass -> Injections.getOrCreate(injectionManager, clientConfigClass);

        LazyValue<Configuration> lazyConfiguration =
                Values.lazy((Value<Configuration>) () -> injectionManager.getInstance(Configuration.class));

        LazyValue<ContextInjectionResolver> lazyContextResolver =
                Values.lazy((Value<ContextInjectionResolver>) () -> injectionManager.getInstance(ContextInjectionResolver.class));

        Provider<MultivaluedParameterExtractorProvider> paramExtractor = serverBag::getMultivaluedParameterExtractorProvider;

        // Parameter injection value providers
        Collection<ValueSupplierProvider> suppliers = new ArrayList<>();

        AsyncResponseValueSupplierProvider asyncProvider = new AsyncResponseValueSupplierProvider(asyncContextProvider);
        suppliers.add(asyncProvider);

        CookieParamValueSupplierProvider cookieProvider = new CookieParamValueSupplierProvider(paramExtractor, requestProvider);
        suppliers.add(cookieProvider);

        EntityParamValueSupplierProvider entityProvider = new EntityParamValueSupplierProvider(paramExtractor, requestProvider);
        suppliers.add(entityProvider);

        FormParamValueSupplierProvider formProvider = new FormParamValueSupplierProvider(paramExtractor, requestProvider);
        suppliers.add(formProvider);

        HeaderParamValueSupplierProvider headerProvider = new HeaderParamValueSupplierProvider(paramExtractor, requestProvider);
        suppliers.add(headerProvider);

        MatrixParamValueSupplierProvider matrixProvider = new MatrixParamValueSupplierProvider(paramExtractor, requestProvider);
        suppliers.add(matrixProvider);

        PathParamValueSupplierProvider pathProvider = new PathParamValueSupplierProvider(paramExtractor, requestProvider);
        suppliers.add(pathProvider);

        QueryParamValueSupplierProvider queryProvider = new QueryParamValueSupplierProvider(paramExtractor, requestProvider);
        suppliers.add(queryProvider);

        BeanParamValueSupplierProvider beanProvider =
                new BeanParamValueSupplierProvider(paramExtractor, requestProvider, injectionManager);
        suppliers.add(beanProvider);

        WebTargetValueSupplierProvider webTargetProvider =
                new WebTargetValueSupplierProvider(requestProvider, lazyConfiguration, clientConfigProvider);
        suppliers.add(webTargetProvider);

        DelegatedInjectionValueSupplierProvider contextProvider =
                new DelegatedInjectionValueSupplierProvider(lazyContextResolver, injectionManager::createForeignDescriptor);
        suppliers.add(contextProvider);

        serverBag.setValueSupplierProviders(Collections.unmodifiableCollection(suppliers));

        // Needs to be in InjectionManager because of CdiComponentProvider
        injectionManager.register(Bindings.service(asyncProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(cookieProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(formProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(headerProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(matrixProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(pathProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(queryProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(webTargetProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(beanProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(entityProvider).to(ValueSupplierProvider.class));
        injectionManager.register(Bindings.service(contextProvider).to(ValueSupplierProvider.class));

        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(asyncProvider, Suspended.class)));
        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(cookieProvider, CookieParam.class)));
        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(formProvider, FormParam.class)));
        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(headerProvider, HeaderParam.class)));
        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(matrixProvider, MatrixParam.class)));
        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(pathProvider, PathParam.class)));
        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(queryProvider, QueryParam.class)));
        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(webTargetProvider, Uri.class)));
        injectionManager.register(Bindings.injectionResolver(new ParamInjectionResolver<>(beanProvider, BeanParam.class)));
    }

    @Override
    public void postInit(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        // Add the ValueSupplierProviders which has been added to ResourceConfig/Feature
        List<ValueSupplierProvider> addedInstances = injectionManager.getAllInstances(ValueSupplierProvider.class);
        if (!addedInstances.isEmpty()) {
            ServerBootstrapBag serverBag = (ServerBootstrapBag) bootstrapBag;
            addedInstances.addAll(serverBag.getValueSupplierProviders());
            serverBag.setValueSupplierProviders(Collections.unmodifiableCollection(addedInstances));
        }
    }
}
