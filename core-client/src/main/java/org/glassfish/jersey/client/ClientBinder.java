/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client;

import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.ext.MessageBodyReader;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.process.internal.RequestScoped;

/**
 * Registers all binders necessary for {@link Client} runtime.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
class ClientBinder extends AbstractBinder {

    private final Map<String, Object> clientRuntimeProperties;

    private static class RequestContextInjectionFactory extends ReferencingFactory<ClientRequest> {

        @Inject
        public RequestContextInjectionFactory(Provider<Ref<ClientRequest>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class PropertiesDelegateFactory implements Supplier<PropertiesDelegate> {

        private final Provider<ClientRequest> requestProvider;

        @Inject
        private PropertiesDelegateFactory(Provider<ClientRequest> requestProvider) {
            this.requestProvider = requestProvider;
        }

        @Override
        public PropertiesDelegate get() {
            return requestProvider.get().getPropertiesDelegate();
        }
    }

    /**
     * Create new client binder for a new client runtime instance.
     *
     * @param clientRuntimeProperties map of client runtime properties.
     */
    ClientBinder(Map<String, Object> clientRuntimeProperties) {
        this.clientRuntimeProperties = clientRuntimeProperties;
    }

    @Override
    protected void configure() {
        install(new MessagingBinders.MessageBodyProviders(clientRuntimeProperties, RuntimeType.CLIENT),
                new MessagingBinders.HeaderDelegateProviders());

        bindFactory(ReferencingFactory.referenceFactory()).to(new GenericType<Ref<ClientConfig>>() {
        }).in(RequestScoped.class);

        bindFactory(RequestContextInjectionFactory.class)
                .to(ClientRequest.class)
                .in(RequestScoped.class);

        bindFactory(ReferencingFactory.referenceFactory()).to(new GenericType<Ref<ClientRequest>>() {
        }).in(RequestScoped.class);

        bindFactory(PropertiesDelegateFactory.class, Singleton.class).to(PropertiesDelegate.class).in(RequestScoped.class);

        // ChunkedInput entity support
        bind(ChunkedInputReader.class).to(MessageBodyReader.class).in(Singleton.class);
    }
}
