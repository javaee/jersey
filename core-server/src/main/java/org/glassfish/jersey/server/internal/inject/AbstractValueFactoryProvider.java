/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * A parameter value factory provider that provides parameter value factories
 * which are using {@link MultivaluedParameterExtractorProvider} to extract parameter
 * values from the supplied {@link javax.ws.rs.core.MultivaluedMap multivalued
 * parameter map}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractValueFactoryProvider implements ValueFactoryProvider {

    private final MultivaluedParameterExtractorProvider mpep;
    private final ServiceLocator locator;
    private final Set<Parameter.Source> compatibleSources;

    /**
     * Initialize the provider.
     *
     * @param mpep              multivalued map parameter extractor provider.
     * @param locator           HK2 service locator.
     * @param compatibleSources compatible parameter sources.
     */
    protected AbstractValueFactoryProvider(MultivaluedParameterExtractorProvider mpep,
                                           ServiceLocator locator,
                                           Parameter.Source... compatibleSources) {
        this.mpep = mpep;
        this.locator = locator;
        this.compatibleSources = new HashSet<Parameter.Source>(Arrays.asList(compatibleSources));
    }

    /**
     * Get a parameter extractor that ignores default value set on the parameter.
     *
     * @param parameter parameter supported by the returned extractor.
     * @return extractor supporting the parameter. The returned instance ignores
     *         any default values set on the parameter.
     * @see #get(org.glassfish.jersey.server.model.Parameter)
     */
    protected final MultivaluedParameterExtractor<?> getWithoutDefaultValue(Parameter parameter) {
        return mpep.getWithoutDefaultValue(parameter);
    }

    /**
     * Get a parameter extractor. Unlike the extractor returned by the
     * {@link #getWithoutDefaultValue(org.glassfish.jersey.server.model.Parameter)}
     * method, the extractor returned from this method will use the default value
     * set on the parameter, in case the parameter is not found in the supplied
     * {@link javax.ws.rs.core.MultivaluedMap multivalued parameter map}.
     *
     * @param parameter parameter supported by the returned extractor.
     * @return extractor supporting the parameter. The returned instance ignores
     *         any default values set on the parameter.
     */
    protected final MultivaluedParameterExtractor<?> get(Parameter parameter) {
        return mpep.get(parameter);
    }

    /**
     * Create a value factory for the parameter. May return {@code null} in case
     * the parameter is not supported by the value factory provider.
     *
     * @param parameter parameter requesting the value factory instance.
     * @return parameter value factory. Returns {@code null} if parameter is
     *         not supported.
     */
    protected abstract Factory<?> createValueFactory(Parameter parameter);

    /**
     * Get an injected value factory for the parameter. May return {@code null}
     * in case the parameter is not supported by the value factory provider.
     *
     * @param parameter parameter requesting the value factory instance.
     * @return injected parameter value factory. Returns {@code null} if parameter
     *         is not supported.
     */
    @Override
    public final Factory<?> getValueFactory(Parameter parameter) {
        if (!compatibleSources.contains(parameter.getSource())) {
            // not compatible
            return null;
        }

        final Factory<?> valueFactory = createValueFactory(parameter);
        if (valueFactory != null) {
            locator.inject(valueFactory);
        }
        return valueFactory;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }

    /**
     * Get the service locator.
     *
     * @return service locator.
     */
    protected final ServiceLocator getLocator() {
        return locator;
    }
}
