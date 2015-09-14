/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.client.spi.DefaultSslContextProvider;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;
import org.glassfish.jersey.internal.util.collection.Values;

import static jersey.repackaged.com.google.common.base.Preconditions.checkNotNull;
import static jersey.repackaged.com.google.common.base.Preconditions.checkState;

/**
 * Jersey implementation of {@link javax.ws.rs.client.Client JAX-RS Client}
 * contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyClient implements javax.ws.rs.client.Client, Initializable<JerseyClient> {
    private static final Logger LOG = Logger.getLogger(JerseyClient.class.getName());

    private static final DefaultSslContextProvider DEFAULT_SSL_CONTEXT_PROVIDER = new DefaultSslContextProvider() {
        @Override
        public SSLContext getDefaultSslContext() {
            return SslConfigurator.getDefaultContext();
        }
    };

    private final AtomicBoolean closedFlag = new AtomicBoolean(false);
    private final boolean isDefaultSslContext;
    private final ClientConfig config;
    private final HostnameVerifier hostnameVerifier;
    private final UnsafeValue<SSLContext, IllegalStateException> sslContext;
    private final LinkedBlockingDeque<WeakReference<JerseyClient.ShutdownHook>> shutdownHooks =
                                        new LinkedBlockingDeque<WeakReference<JerseyClient.ShutdownHook>>();
    private final ReferenceQueue<JerseyClient.ShutdownHook> shReferenceQueue = new ReferenceQueue<JerseyClient.ShutdownHook>();

    /**
     * Client instance shutdown hook.
     */
    interface ShutdownHook {
        /**
         * Invoked when the client instance is closed.
         */
        public void onShutdown();
    }

    /**
     * Create a new Jersey client instance using a default configuration.
     */
    protected JerseyClient() {
        this(null, (UnsafeValue<SSLContext, IllegalStateException>) null, null, null);
    }

    /**
     * Create a new Jersey client instance.
     *
     * @param config     jersey client configuration.
     * @param sslContext jersey client SSL context.
     * @param verifier   jersey client host name verifier.
     */
    protected JerseyClient(final Configuration config,
                           final SSLContext sslContext,
                           final HostnameVerifier verifier) {

        this(config, sslContext, verifier, null);
    }

    /**
     * Create a new Jersey client instance.
     *
     * @param config                    jersey client configuration.
     * @param sslContext                jersey client SSL context.
     * @param verifier                  jersey client host name verifier.
     * @param defaultSslContextProvider default SSL context provider.
     */
    protected JerseyClient(final Configuration config,
                           final SSLContext sslContext,
                           final HostnameVerifier verifier,
                           final DefaultSslContextProvider defaultSslContextProvider) {
        this(config, sslContext == null ? null : Values.<SSLContext, IllegalStateException>unsafe(sslContext), verifier,
             defaultSslContextProvider);
    }

    /**
     * Create a new Jersey client instance.
     *
     * @param config             jersey client configuration.
     * @param sslContextProvider jersey client SSL context provider.
     * @param verifier           jersey client host name verifier.
     */
    protected JerseyClient(final Configuration config,
                           final UnsafeValue<SSLContext, IllegalStateException> sslContextProvider,
                           final HostnameVerifier verifier) {
        this(config, sslContextProvider, verifier, null);
    }

    /**
     * Create a new Jersey client instance.
     *
     * @param config                    jersey client configuration.
     * @param sslContextProvider        jersey client SSL context provider. Non {@code null} provider is expected to
     *                                  return non-default value.
     * @param verifier                  jersey client host name verifier.
     * @param defaultSslContextProvider default SSL context provider.
     */
    protected JerseyClient(final Configuration config,
                           final UnsafeValue<SSLContext, IllegalStateException> sslContextProvider,
                           final HostnameVerifier verifier,
                           final DefaultSslContextProvider defaultSslContextProvider) {
        this.config = config == null ? new ClientConfig(this) : new ClientConfig(this, config);

        if (sslContextProvider == null) {
            this.isDefaultSslContext = true;

            if (defaultSslContextProvider != null) {
                this.sslContext = createLazySslContext(defaultSslContextProvider);
            } else {
                final DefaultSslContextProvider lookedUpSslContextProvider;

                final Iterator<DefaultSslContextProvider> iterator =
                        ServiceFinder.find(DefaultSslContextProvider.class).iterator();

                if (iterator.hasNext()) {
                    lookedUpSslContextProvider = iterator.next();
                } else {
                    lookedUpSslContextProvider = DEFAULT_SSL_CONTEXT_PROVIDER;
                }

                this.sslContext = createLazySslContext(lookedUpSslContextProvider);
            }
        } else {
            this.isDefaultSslContext = false;
            this.sslContext = Values.lazy(sslContextProvider);
        }

        this.hostnameVerifier = verifier;
    }

    @Override
    public void close() {
        if (closedFlag.compareAndSet(false, true)) {
            release();
        }
    }

    private void release() {
        Reference<ShutdownHook> listenerRef;
        while ((listenerRef = shutdownHooks.pollFirst()) != null) {
            JerseyClient.ShutdownHook listener = listenerRef.get();
            if (listener != null) {
                try {
                    listener.onShutdown();
                } catch (Throwable t) {
                    LOG.log(Level.WARNING, LocalizationMessages.ERROR_SHUTDOWNHOOK_CLOSE(listenerRef.getClass().getName()), t);
                }
            }
        }
    }

    private UnsafeValue<SSLContext, IllegalStateException> createLazySslContext(final DefaultSslContextProvider provider) {
        return Values.lazy(new UnsafeValue<SSLContext, IllegalStateException>() {
            @Override
            public SSLContext get() {
                return provider.getDefaultSslContext();
            }
        });
    }

    /**
     * Register a new client shutdown hook.
     *
     * @param shutdownHook client shutdown hook.
     */
    /* package */ void registerShutdownHook(final ShutdownHook shutdownHook) {
        checkNotClosed();
        shutdownHooks.push(new WeakReference<JerseyClient.ShutdownHook>(shutdownHook, shReferenceQueue));
        cleanUpShutdownHooks();
    }

    /**
     * Clean up shutdown hooks that have been garbage collected.
     */
    private void cleanUpShutdownHooks() {

        Reference<? extends ShutdownHook> reference;

        while ((reference = shReferenceQueue.poll()) != null) {

            shutdownHooks.remove(reference);

            final ShutdownHook shutdownHook = reference.get();
            if (shutdownHook != null) { // close this one off if still accessible
                shutdownHook.onShutdown();
            }
        }
    }

    /**
     * Check client state.
     *
     * @return {@code true} if current {@code JerseyClient} instance is closed, otherwise {@code false}.
     *
     * @see #close()
     */
    public boolean isClosed() {
        return closedFlag.get();
    }

    /**
     * Check that the client instance has not been closed.
     *
     * @throws IllegalStateException in case the client instance has been closed already.
     */
    void checkNotClosed() {
        checkState(!closedFlag.get(), LocalizationMessages.CLIENT_INSTANCE_CLOSED());
    }

    /**
     * Get information about used {@link SSLContext}.
     *
     * @return {@code true} when used {@code SSLContext} is acquired from {@link SslConfigurator#getDefaultContext()},
     * {@code false} otherwise.
     */
    public boolean isDefaultSslContext() {
        return isDefaultSslContext;
    }

    @Override
    public JerseyWebTarget target(final String uri) {
        checkNotClosed();
        checkNotNull(uri, LocalizationMessages.CLIENT_URI_TEMPLATE_NULL());
        return new JerseyWebTarget(uri, this);
    }

    @Override
    public JerseyWebTarget target(final URI uri) {
        checkNotClosed();
        checkNotNull(uri, LocalizationMessages.CLIENT_URI_NULL());
        return new JerseyWebTarget(uri, this);
    }

    @Override
    public JerseyWebTarget target(final UriBuilder uriBuilder) {
        checkNotClosed();
        checkNotNull(uriBuilder, LocalizationMessages.CLIENT_URI_BUILDER_NULL());
        return new JerseyWebTarget(uriBuilder, this);
    }

    @Override
    public JerseyWebTarget target(final Link link) {
        checkNotClosed();
        checkNotNull(link, LocalizationMessages.CLIENT_TARGET_LINK_NULL());
        return new JerseyWebTarget(link, this);
    }

    @Override
    public JerseyInvocation.Builder invocation(final Link link) {
        checkNotClosed();
        checkNotNull(link, LocalizationMessages.CLIENT_INVOCATION_LINK_NULL());
        final JerseyWebTarget t = new JerseyWebTarget(link, this);
        final String acceptType = link.getType();
        return (acceptType != null) ? t.request(acceptType) : t.request();
    }

    @Override
    public JerseyClient register(final Class<?> providerClass) {
        checkNotClosed();
        config.register(providerClass);
        return this;
    }

    @Override
    public JerseyClient register(final Object provider) {
        checkNotClosed();
        config.register(provider);
        return this;
    }

    @Override
    public JerseyClient register(final Class<?> providerClass, final int bindingPriority) {
        checkNotClosed();
        config.register(providerClass, bindingPriority);
        return this;
    }

    @Override
    public JerseyClient register(final Class<?> providerClass, final Class<?>... contracts) {
        checkNotClosed();
        config.register(providerClass, contracts);
        return this;
    }

    @Override
    public JerseyClient register(final Class<?> providerClass, final Map<Class<?>, Integer> contracts) {
        checkNotClosed();
        config.register(providerClass, contracts);
        return this;
    }

    @Override
    public JerseyClient register(final Object provider, final int bindingPriority) {
        checkNotClosed();
        config.register(provider, bindingPriority);
        return this;
    }

    @Override
    public JerseyClient register(final Object provider, final Class<?>... contracts) {
        checkNotClosed();
        config.register(provider, contracts);
        return this;
    }

    @Override
    public JerseyClient register(final Object provider, final Map<Class<?>, Integer> contracts) {
        checkNotClosed();
        config.register(provider, contracts);
        return this;
    }

    @Override
    public JerseyClient property(final String name, final Object value) {
        checkNotClosed();
        config.property(name, value);
        return this;
    }

    @Override
    public ClientConfig getConfiguration() {
        checkNotClosed();
        return config.getConfiguration();
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext.get();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    @Override
    public JerseyClient preInitialize() {
        config.preInitialize();
        return this;
    }
}
