/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.model;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.internal.LocalizationMessages;

import org.glassfish.hk2.api.PerLookup;

/**
 * Validator ensuring that {@link Invocable invocable} and {@link HandlerConstructor constructor} is correctly defined (for
 * example correctly annotated with scope annotation). This validator is stateful and therefore new instance must be created
 * for each resource model validation.
 *
 * @author Miroslav Fuksa
 *
 */
class InvocableValidator extends AbstractResourceModelVisitor {
    private static final Set<Class<?>> SCOPE_ANNOTATIONS = getScopeAnnotations();
    /**
     * Classes that have been checked already.
     */
    protected final Set<Class<?>> checkedClasses = new HashSet<Class<?>>();

    private static Set<Class<?>> getScopeAnnotations() {
        Set<Class<?>> scopeAnnotations = new HashSet<Class<?>>();
        scopeAnnotations.add(Singleton.class);
        scopeAnnotations.add(PerLookup.class);
        return scopeAnnotations;
    }

    @Override
    public void visitInvocable(final Invocable invocable) {
        // TODO: check invocable.
        Class resClass = invocable.getHandler().getHandlerClass();
        if (resClass != null && !checkedClasses.contains(resClass)) {
            checkedClasses.add(resClass);
            final boolean provider = Providers.isProvider(resClass);
            int counter = 0;
            for (Annotation annotation : resClass.getAnnotations()) {
                if (SCOPE_ANNOTATIONS.contains(annotation.annotationType())) {
                    counter++;
                }
            }
            if (counter == 0 && provider) {
                Errors.warning(resClass, LocalizationMessages.RESOURCE_IMPLEMENTS_PROVIDER(resClass,
                        Providers.getProviderContracts(resClass)));
            } else if (counter > 1) {
                Errors.fatal(resClass, LocalizationMessages.RESOURCE_MULTIPLE_SCOPE_ANNOTATIONS(resClass));
            }
        }


    }

    /**
     * Check if the resource class is declared to be a singleton.
     *
     * @param resourceClass resource class.
     * @return {@code true} if the resource class is a singleton, {@code false} otherwise.
     */
    public static boolean isSingleton(Class<?> resourceClass) {
        return resourceClass.isAnnotationPresent(Singleton.class)
                || (Providers.isProvider(resourceClass) && !resourceClass.isAnnotationPresent(PerLookup.class));
    }


    @Override
    public void visitResourceHandlerConstructor(final HandlerConstructor constructor) {
        Class<?> resClass = constructor.getConstructor().getDeclaringClass();
        boolean isSingleton = isSingleton(resClass);
        int paramCount = 0;
        for (Parameter p : constructor.getParameters()) {
            ResourceMethodValidator.validateParameter(p, constructor.getConstructor(), constructor.getConstructor()
                    .toGenericString(),
                    Integer.toString(++paramCount), isSingleton);
        }
    }


    // TODO: validate also method handler.


}
