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

import java.lang.reflect.ParameterizedType;
import java.util.function.Function;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjecteeImpl;
import org.glassfish.jersey.internal.util.collection.Cache;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Parameter.Source;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

/**
 * Value factory provider that delegates the injection target lookup to the underlying injection provider.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Singleton
class DelegatedInjectionValueParamProvider implements ValueParamProvider {

    private final LazyValue<ContextInjectionResolver> resolver;

    private final Function<Binding, ForeignDescriptor> foreignDescriptorFactory;

    /**
     * Injection constructor.
     *
     * @param resolver                 context injection resolver.
     * @param foreignDescriptorFactory function that is able to create a new foreign descriptor.
     */
    public DelegatedInjectionValueParamProvider(LazyValue<ContextInjectionResolver> resolver,
            Function<Binding, ForeignDescriptor> foreignDescriptorFactory) {
        this.resolver = resolver;
        this.foreignDescriptorFactory = foreignDescriptorFactory;
    }

    @Override
    public Function<ContainerRequest, ?> getValueProvider(final Parameter parameter) {
        Source paramSource = parameter.getSource();
        if (paramSource == Parameter.Source.CONTEXT) {
            return containerRequest -> resolver.get().resolve(getInjectee(parameter));
        }
        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.LOW;
    }

    /**
     * Creates a new object {@link Injectee} corresponding to the injecting point. The injectee contains basic information
     * about the injection point types and {@link ForeignDescriptor} of the underlying DI provider to make delegated injection
     * resolver as simple as possible.
     *
     * @param parameter jersey-like parameter corresponding to one resource-method's parameter.
     * @return injectee instance as a source of the information about the injecting point.
     */
    private Injectee getInjectee(Parameter parameter) {
        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setRequiredType(parameter.getType());
        injectee.setInjecteeClass(parameter.getRawType());
        ForeignDescriptor proxyDescriptor = descriptorCache.apply(parameter);
        if (proxyDescriptor != null) {
            injectee.setInjecteeDescriptor(proxyDescriptor);
        }
        return injectee;
    }

    /**
     * We do not want to create a new descriptor instance for every and each method invocation.
     * If the underlying DI descriptor {@link ForeignDescriptor} is already created for the given {@link Parameter} then
     * used the already created descriptor.
     */
    private final Cache<Parameter, ForeignDescriptor> descriptorCache =
            new Cache<>(parameter -> {
                Class<?> rawType = parameter.getRawType();
                if (rawType.isInterface() && !(parameter.getType() instanceof ParameterizedType)) {
                    return createDescriptor(rawType);
                }
                return null;
            });

    /**
     * Method is able to create form incoming class and {@link Binding jersey descriptor} a {@link ForeignDescriptor} which is
     * provided by underlying DI provider.
     *
     * @param clazz class from which jersey-like descriptor is created.
     * @return foreign descriptor of the underlying DI provider.
     */
    private ForeignDescriptor createDescriptor(Class<?> clazz) {
        return foreignDescriptorFactory.apply(Bindings.serviceAsContract(clazz).in(RequestScoped.class));
    }
}
