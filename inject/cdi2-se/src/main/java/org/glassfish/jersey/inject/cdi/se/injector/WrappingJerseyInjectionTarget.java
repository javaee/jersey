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

package org.glassfish.jersey.inject.cdi.se.injector;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;

/**
 * An implementation of {@link InjectionTarget} that just wraps the provided {@code InjectionTarget} because of additional
 * features in an injection phase.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class WrappingJerseyInjectionTarget<T> extends AbstractInjectionTarget<T> {

    private static InjectionTarget NOOP_INJECTION_TARGET = new NoOpInjectionTarget();

    private final Bean<T> bean;
    private final LazyValue<JerseyInstanceInjector<T>> injector;
    private final InjectionTarget<T> delegate;
    private Collection<InjectionResolver> resolvers;

    /**
     * Creates a new jersey injection target with delegate as a {@link NoOpInjectionTarget} that creates no operation that
     * means that only jersey injection is available as a additional feature.
     *
     * @param bean      bean as descriptor of the class which will be injected.
     * @param resolvers all resolvers that can provide a valued for Jersey-specific injection.
     */
    public WrappingJerseyInjectionTarget(Bean<T> bean, Collection<InjectionResolver> resolvers) {
        this(NOOP_INJECTION_TARGET, bean, resolvers);
    }

    /**
     * An implementation of {@link InjectionTarget} for classes that do not fulfill bean class requirements
     * (e.g. are abstract or non-static inner classes). Instances of these class can be injected using this implementation. If the
     * application attempts to {@link #produce(CreationalContext)} a new instance of the class, {@code CreationException} is
     * thrown.
     *
     * @param delegate  CDI specific injection target.
     * @param bean      bean as descriptor of the class which will be injected.
     * @param resolvers all resolvers that can provide a valued for Jersey-specific injection.
     */
    public WrappingJerseyInjectionTarget(InjectionTarget<T> delegate, Bean<T> bean, Collection<InjectionResolver> resolvers) {
        this.bean = bean;
        this.delegate = delegate;
        this.resolvers = resolvers;
        this.injector = Values.lazy((Value<JerseyInstanceInjector<T>>)
                () -> new JerseyInstanceInjector<>(bean, this.resolvers));
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
        /*
         * If an instance contains any fields which be injected by Jersey then Jersey attempts to inject them using annotations
         * retrieves from registered InjectionResolvers.
         */
        try {
            injector.get().inject(instance);
        } catch (Throwable cause) {
            throw new InjectionException(
                    "Exception occurred during Jersey/JAX-RS annotations processing in the class: " + bean.getBeanClass(), cause);
        }

        /*
         * The rest of the fields (annotated by @Inject) are injected using CDI.
         */
        super.inject(instance, ctx);
    }

    @Override
    InjectionTarget<T> delegate() {
        return delegate;
    }

    private static class NoOpInjectionTarget implements InjectionTarget<Object> {

        @Override
        public void inject(Object instance, CreationalContext<Object> ctx) {
        }

        @Override
        public void postConstruct(Object instance) {
        }

        @Override
        public void preDestroy(Object instance) {
        }

        @Override
        public Object produce(CreationalContext<Object> ctx) {
            return null;
        }

        @Override
        public void dispose(Object instance) {
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }
    }
}
