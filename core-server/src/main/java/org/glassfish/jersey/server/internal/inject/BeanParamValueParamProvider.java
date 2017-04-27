/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.util.collection.Cache;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;

/**
 * Value factory provider for {@link BeanParam bean parameters}.
 *
 * @author Miroslav Fuksa
 */
@Singleton
final class BeanParamValueParamProvider extends AbstractValueParamProvider {

    private final InjectionManager injectionManager;

    private static final class BeanParamValueProvider implements Function<ContainerRequest, Object> {
        private final Parameter parameter;
        private final InjectionManager injectionManager;

        private final Cache<Class<?>, ForeignDescriptor> descriptorCache
                = new Cache<>(new Function<Class<?>, ForeignDescriptor>() {
                    @Override
                    public ForeignDescriptor apply(Class<?> key) {
                        // below we make sure HK2 behaves as if injection happens into a request scoped type
                        // this is to avoid having proxies injected (see JERSEY-2386)
                        // before touching the following statement, check BeanParamMemoryLeakTest first!
                        return injectionManager
                                .createForeignDescriptor(Bindings.serviceAsContract(key).in(RequestScoped.class));
                    }
                });

        private BeanParamValueProvider(InjectionManager injectionManager, Parameter parameter) {
            this.injectionManager = injectionManager;
            this.parameter = parameter;
        }

        @Override
        public Object apply(ContainerRequest request) {
            Class<?> rawType = parameter.getRawType();
            Object fromHk2 = injectionManager.getInstance(rawType);
            if (fromHk2 != null) { // the bean parameter type is already bound in HK2, let's just take it from there
                return fromHk2;
            }
            ForeignDescriptor foreignDescriptor = descriptorCache.apply(rawType);
            return injectionManager.getInstance(foreignDescriptor);
        }
    }

    /**
     * Creates new instance initialized from parameters injected by HK2.
     *
     * @param mpep            multivalued parameter extractor provider.
     */
    public BeanParamValueParamProvider(Provider<MultivaluedParameterExtractorProvider> mpep,
            InjectionManager injectionManager) {
        super(mpep, Parameter.Source.BEAN_PARAM);
        this.injectionManager = injectionManager;
    }

    @Override
    public Function<ContainerRequest, ?> createValueProvider(Parameter parameter) {
        return new BeanParamValueProvider(injectionManager, parameter);
    }
}
