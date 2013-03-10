/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.model.internal;

import java.util.Map;

import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Immutable runtime configuration.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ImmutableCommonConfig extends CommonConfig {

    private final String errorMessage;

    /**
     * Create new immutable copy of the original runtime configuration
     * with a custom modification error message.
     *
     * @param config original runtime configuration.
     * @param modificationErrorMessage custom modification error message.
     */
    public ImmutableCommonConfig(final CommonConfig config, final String modificationErrorMessage) {
        super(config);

        this.errorMessage = modificationErrorMessage;
    }

    /**
     * Create new immutable copy of the original runtime configuration.
     *
     * @param config original runtime configuration.
     */
    public ImmutableCommonConfig(final CommonConfig config) {
        this(config, LocalizationMessages.CONFIGURATION_NOT_MODIFIABLE());
    }

    @Override
    public ImmutableCommonConfig property(String name, Object value) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public ImmutableCommonConfig setProperties(final Map<String, ?> properties) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public ImmutableCommonConfig register(final Class<?> componentClass) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public ImmutableCommonConfig register(final Class<?> componentClass, final int bindingPriority) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public ImmutableCommonConfig register(final Class<?> componentClass, final Class<?>... contracts) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public CommonConfig register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public ImmutableCommonConfig register(final Object component) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public ImmutableCommonConfig register(final Object component, final int bindingPriority) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public ImmutableCommonConfig register(final Object component, final Class<?>... contracts) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public CommonConfig register(Object component, Map<Class<?>, Integer> contracts) {
        throw new IllegalStateException(errorMessage);
    }

    @Override
    public CommonConfig loadFrom(Configuration config) {
        throw new IllegalStateException(errorMessage);
    }
}
