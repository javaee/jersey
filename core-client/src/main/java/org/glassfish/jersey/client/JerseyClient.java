/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Jersey implementation of {@link javax.ws.rs.client.Client JAX-RS JerseyClient}
 * contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyClient implements javax.ws.rs.client.Client {
    private final AtomicBoolean closedFlag = new AtomicBoolean(false);
    private final ClientConfig config;
    private final LinkedBlockingDeque<LifecycleListener> listeners = new LinkedBlockingDeque<LifecycleListener>();

    /**
     * Client life-cycle event listener contract.
     */
    static interface LifecycleListener {
        /**
         * Invoked when the client is closed.
         */
        public void onClose();
    }

    /**
     * Create a new Jersey client instance using a default configuration.
     */
    protected JerseyClient() {
        this.config = new ClientConfig(this);
    }

    /**
     * Create a new Jersey client instance.
     *
     * @param config jersey client configuration.
     */
    protected JerseyClient(final Configuration config) {
        this.config = new ClientConfig(this, config);
    }

    @Override
    public void close() {
        if (closedFlag.compareAndSet(false, true)) {
            release();
        }
    }

    private void release() {
        LifecycleListener listener;
        while ((listener = listeners.pollFirst()) != null) {
            listener.onClose();
        }
    }

    /**
     * Add a new client lifecycle listener.
     *
     * @param listener client lifecycle listener.
     */
    public void addListener(LifecycleListener listener) {
        checkNotClosed();
        listeners.push(listener);
    }

    /**
     * Check client state.
     *
     * @return {@code true} if current {@code JerseyClient} instance is closed, otherwise {@code false}.
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
    void checkNotClosed() throws IllegalStateException {
        checkState(!closedFlag.get(), "Client instance has been closed.");
    }

    @Override
    public JerseyWebTarget target(String uri) throws IllegalArgumentException, NullPointerException {
        checkNotClosed();
        checkNotNull(uri, "URI template of the newly created target must not be 'null'.");
        return new JerseyWebTarget(uri, this);
    }

    @Override
    public JerseyWebTarget target(URI uri) throws NullPointerException {
        checkNotClosed();
        checkNotNull(uri, "URI of the newly created target must not be 'null'.");
        return new JerseyWebTarget(uri, this);
    }

    @Override
    public JerseyWebTarget target(UriBuilder uriBuilder) throws NullPointerException {
        checkNotClosed();
        checkNotNull(uriBuilder, "URI builder of the newly created target must not be 'null'.");
        return new JerseyWebTarget(uriBuilder, this);
    }

    @Override
    public JerseyWebTarget target(Link link) throws NullPointerException {
        checkNotClosed();
        checkNotNull(link, "Link to the newly created target must not be 'null'.");
        return new JerseyWebTarget(link, this);
    }

    @Override
    public JerseyInvocation.Builder invocation(Link link) throws NullPointerException, IllegalArgumentException {
        checkNotClosed();
        checkNotNull(link, "Link of the newly created invocation must not be 'null'.");
        JerseyWebTarget t = new JerseyWebTarget(link, this);
        final String acceptType = link.getType();
        return (acceptType != null) ? t.request(acceptType) : t.request();
    }

    @Override
    public JerseyClient replaceWith(Configuration configuration) {
        checkNotClosed();
        this.config.replaceWith(configuration);
        return this;
    }

    @Override
    public JerseyClient register(Class<?> providerClass) {
        checkNotClosed();
        config.register(providerClass);
        return this;
    }

    @Override
    public JerseyClient register(Object provider) {
        checkNotClosed();
        config.register(provider);
        return this;
    }

    @Override
    public JerseyClient register(Class<?> providerClass, int bindingPriority) {
        checkNotClosed();
        config.register(providerClass, bindingPriority);
        return this;
    }

    @Override
    public JerseyClient register(Class<?> providerClass, Class<?>... contracts) {
        checkNotClosed();
        config.register(providerClass, contracts);
        return this;
    }

    @Override
    public JerseyClient register(Class<?> providerClass, Map<Class<?>, Integer> contracts) {
        checkNotClosed();
        config.register(providerClass, contracts);
        return this;
    }

    @Override
    public JerseyClient register(Object provider, int bindingPriority) {
        checkNotClosed();
        config.register(provider, bindingPriority);
        return this;
    }

    @Override
    public JerseyClient register(Object provider, Class<?>... contracts) {
        checkNotClosed();
        config.register(provider, contracts);
        return this;
    }

    @Override
    public JerseyClient register(Object provider, Map<Class<?>, Integer> contracts) {
        checkNotClosed();
        config.register(provider, contracts);
        return this;
    }

    @Override
    public JerseyClient setProperty(String name, Object value) {
        checkNotClosed();
        config.setProperty(name, value);
        return this;
    }

    @Override
    public ClientConfig getConfiguration() {
        checkNotClosed();
        return config.getConfiguration();
    }

    /**
     * Pre initializes the {@link Configuration configuration} of this client in order to improve
     * performance during the first request.
     * <p/>
     * Pre initialized configuration cannot be changed. Once this method is called no other
     * method implementing {@link javax.ws.rs.core.Configurable} must be called
     * on this pre initialized client or any other object derived from this client otherwise
     * exception will be thrown.
     *
     * @return Jersey client.
     */
    public JerseyClient preInitialize() {
        config.preInitialize();
        return this;
    }
}
