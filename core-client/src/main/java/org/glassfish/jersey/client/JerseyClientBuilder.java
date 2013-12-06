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
package org.glassfish.jersey.client;

import java.security.KeyStore;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;
import org.glassfish.jersey.internal.util.collection.Values;

/**
 * Jersey provider of {@link javax.ws.rs.client.ClientBuilder JAX-RS client builder}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyClientBuilder extends ClientBuilder {

    private final ClientConfig config;
    private HostnameVerifier hostnameVerifier;
    private SslConfigurator sslConfigurator;
    private SSLContext sslContext;

    /**
     * Create a new custom-configured {@link JerseyClient} instance.
     *
     * @return new configured Jersey client instance.
     * @since 2.5
     */
    public static JerseyClient createClient() {
        return new JerseyClientBuilder().build();
    }

    /**
     * Create a new custom-configured {@link JerseyClient} instance.
     *
     * @param configuration data used to provide initial configuration for the new
     *                      Jersey client instance.
     * @return new configured Jersey client instance.
     * @since 2.5
     */
    public static JerseyClient createClient(Configuration configuration) {
        return new JerseyClientBuilder().withConfig(configuration).build();
    }

    /**
     * Create new Jersey client builder instance.
     */
    public JerseyClientBuilder() {
        this.config = new ClientConfig();
    }

    @Override
    public JerseyClientBuilder sslContext(SSLContext sslContext) {
        if (sslContext == null) {
            throw new NullPointerException(LocalizationMessages.NULL_SSL_CONTEXT());
        }
        this.sslContext = sslContext;
        sslConfigurator = null;
        return this;
    }

    @Override
    public JerseyClientBuilder keyStore(KeyStore keyStore, char[] password) {
        if (keyStore == null) {
            throw new NullPointerException(LocalizationMessages.NULL_KEYSTORE());
        }
        if (password == null) {
            throw new NullPointerException(LocalizationMessages.NULL_KEYSTORE_PASWORD());
        }
        if (sslConfigurator == null) {
            sslConfigurator = SslConfigurator.newInstance();
        }
        sslConfigurator.keyStore(keyStore);
        sslConfigurator.keyPassword(password);
        sslContext = null;
        return this;
    }

    @Override
    public JerseyClientBuilder trustStore(KeyStore trustStore) {
        if (trustStore == null) {
            throw new NullPointerException(LocalizationMessages.NULL_TRUSTSTORE());
        }
        if (sslConfigurator == null) {
            sslConfigurator = SslConfigurator.newInstance();
        }
        sslConfigurator.trustStore(trustStore);
        sslContext = null;
        return this;
    }

    @Override
    public JerseyClientBuilder hostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    @Override
    public JerseyClient build() {
        if (sslContext != null) {
            return new JerseyClient(config, sslContext, hostnameVerifier);
        } else if (sslConfigurator != null) {
            final SslConfigurator sslConfiguratorCopy = sslConfigurator.copy();
            return new JerseyClient(
                    config,
                    Values.lazy(new UnsafeValue<SSLContext, IllegalStateException>() {
                        @Override
                        public SSLContext get() {
                            return sslConfiguratorCopy.createSSLContext();
                        }
                    }),
                    hostnameVerifier);
        } else {
            return new JerseyClient(config, (UnsafeValue<SSLContext, IllegalStateException>) null, hostnameVerifier);
        }
    }

    @Override
    public ClientConfig getConfiguration() {
        return config;
    }

    @Override
    public JerseyClientBuilder property(String name, Object value) {
        this.config.property(name, value);
        return this;
    }

    @Override
    public JerseyClientBuilder register(Class<?> componentClass) {
        this.config.register(componentClass);
        return this;
    }

    @Override
    public JerseyClientBuilder register(Class<?> componentClass, int priority) {
        this.config.register(componentClass, priority);
        return this;
    }

    @Override
    public JerseyClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        this.config.register(componentClass, contracts);
        return this;
    }

    @Override
    public JerseyClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        this.config.register(componentClass, contracts);
        return this;
    }

    @Override
    public JerseyClientBuilder register(Object component) {
        this.config.register(component);
        return this;
    }

    @Override
    public JerseyClientBuilder register(Object component, int priority) {
        this.config.register(component, priority);
        return this;
    }

    @Override
    public JerseyClientBuilder register(Object component, Class<?>... contracts) {
        this.config.register(component, contracts);
        return this;
    }

    @Override
    public JerseyClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        this.config.register(component, contracts);
        return this;
    }

    @Override
    public JerseyClientBuilder withConfig(Configuration config) {
        this.config.loadFrom(config);
        return this;
    }
}
