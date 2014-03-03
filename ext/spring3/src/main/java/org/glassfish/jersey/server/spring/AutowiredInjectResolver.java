/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Singleton;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;

/**
 * HK2 injection resolver for Spring framework {@link Autowired} annotation injection.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Singleton
public class AutowiredInjectResolver implements InjectionResolver<Autowired> {
    private static final Logger LOGGER = Logger.getLogger(AutowiredInjectResolver.class.getName());
    private volatile ApplicationContext ctx;

    /**
     * Create a new instance.
     * @param ctx Spring application context.
     */
    public AutowiredInjectResolver(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        AnnotatedElement parent = injectee.getParent();
        String beanName = null;
        if(parent != null) {
            Qualifier an = parent.getAnnotation(Qualifier.class);
            if(an != null) {
                beanName = an.value();
            }
        }
        return getBeanFromSpringContext(beanName, injectee.getRequiredType());
    }

    private Object getBeanFromSpringContext(String beanName, Type beanType) {
        Class<?> bt = getClassFromType(beanType);
        if(beanName != null) {
            return ctx.getBean(beanName, bt);
        }
        Map<String, ?> beans = ctx.getBeansOfType(bt);
        if(beans == null || beans.size() < 1) {
            LOGGER.warning(LocalizationMessages.NO_BEANS_FOUND_FOR_TYPE(beanType));
            return null;
        }
        if(beanType instanceof ParameterizedType && List.class.isAssignableFrom((Class<?>) ((ParameterizedType) beanType).getRawType())) {
            return new ArrayList(beans.values());
        }
        return beans.values().iterator().next();
    }

    private Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;

            return (Class<?>) pt.getActualTypeArguments()[0];
        }
        return null;
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