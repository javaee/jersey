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

package org.glassfish.jersey.client;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.spi.DefaultSslContextProvider;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;
import org.glassfish.jersey.internal.util.collection.Values;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class DefaultSslContextProviderTest {

    @Test
    public void testProvidedDefaultSslContextProvider() {

        final ClientConfig clientConfig = new ClientConfig();

        final AtomicBoolean getDefaultSslContextCalled = new AtomicBoolean(false);


        final JerseyClient jerseyClient =
                new JerseyClient(clientConfig, SslConfigurator.getDefaultContext(),
                                 null, new DefaultSslContextProvider() {
                    @Override
                    public SSLContext getDefaultSslContext() {
                        getDefaultSslContextCalled.set(true);

                        return SslConfigurator.getDefaultContext();
                    }
                });

        jerseyClient.getSslContext();

        assertFalse(getDefaultSslContextCalled.get());
        assertFalse(jerseyClient.isDefaultSslContext());
    }

    @Test
    public void testProvidedDefaultSslContextProviderUnsafeVal() {

        final ClientConfig clientConfig = new ClientConfig();

        final AtomicBoolean getDefaultSslContextCalled = new AtomicBoolean(false);


        final JerseyClient jerseyClient =
                new JerseyClient(clientConfig, Values.<SSLContext,
                        IllegalStateException>unsafe(SslConfigurator.getDefaultContext()),
                                 null, new DefaultSslContextProvider() {
                    @Override
                    public SSLContext getDefaultSslContext() {
                        getDefaultSslContextCalled.set(true);

                        return SslConfigurator.getDefaultContext();
                    }
                });

        jerseyClient.getSslContext();

        assertFalse(getDefaultSslContextCalled.get());
        assertFalse(jerseyClient.isDefaultSslContext());
    }

    @Test
    public void testCustomDefaultSslContextProvider() {

        final ClientConfig clientConfig = new ClientConfig();

        final AtomicBoolean getDefaultSslContextCalled = new AtomicBoolean(false);
        final AtomicReference<SSLContext> returnedContext = new AtomicReference<SSLContext>(null);

        final JerseyClient jerseyClient =
                new JerseyClient(clientConfig, (SSLContext) null,
                                 null, new DefaultSslContextProvider() {
                    @Override
                    public SSLContext getDefaultSslContext() {
                        getDefaultSslContextCalled.set(true);

                        final SSLContext defaultSslContext = SslConfigurator.getDefaultContext();
                        returnedContext.set(defaultSslContext);
                        return defaultSslContext;
                    }
                });

        // make sure context is created
        jerseyClient.getSslContext();

        assertEquals(returnedContext.get(), jerseyClient.getSslContext());
        assertTrue(getDefaultSslContextCalled.get());
        assertTrue(jerseyClient.isDefaultSslContext());
    }

    @Test
    public void testCustomDefaultSslContextProviderUnsafeVal() {

        final ClientConfig clientConfig = new ClientConfig();

        final AtomicBoolean getDefaultSslContextCalled = new AtomicBoolean(false);
        final AtomicReference<SSLContext> returnedContext = new AtomicReference<SSLContext>(null);

        final JerseyClient jerseyClient =
                new JerseyClient(clientConfig, (UnsafeValue<SSLContext, IllegalStateException>) null,
                                 null, new DefaultSslContextProvider() {
                    @Override
                    public SSLContext getDefaultSslContext() {
                        getDefaultSslContextCalled.set(true);

                        final SSLContext defaultSslContext = SslConfigurator.getDefaultContext();
                        returnedContext.set(defaultSslContext);
                        return defaultSslContext;
                    }
                });

        // make sure context is created
        jerseyClient.getSslContext();

        assertEquals(returnedContext.get(), jerseyClient.getSslContext());
        assertTrue(getDefaultSslContextCalled.get());
        assertTrue(jerseyClient.isDefaultSslContext());
    }
}
