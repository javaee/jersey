/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;
import javax.inject.Qualifier;

import javax.ws.rs.core.Context;

import org.glassfish.hk2.ContractLocator;
import org.glassfish.hk2.Provider;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Inhabitant;

import org.jvnet.tiger_types.Types;

import com.sun.hk2.component.InjectionResolver;

/**
 * Injection resolver for {@link Context @Context} injection annotation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ContextInjectionResolver extends InjectionResolver<Context> {

    public static final class Module extends AbstractModule {

        @Override
        protected void configure() {
            // @Context
            bind(InjectionResolver.class).to(ContextInjectionResolver.class).in(Singleton.class);
        }
    }
    //
    @Inject
    Services services;

    public ContextInjectionResolver() {
        super(Context.class);
    }

    @Override
    public boolean isOptional(AnnotatedElement annotated, Context annotation) {
        return true; // seems like all @Context are optional so far...
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Object component, Inhabitant<?> onBehalfOf, AnnotatedElement annotated, Type genericType, Class<V> type) throws ComponentException {
        final boolean isHk2Factory = Types.isSubClassOf(type, org.glassfish.hk2.Factory.class);

        String targetTypeName;
        if (isHk2Factory) {
            targetTypeName = exploreType(Types.getTypeArgument(genericType, 0));
        } else {
            targetTypeName = exploreType(genericType);
        }

        ContractLocator<?> locator = services.forContract(targetTypeName);
        for (Annotation a : annotated.getAnnotations()) {
            final Class<? extends Annotation> ac = a.annotationType();
            if (Named.class.isAssignableFrom(ac)) {
                locator = locator.named(Named.class.cast(a).value());
            } else if (ac.isAnnotationPresent(Qualifier.class)) {
                locator = locator.annotatedWith(ac);
            }
            // todo: what to do about scopes?
        }

        Provider<?> provider = locator.getProvider();
        if (isHk2Factory) {
            return (V) ((provider == null) ? Providers.asFactory(services.byType(targetTypeName).getProvider()) : Providers.asFactory(provider));
        } else {
            return (V) ((provider == null) ? services.byType(targetTypeName).get() : provider.get());
        }
    }

    // TODO: Replace with HK2 API call once such API is available
    private static void exploreType(Type type, StringBuilder builder) {
        if (type instanceof ParameterizedType) {
            builder.append(TypeLiteral.getRawType(type).getName());

            // we ignore wildcard types.
            Collection<Type> types = Arrays.asList(((ParameterizedType) type).getActualTypeArguments());
            Iterator<Type> typesEnum = types.iterator();
            List<Type> nonWildcards = new ArrayList<Type>();
            while (typesEnum.hasNext()) {
                Type genericType = typesEnum.next();
                if (!(genericType instanceof WildcardType)) {
                    nonWildcards.add(genericType);
                }
            }
            if (!nonWildcards.isEmpty()) {
                builder.append("<");
                Iterator<Type> typesItr = nonWildcards.iterator();
                while (typesItr.hasNext()) {
                    exploreType(typesItr.next(), builder);
                    if (typesItr.hasNext()) {
                        builder.append(",");
                    }
                }
                builder.append(">");
            }
        } else {
            builder.append(TypeLiteral.getRawType(type).getName());
        }
    }

    private static String exploreType(Type type) {
        StringBuilder builder = new StringBuilder();
        exploreType(type, builder);
        return builder.toString();
    }
}
