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

package org.glassfish.jersey.inject.cdi.se.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.PerLookup;
import org.glassfish.jersey.internal.inject.PerThread;

import org.jboss.weld.environment.se.contexts.ThreadScoped;

/**
 * Jersey-specific abstract class which implements {@link Bean} interface. Class particularly contains default implementations
 * of {@link Bean} interface.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public abstract class JerseyBean<T> implements Bean<T>, PassivationCapable {

    static final Set<Annotation> DEFAULT_QUALIFIERS;

    static {
        DEFAULT_QUALIFIERS = new HashSet<>();
        DEFAULT_QUALIFIERS.add(new AnnotationLiteral<Default>() {});
        DEFAULT_QUALIFIERS.add(new AnnotationLiteral<Any>() {});
    }

    private final Binding<T, ?> binding;

    /**
     * JerseyBean constructor with {@link Binding} which represents {@link javax.enterprise.context.spi.Contextual} part of the
     * bean.
     *
     * @param binding information about the bean.
     */
    JerseyBean(Binding<T, ?> binding) {
        this.binding = binding;
    }

    /**
     * Transforms Jersey scopes/annotations to HK2 equivalents.
     *
     * @param scope Jersey scope/annotation.
     * @return HK2 equivalent scope/annotation.
     */
    protected static Class<? extends Annotation> transformScope(Class<? extends Annotation> scope) {
        if (scope == PerLookup.class) {
            return Dependent.class;
        } else if (scope == PerThread.class) {
            return ThreadScoped.class;
        } else if (scope == org.glassfish.jersey.process.internal.RequestScoped.class) {
            return RequestScoped.class;
        }
        return scope;
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> contracts = new HashSet<>();
        contracts.addAll(binding.getContracts());

        // Merge aliases with the main bean
        if (!binding.getAliases().isEmpty()) {
            binding.getAliases().forEach(alias -> contracts.add(alias.getContract()));
        }
        contracts.add(Object.class);
        return contracts;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.addAll(DEFAULT_QUALIFIERS);
        if (binding.getQualifiers() != null) {
            qualifiers.addAll(binding.getQualifiers());
        }

        // Merge aliases with the main bean
        if (!binding.getAliases().isEmpty()) {
            binding.getAliases().forEach(alias -> qualifiers.addAll(alias.getQualifiers()));
        }
        return qualifiers;
    }

    @Override
    public String getName() {
        return binding.getName();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Singleton.class;
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
    public boolean isNullable() {
        return false;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    public int getRank() {
        if (binding.getRank() != null) {
            return binding.getRank();
        }

        Class<T> type = binding.getImplementationType();
        if (type != null) {
            Priority priority = type.getAnnotation(Priority.class);
            if (priority != null) {
                return priority.value();
            }
        }

        return 1;
    }

    @Override
    public Class<?> getBeanClass() {
        return Object.class;
    }

    @Override
    public String getId() {
        return getBeanClass().getTypeName() + "#jersey";
    }
}
