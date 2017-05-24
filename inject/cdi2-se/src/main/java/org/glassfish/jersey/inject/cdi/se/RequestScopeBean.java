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

package org.glassfish.jersey.inject.cdi.se;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Singleton;

import org.glassfish.jersey.process.internal.RequestScope;

/**
 * CDI Class Bean represents {@link CdiRequestScope}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class RequestScopeBean implements Bean<CdiRequestScope> {

    private final InjectionTarget<CdiRequestScope> injectionTarget;

    /**
     * Creates a new Jersey-specific {@link javax.enterprise.inject.spi.Bean} instance.
     */
    public RequestScopeBean(BeanManager beanManager) {
        AnnotatedType<CdiRequestScope> annotatedType = beanManager.createAnnotatedType(CdiRequestScope.class);
        this.injectionTarget = beanManager.createInjectionTarget(annotatedType);
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CdiRequestScope create(CreationalContext<CdiRequestScope> context) {
        CdiRequestScope instance = injectionTarget.produce(context);
        injectionTarget.inject(instance, context);
        injectionTarget.postConstruct(instance);
        return instance;
    }

    @Override
    public void destroy(CdiRequestScope instance, CreationalContext<CdiRequestScope> context) {
        injectionTarget.preDestroy(instance);
        injectionTarget.dispose(instance);
        context.release();
    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(RequestScope.class);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(new AnnotationLiteral<Default>() {});
        qualifiers.add(new AnnotationLiteral<Any>() {});
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Singleton.class;
    }

    @Override
    public String getName() {
        return CdiRequestScope.class.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Class<?> getBeanClass() {
        return CdiRequestScope.class;
    }
}
