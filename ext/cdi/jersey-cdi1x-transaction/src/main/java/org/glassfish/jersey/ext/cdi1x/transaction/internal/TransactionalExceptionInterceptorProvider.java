/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.ext.cdi1x.transaction.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ExceptionMapper;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Qualifier;
import javax.interceptor.Interceptor;
import javax.transaction.TransactionalException;

import org.glassfish.jersey.ext.cdi1x.internal.CdiUtil;
import org.glassfish.jersey.ext.cdi1x.internal.GenericCdiBeanHk2Factory;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.spi.ComponentProvider;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Jersey CDI extension that provides means to retain {@link WebApplicationException}
 * thrown from JAX-RS components implemented as CDI transactional beans.
 * This is to avoid the {@link WebApplicationException} from being masked with
 * {@link TransactionalException}. Jersey will try to restore the original
 * JAX-RS exception using {@link TransactionalExceptionMapper}.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Priority(value = Interceptor.Priority.PLATFORM_BEFORE + 199)
public class TransactionalExceptionInterceptorProvider implements ComponentProvider, Extension {

    private ServiceLocator locator;
    private BeanManager beanManager;

    @Qualifier
    @Retention(RUNTIME)
    @Target({METHOD, FIELD, PARAMETER, TYPE})
    public static @interface WaeQualifier {
    }

    @Override
    public void initialize(final ServiceLocator locator) {
        this.locator = locator;
        this.beanManager = CdiUtil.getBeanManager();
    }

    @Override
    public boolean bind(final Class<?> component, final Set<Class<?>> providerContracts) {
        return false;
    }

    @Override
    public void done() {
        if (beanManager != null) {
            bindWaeRestoringExceptionMapper();
        }
    }

    private void bindWaeRestoringExceptionMapper() {
        final DynamicConfiguration dc = Injections.getConfiguration(locator);
        final ServiceBindingBuilder bindingBuilder = Injections.newFactoryBinder(
                new GenericCdiBeanHk2Factory(TransactionalExceptionMapper.class, locator, beanManager, true));
        bindingBuilder.to(ExceptionMapper.class);
        Injections.addBinding(bindingBuilder, dc);
        dc.commit();
    }

    @SuppressWarnings("unused")
    private void afterTypeDiscovery(@Observes final AfterTypeDiscovery afterTypeDiscovery) {
        final List<Class<?>> interceptors = afterTypeDiscovery.getInterceptors();
        interceptors.add(WebAppExceptionInterceptor.class);
    }

    @SuppressWarnings("unused")
    private void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(WebAppExceptionHolder.class));
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(WebAppExceptionInterceptor.class));
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(TransactionalExceptionMapper.class));
    }
}
