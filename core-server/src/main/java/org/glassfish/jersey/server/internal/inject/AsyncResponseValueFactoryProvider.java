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

package org.glassfish.jersey.server.internal.inject;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.server.internal.process.ProcessingContext;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

import org.glassfish.hk2.api.Factory;

/**
 * Value factory provider supporting the {@link javax.ws.rs.container.Suspended} injection annotation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class AsyncResponseValueFactoryProvider implements ValueFactoryProvider {

    private final Provider<ProcessingContext> processingContextProvider;

    /**
     * {@link javax.ws.rs.container.Suspended} injection resolver.
     */
    static final class InjectionResolver extends ParamInjectionResolver<Suspended> {

        /**
         * Create new injection resolver.
         */
        public InjectionResolver() {
            super(AsyncResponseValueFactoryProvider.class);
        }
    }

    /**
     * Initialize the provider.
     *
     * @param processingContextProvider processing context provider.
     */
    @Inject
    public AsyncResponseValueFactoryProvider(Provider<ProcessingContext> processingContextProvider) {
        this.processingContextProvider = processingContextProvider;
    }

    @Override
    public Factory<?> getValueFactory(final Parameter parameter) {
        if (parameter.getSource() != Parameter.Source.SUSPENDED) {
            return null;
        }
        if (!AsyncResponse.class.isAssignableFrom(parameter.getRawType())) {
            return null;
        }

        return new Factory<AsyncResponse>() {
            @Override
            public AsyncResponse provide() {
                final ProcessingContext processingContext = processingContextProvider.get();
                processingContext.suspend();
                return processingContext;
            }

            @Override
            public void dispose(AsyncResponse instance) {
                // not used
            }
        };
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }
}
