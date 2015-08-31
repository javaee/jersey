/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spring;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;

/**
 * HK2 injection resolver for Spring framework {@link Autowired} annotation injection.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 * @author Vetle Leinonen-Roeim (vetle at roeim.net)
 */
@Singleton
public class AutowiredInjectResolver implements InjectionResolver<Autowired> {

    private static final Logger LOGGER = Logger.getLogger(AutowiredInjectResolver.class.getName());

    private volatile ApplicationContext ctx;

    /**
     * Create a new instance.
     *
     * @param ctx Spring application context.
     */
    public AutowiredInjectResolver(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        AnnotatedElement parent = injectee.getParent();
        String beanName = null;
        if (parent != null) {
            Qualifier an = parent.getAnnotation(Qualifier.class);
            if (an != null) {
                beanName = an.value();
            }
        }
        boolean required = parent != null ? parent.getAnnotation(Autowired.class).required() : false;
        return getBeanFromSpringContext(beanName, injectee, required);
    }

    private Object getBeanFromSpringContext(String beanName, Injectee injectee, final boolean required) {
        try {
            DependencyDescriptor dependencyDescriptor = createSpringDependencyDescriptor(injectee);
            Set<String> autowiredBeanNames = new HashSet<>(1);
            autowiredBeanNames.add(beanName);
            return ctx.getAutowireCapableBeanFactory().resolveDependency(dependencyDescriptor, null,
                    autowiredBeanNames, null);
        } catch (NoSuchBeanDefinitionException e) {
            if (required) {
                LOGGER.warning(e.getMessage());
                throw e;
            }
            return null;
        }
    }

    private DependencyDescriptor createSpringDependencyDescriptor(final Injectee injectee) {
        AnnotatedElement annotatedElement = injectee.getParent();

        if (annotatedElement.getClass().isAssignableFrom(Field.class)) {
            return new DependencyDescriptor((Field) annotatedElement, !injectee.isOptional());
        } else if (annotatedElement.getClass().isAssignableFrom(Method.class)) {
            return new DependencyDescriptor(
                    new MethodParameter((Method) annotatedElement, injectee.getPosition()), !injectee.isOptional());
        } else {
            return new DependencyDescriptor(
                    new MethodParameter((Constructor) annotatedElement, injectee.getPosition()), !injectee.isOptional());
        }
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
