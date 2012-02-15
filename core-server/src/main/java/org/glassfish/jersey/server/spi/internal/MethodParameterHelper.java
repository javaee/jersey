/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spi.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Parameterized;

/**
 *
 * @author Marek Potociar
 */
public class MethodParameterHelper {

    public static Object[] getParameterValues(List<Factory<?>> valueProviders) {
        final Object[] params = new Object[valueProviders.size()];
        try {
            int index = 0;
            for (Factory<?> valueProvider : valueProviders) {
                params[index++] = valueProvider.get();
            }
            return params;
        } catch (WebApplicationException e) {
            throw e;
        } catch (ProcessingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ProcessingException("Exception obtaining parameters", e);
        }
    }

    public static List<Factory<?>> createValueProviders(final Services services, final Parameterized resourceMethod) {
        if ((null == resourceMethod.getParameters()) || (0 == resourceMethod.getParameters().size())) {
            return Collections.emptyList();
        }

        Set<ValueFactoryProvider> valueFactoryProviders = Providers.getProviders(services, ValueFactoryProvider.class);

        boolean entityParamFound = false;
        final List<Factory<?>> providers = new ArrayList<Factory<?>>(resourceMethod.getParameters().size());
        for (final Parameter parameter : resourceMethod.getParameters()) {
            entityParamFound = entityParamFound || Parameter.Source.ENTITY == parameter.getSource();
            providers.add(getValueFactory(valueFactoryProviders, parameter));
        }

        if (!entityParamFound && Collections.frequency(providers, null) == 1) {
            // Try to find entity if there is one unresolved parameter and the annotations are unknown
            final int entityParamIndex = providers.lastIndexOf(null);
            final Parameter parameter = resourceMethod.getParameters().get(entityParamIndex);
            if (Parameter.Source.UNKNOWN == parameter.getSource() && !parameter.isQualified()) {
                providers.set(entityParamIndex, getValueFactory(
                        valueFactoryProviders,
                        new SourceOverrideParameter(parameter, Parameter.Source.ENTITY)));
            }
        }

        return providers;
    }

    private static Factory<?> getValueFactory(Set<ValueFactoryProvider> valueFactoryProviders, final Parameter parameter) {
        Factory<?> valueFactory = null;
        final Iterator<ValueFactoryProvider> vfpIterator = valueFactoryProviders.iterator();
        while (valueFactory == null && vfpIterator.hasNext()) {
            valueFactory = vfpIterator.next().getValueFactory(parameter);
        }
        return valueFactory;
    }

    private static class SourceOverrideParameter extends Parameter {

        public SourceOverrideParameter(final Parameter p, final Parameter.Source overrideSource) {
            super(
                    p.getAnnotations(),
                    p.getAnnotation(),
                    overrideSource,
                    p.getSourceName(),
                    p.getParameterType(),
                    p.getParameterClass(),
                    p.isEncoded(),
                    p.getDefaultValue());
        }
    }

    /**
     * Prevents instantiation.
     */
    private MethodParameterHelper() {
    }
}
