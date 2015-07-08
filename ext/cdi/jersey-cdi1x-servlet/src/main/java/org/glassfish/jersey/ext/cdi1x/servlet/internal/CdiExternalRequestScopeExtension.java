/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.ext.cdi1x.servlet.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.util.AnnotationLiteral;

/**
 * CDI extension to register {@link CdiExternalRequestScope}.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class CdiExternalRequestScopeExtension implements Extension {

    public static final AnnotationLiteral<Default> DEFAULT_ANNOTATION_LITERAL = new AnnotationLiteral<Default>() {};
    public static final AnnotationLiteral<Any> ANY_ANNOTATION_LITERAL = new AnnotationLiteral<Any>() {};

    private AnnotatedType<CdiExternalRequestScope> requestScopeType;

    /**
     * Register our external request scope.
     *
     * @param beforeBeanDiscoveryEvent CDI bootstrap event.
     * @param beanManager current bean manager.
     */
    private void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscoveryEvent, final BeanManager beanManager) {
        requestScopeType = beanManager.createAnnotatedType(CdiExternalRequestScope.class);
        beforeBeanDiscoveryEvent.addAnnotatedType(requestScopeType);
    }

    /**
     * Register a CDI bean for the external scope.
     *
     * @param afterBeanDiscovery CDI bootstrap event.
     * @param beanManager current bean manager
     */
    private void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

        // we need the injection target so that CDI could instantiate the original interceptor for us
        final InjectionTarget<CdiExternalRequestScope> interceptorTarget = beanManager.createInjectionTarget(requestScopeType);


        afterBeanDiscovery.addBean(new Bean<CdiExternalRequestScope>() {

            @Override
            public Class<?> getBeanClass() {
                return CdiExternalRequestScope.class;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return interceptorTarget.getInjectionPoints();
            }

            @Override
            public String getName() {
                return "CdiExternalRequestScope";
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return new HashSet<Annotation>() {{
                    add(DEFAULT_ANNOTATION_LITERAL);
                    add(ANY_ANNOTATION_LITERAL);
                }};
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return Dependent.class;
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return Collections.emptySet();
            }

            @Override
            public Set<Type> getTypes() {
                return new HashSet<Type>() {{
                    add(CdiExternalRequestScope.class);
                    add(Object.class);
                }};
            }

            @Override
            public boolean isAlternative() {
                return false;
            }

            @Override
            public boolean isNullable() {
                return false;
            }

            @Override
            public CdiExternalRequestScope create(CreationalContext<CdiExternalRequestScope> ctx) {

                final CdiExternalRequestScope result = interceptorTarget.produce(ctx);
                interceptorTarget.inject(result, ctx);
                interceptorTarget.postConstruct(result);
                return result;
            }


            @Override
            public void destroy(CdiExternalRequestScope instance,
                                CreationalContext<CdiExternalRequestScope> ctx) {

                interceptorTarget.preDestroy(instance);
                interceptorTarget.dispose(instance);
                ctx.release();
            }
        });
    }
}