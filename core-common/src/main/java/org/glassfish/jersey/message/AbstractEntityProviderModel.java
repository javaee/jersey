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
package org.glassfish.jersey.message;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Abstract entity provider model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.16
 */
public abstract class AbstractEntityProviderModel<T> {

    private final T provider;
    private final List<MediaType> declaredTypes;
    private final boolean custom;
    private final Class<?> providedType;

    /**
     * Create new entity provider model.
     *
     * NOTE: The constructor is package private on purpose as we do not support extensions of this class from another package.
     *
     * @param provider      entity provider instance.
     * @param declaredTypes declared supported media types.
     * @param custom        custom flag; {@code true} is the provider is custom, {@code false} if the provider is one of the
     *                      default Jersey providers.
     * @param providerType  parameterized entity provider type (used to retrieve the provided Java type).
     */
    AbstractEntityProviderModel(final T provider,
                                final List<MediaType> declaredTypes,
                                final boolean custom,
                                final Class<T> providerType) {
        this.provider = provider;
        this.declaredTypes = declaredTypes;
        this.custom = custom;
        this.providedType = getProviderClassParam(provider, providerType);
    }

    /**
     * Get the modelled entity provider instance.
     *
     * @return entity provider instance.
     */
    public T provider() {
        return provider;
    }

    /**
     * Get types declared as supported (via {@code @Produces} or {@code @Consumes}) on the entity provider.
     *
     * @return declared supported types.
     */
    public List<MediaType> declaredTypes() {
        return declaredTypes;
    }

    /**
     * Get the {@code custom} flag value.
     *
     * @return {@code true} if the provider is a custom implementation, {@code false} if the provider is
     * one of the default providers supplied with Jersey.
     */
    public boolean isCustom() {
        return custom;
    }

    /**
     * Get the provided Java type.
     *
     * @return provided Java type.
     */
    public Class<?> providedType() {
        return providedType;
    }

    private static Class<?> getProviderClassParam(Object provider, Class<?> providerType) {
        final ReflectionHelper.DeclaringClassInterfacePair pair =
                ReflectionHelper.getClass(provider.getClass(), providerType);
        final Class[] classArgs = ReflectionHelper.getParameterizedClassArguments(pair);

        return classArgs != null ? classArgs[0] : Object.class;
    }
}
