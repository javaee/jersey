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

package org.glassfish.jersey.client;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * SSL configuration for HTTPS connections.
 * <p>
 * An instance of this class may be added as a property of the {@link ClientConfig}
 * using {@link ClientProperties#SSL_CONFIG}.
 * </p>
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class SslConfig {

    private final HostnameVerifier hostnameVerifier;
    private final SSLContext sslContext;

    /**
     * Create SSL configuration with no {@link HostnameVerifier}
     * and a default {@link SSLContext} constructed using {@code SSLContext.getInstance("SSL")}.
     *
     * @throws java.security.NoSuchAlgorithmException
     *          if the {@code SSLContext} could not be created.
     */
    public SslConfig() throws NoSuchAlgorithmException {
        this(null, SSLContext.getInstance("SSL"));
    }

    /**
     * Create SSL configuration with a {@link HostnameVerifier} and a default {@link SSLContext}
     * constructed using {@code SSLContext.getInstance("SSL")}.
     *
     * @param verifier the HostnameVerifier.
     * @throws java.security.NoSuchAlgorithmException
     *          if the {@code SSLContext} could not be created.
     */
    public SslConfig(HostnameVerifier verifier) throws NoSuchAlgorithmException {
        this(verifier, SSLContext.getInstance("SSL"));
    }

    /**
     * Create SSL configuration with a {@link HostnameVerifier} and a {@link SSLContext}.
     *
     * @param verifier the hostname verifier.
     * @param context  the SSL context. Must not be {@code null}.
     */
    public SslConfig(HostnameVerifier verifier, SSLContext context) {
        if (context == null)
            throw new IllegalArgumentException("SSLContext must not be null.");

        this.hostnameVerifier = verifier;
        this.sslContext = context;
    }

    /**
     * Create SSL configuration with no {@link HostnameVerifier} and a specified
     * {@link SSLContext}.
     *
     * @param context the SSL context. Must not be {@code null}.
     */
    public SslConfig(SSLContext context) {
        if (context == null)
            throw new IllegalArgumentException("SSLContext must not be null.");

        this.hostnameVerifier = null;
        this.sslContext = context;
    }

    /**
     * Check if a {@link HostnameVerifier hostname verifier} is configured in this SSL config.
     *
     * @return {@code true} if a hostname verifier is configured, {@code false} otherwise.
     */
    public boolean isHostnameVerifierSet() {
        return hostnameVerifier != null;
    }

    /**
     * Get the {@link HostnameVerifier hostname verifier}.
     *
     * @return the configured hostname verifier, or {@code null} if not set.
     */
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Get the {@link SSLContext}.
     *
     * @return the SSL context. Is never {@code null}.
     */
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
