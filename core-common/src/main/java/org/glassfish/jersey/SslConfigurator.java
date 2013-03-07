/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Utility class, which helps to configure {@link SSLContext} instances.
 *
 * For example:
 * <pre>
 * SslConfigurator sslConfig = SslConfigurator.newInstance()
 *    .trustStoreFile("truststore.jks")
 *    .trustStorePassword("asdfgh")
 *    .trustStoreType("JKS")
 *    .trustManagerFactoryAlgorithm("PKIX")
 *
 *    .keyStoreFile("keystore.jks")
 *    .keyPassword("asdfgh")
 *    .keyStoreType("JKS")
 *    .keyManagerFactoryAlgorithm("SunX509")
 *    .keyStoreProvider("SunJSSE")
 *
 *    .securityProtocol("SSL");
 *
 * SSLContext sslContext = sslConfig.createSSLContext();
 * </pre>
 *
 * @author Alexey Stashok
 * @author Hubert Iwaniuk
 * @author Bruno Harbulot
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@SuppressWarnings("UnusedDeclaration")
public final class SslConfigurator {
    /**
     * <em>Trust</em> store provider name.
     *
     * The value MUST be a {@code String} representing the name of a <em>trust</em> store provider.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String TRUST_STORE_PROVIDER = "javax.net.ssl.trustStoreProvider";
    /**
     * <em>Key</em> store provider name.
     *
     * The value MUST be a {@code String} representing the name of a <em>trust</em> store provider.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String KEY_STORE_PROVIDER = "javax.net.ssl.keyStoreProvider";
    /**
     * <em>Trust</em> store file name.
     *
     * The value MUST be a {@code String} representing the name of a <em>trust</em> store file.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String TRUST_STORE_FILE = "javax.net.ssl.trustStore";
    /**
     * <em>Key</em> store file name.
     *
     * The value MUST be a {@code String} representing the name of a <em>key</em> store file.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String KEY_STORE_FILE = "javax.net.ssl.keyStore";
    /**
     * <em>Trust</em> store file password - the password used to unlock the <em>trust</em> store file.
     *
     * The value MUST be a {@code String} representing the <em>trust</em> store file password.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    /**
     * <em>Key</em> store file password - the password used to unlock the <em>trust</em> store file.
     *
     * The value MUST be a {@code String} representing the <em>key</em> store file password.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    /**
     * <em>Trust</em> store type (see {@link java.security.KeyStore#getType()} for more info).
     *
     * The value MUST be a {@code String} representing the <em>trust</em> store type name.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    /**
     * <em>Key</em> store type (see {@link java.security.KeyStore#getType()} for more info).
     *
     * The value MUST be a {@code String} representing the <em>key</em> store type name.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";
    /**
     * <em>Key</em> manager factory algorithm name.
     *
     * The value MUST be a {@code String} representing the <em>key</em> manager factory algorithm name.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String KEY_MANAGER_FACTORY_ALGORITHM = "ssl.keyManagerFactory.algorithm";
    /**
     * <em>Key</em> manager factory provider name.
     *
     * The value MUST be a {@code String} representing the <em>key</em> manager factory provider name.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String KEY_MANAGER_FACTORY_PROVIDER = "ssl.keyManagerFactory.provider";
    /**
     * <em>Trust</em> manager factory algorithm name.
     *
     * The value MUST be a {@code String} representing the <em>trust</em> manager factory algorithm name.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String TRUST_MANAGER_FACTORY_ALGORITHM = "ssl.trustManagerFactory.algorithm";
    /**
     * <em>Trust</em> manager factory provider name.
     *
     * The value MUST be a {@code String} representing the <em>trust</em> manager factory provider name.
     * <p>
     * No default value is set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String TRUST_MANAGER_FACTORY_PROVIDER = "ssl.trustManagerFactory.provider";

    /**
     * Default SSL configuration. If you have changed any of
     * {@link System#getProperties()} of javax.net.ssl family you should refresh
     * this configuration by calling {@link #retrieve(java.util.Properties)}.
     */
    private static final SslConfigurator DEFAULT_CONFIG = new SslConfigurator(true);

    private KeyStore keyStore;
    private KeyStore trustStore;

    private String trustStoreProvider;
    private String keyStoreProvider;

    private String trustStoreType;
    private String keyStoreType;

    private char[] trustStorePass;
    private char[] keyStorePass;
    private char[] keyPass;

    private String trustStoreFile;
    private String keyStoreFile;

    private byte[] trustStoreBytes;
    private byte[] keyStoreBytes;

    private String trustManagerFactoryAlgorithm;
    private String keyManagerFactoryAlgorithm;

    private String trustManagerFactoryProvider;
    private String keyManagerFactoryProvider;

    private String securityProtocol = "TLS";

    /**
     * Get a new instance of a {@link SSLContext} configured using default
     * configuration settings.
     *
     * @return new instance of a default SSL context.
     */
    public static SSLContext getDefaultContext() {
        return DEFAULT_CONFIG.createSSLContext();
    }

    /**
     * Get a new & initialized SSL configurator instance.
     *
     * The instance {@link #retrieve(java.util.Properties) retrieves} the initial configuration from
     * {@link System#getProperties() system properties}.
     *
     * @return new & initialized SSL configurator instance.
     */
    public static SslConfigurator newInstance() {
        return new SslConfigurator(true);
    }

    /**
     * Get a new SSL configurator instance.
     *
     * @param readSystemProperties if {@code true}, {@link #retrieve(java.util.Properties) Retrieves}
     *                             the initial configuration from {@link System#getProperties()},
     *                             otherwise the instantiated configurator will be empty.
     * @return new SSL configurator instance.
     */
    public static SslConfigurator newInstance(boolean readSystemProperties) {
        return new SslConfigurator(readSystemProperties);
    }

    private SslConfigurator(boolean readSystemProperties) {
        if (readSystemProperties) {
            retrieve(System.getProperties());
        }
    }

    /**
     * Set the <em>trust</em> store provider name.
     *
     * @param trustStoreProvider <em>trust</em> store provider to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator trustStoreProvider(String trustStoreProvider) {
        this.trustStoreProvider = trustStoreProvider;
        return this;
    }

    /**
     * Set the <em>key</em> store provider name.
     *
     * @param keyStoreProvider <em>key</em> store provider to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyStoreProvider(String keyStoreProvider) {
        this.keyStoreProvider = keyStoreProvider;
        return this;
    }

    /**
     * Set the type of <em>trust</em> store.
     *
     * @param trustStoreType type of <em>trust</em> store to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator trustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
        return this;
    }

    /**
     * Set the type of <em>key</em> store.
     *
     * @param keyStoreType type of <em>key</em> store to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
        return this;
    }

    /**
     * Set the password of <em>trust</em> store.
     *
     * @param password password of <em>trust</em> store to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator trustStorePassword(String password) {
        this.trustStorePass = password.toCharArray();
        return this;
    }

    /**
     * Set the password of <em>key</em> store.
     *
     * @param password password of <em>key</em> store to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyStorePassword(String password) {
        this.keyStorePass = password.toCharArray();
        return this;
    }

    /**
     * Set the password of <em>key</em> store.
     *
     * @param password password of <em>key</em> store to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyStorePassword(char[] password) {
        this.keyStorePass = password;
        return this;
    }

    /**
     * Set the password of the key in the <em>key</em> store.
     *
     * @param password password of <em>key</em> to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyPassword(String password) {
        this.keyPass = password.toCharArray();
        return this;
    }

    /**
     * Set the password of the key in the <em>key</em> store.
     *
     * @param password password of <em>key</em> to set.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyPassword(char[] password) {
        this.keyPass = password;
        return this;
    }

    /**
     * Set the <em>trust</em> store file name.
     * <p>
     * Setting a trust store instance resets any {@link #trustStore(java.security.KeyStore) trust store instance}
     * or {@link #trustStoreBytes(byte[]) trust store payload} value previously set.
     * </p>
     *
     * @param fileName {@link java.io.File file} name of the <em>trust</em> store.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator trustStoreFile(String fileName) {
        this.trustStoreFile = fileName;
        this.trustStoreBytes = null;
        this.trustStore = null;
        return this;
    }

    /**
     * Set the <em>trust</em> store payload as byte array.
     * <p>
     * Setting a trust store instance resets any {@link #trustStoreFile(String) trust store file}
     * or {@link #trustStore(java.security.KeyStore) trust store instance} value previously set.
     * </p>
     *
     * @param payload <em>trust</em> store payload.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator trustStoreBytes(byte[] payload) {
        this.trustStoreBytes = payload;
        this.trustStoreFile = null;
        this.trustStore = null;
        return this;
    }

    /**
     * Set the <em>key</em> store file name.
     * <p>
     * Setting a key store instance resets any {@link #keyStore(java.security.KeyStore) key store instance}
     * or {@link #keyStoreBytes(byte[]) key store payload} value previously set.
     * </p>
     *
     * @param fileName {@link java.io.File file} name of the <em>key</em> store.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyStoreFile(String fileName) {
        this.keyStoreFile = fileName;
        this.keyStoreBytes = null;
        this.keyStore = null;
        return this;
    }

    /**
     * Set the <em>key</em> store payload as byte array.
     * <p>
     * Setting a key store instance resets any {@link #keyStoreFile(String) key store file}
     * or {@link #keyStore(java.security.KeyStore) key store instance} value previously set.
     * </p>
     *
     * @param payload <em>key</em> store payload.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyStoreBytes(byte[] payload) {
        this.keyStoreBytes = payload;
        this.keyStoreFile = null;
        this.keyStore = null;
        return this;
    }

    /**
     * Set the <em>trust</em> manager factory algorithm.
     *
     * @param algorithm the <em>trust</em> manager factory algorithm.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator trustManagerFactoryAlgorithm(String algorithm) {
        this.trustManagerFactoryAlgorithm = algorithm;
        return this;
    }

    /**
     * Set the <em>key</em> manager factory algorithm.
     *
     * @param algorithm the <em>key</em> manager factory algorithm.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyManagerFactoryAlgorithm(String algorithm) {
        this.keyManagerFactoryAlgorithm = algorithm;
        return this;
    }

    /**
     * Set the <em>trust</em> manager factory provider.
     *
     * @param provider the <em>trust</em> manager factory provider.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator trustManagerFactoryProvider(String provider) {
        this.trustManagerFactoryAlgorithm = provider;
        return this;
    }

    /**
     * Set the <em>key</em> manager factory provider.
     *
     * @param provider the <em>key</em> manager factory provider.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyManagerFactoryProvider(String provider) {
        this.keyManagerFactoryAlgorithm = provider;
        return this;
    }

    /**
     * Set the SSLContext protocol. The default value is {@code TLS} if this is {@code null}.
     *
     * @param protocol protocol for {@link javax.net.ssl.SSLContext#getProtocol()}.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator securityProtocol(String protocol) {
        this.securityProtocol = protocol;
        return this;
    }

    /**
     * Get the <em>key</em> store instance.
     *
     * @return <em>key</em> store instance or {@code null} if not explicitly set.
     */
    KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * Set the <em>key</em> store instance.
     * <p>
     * Setting a key store instance resets any {@link #keyStoreFile(String) key store file}
     * or {@link #keyStoreBytes(byte[]) key store payload} value previously set.
     * </p>
     *
     * @param keyStore <em>key</em> store instance.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator keyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
        this.keyStoreFile = null;
        this.keyStoreBytes = null;
        return this;
    }

    /**
     * Get the <em>trust</em> store instance.
     * <p>
     * Setting a trust store instance resets any {@link #trustStoreFile(String) trust store file}
     * or {@link #trustStoreBytes(byte[]) trust store payload} value previously set.
     * </p>
     *
     * @return <em>trust</em> store instance or {@code null} if not explicitly set.
     */
    KeyStore getTrustStore() {
        return trustStore;
    }

    /**
     * Set the <em>trust</em> store instance.
     *
     * @param trustStore <em>trust</em> store instance.
     * @return updated SSL configurator instance.
     */
    public SslConfigurator trustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
        this.trustStoreFile = null;
        this.trustStoreBytes = null;
        return this;
    }

    /**
     * Create new SSL context instance using the current SSL context configuration.
     *
     * @return newly configured SSL context instance.
     */
    public SSLContext createSSLContext() {
        TrustManagerFactory trustManagerFactory = null;
        KeyManagerFactory keyManagerFactory = null;

        KeyStore _keyStore = keyStore;
        if (_keyStore == null && (keyStoreBytes != null || keyStoreFile != null)) {
            try {
                if (keyStoreProvider != null) {
                    _keyStore = KeyStore.getInstance(
                            keyStoreType != null ? keyStoreType : KeyStore.getDefaultType(), keyStoreProvider);
                } else {
                    _keyStore = KeyStore.getInstance(keyStoreType != null ? keyStoreType : KeyStore.getDefaultType());
                }
                InputStream keyStoreInputStream = null;
                try {
                    if (keyStoreBytes != null) {
                        keyStoreInputStream = new ByteArrayInputStream(keyStoreBytes);
                    } else if (!keyStoreFile.equals("NONE")) {
                        keyStoreInputStream = new FileInputStream(keyStoreFile);
                    }
                    _keyStore.load(keyStoreInputStream, keyStorePass);
                } finally {
                    try {
                        if (keyStoreInputStream != null) {
                            keyStoreInputStream.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KS_IMPL_NOT_FOUND(), e);
            } catch (CertificateException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KS_CERT_LOAD_ERROR(), e);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KS_FILE_NOT_FOUND(keyStoreFile), e);
            } catch (IOException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KS_LOAD_ERROR(keyStoreFile), e);
            } catch (NoSuchProviderException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KS_PROVIDERS_NOT_REGISTERED(), e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KS_INTEGRITY_ALGORITHM_NOT_FOUND(), e);
            }
        }
        if (_keyStore != null) {
            String kmfAlgorithm = keyManagerFactoryAlgorithm;
            if (kmfAlgorithm == null) {
                kmfAlgorithm = System.getProperty(
                        KEY_MANAGER_FACTORY_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
            }
            try {
                if (keyManagerFactoryProvider != null) {
                    keyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm, keyManagerFactoryProvider);
                } else {
                    keyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm);
                }
                keyManagerFactory.init(_keyStore, keyPass != null ? keyPass : keyStorePass);
            } catch (KeyStoreException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KMF_INIT_FAILED(), e);
            } catch (UnrecoverableKeyException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KMF_UNRECOVERABLE_KEY(), e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KMF_ALGORITHM_NOT_SUPPORTED(), e);
            } catch (NoSuchProviderException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_KMF_PROVIDER_NOT_REGISTERED(), e);
            }
        }

        KeyStore _trustStore = trustStore;
        if (_trustStore == null && (trustStoreBytes != null || trustStoreFile != null)) {
            try {
                if (trustStoreProvider != null) {
                    _trustStore = KeyStore.getInstance(
                            trustStoreType != null ? trustStoreType : KeyStore.getDefaultType(), trustStoreProvider);
                } else {
                    _trustStore =
                            KeyStore.getInstance(trustStoreType != null ? trustStoreType : KeyStore.getDefaultType());
                }
                InputStream trustStoreInputStream = null;
                try {
                    if (trustStoreBytes != null) {
                        trustStoreInputStream = new ByteArrayInputStream(trustStoreBytes);
                    } else if (!trustStoreFile.equals("NONE")) {
                        trustStoreInputStream = new FileInputStream(trustStoreFile);
                    }
                    _trustStore.load(trustStoreInputStream, trustStorePass);
                } finally {
                    try {
                        if (trustStoreInputStream != null) {
                            trustStoreInputStream.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TS_IMPL_NOT_FOUND(), e);
            } catch (CertificateException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TS_CERT_LOAD_ERROR(), e);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TS_FILE_NOT_FOUND(trustStoreFile), e);
            } catch (IOException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TS_LOAD_ERROR(trustStoreFile), e);
            } catch (NoSuchProviderException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TS_PROVIDERS_NOT_REGISTERED(), e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TS_INTEGRITY_ALGORITHM_NOT_FOUND(), e);
            }
        }
        if (_trustStore != null) {
            String tmfAlgorithm = trustManagerFactoryAlgorithm;
            if (tmfAlgorithm == null) {
                tmfAlgorithm = System.getProperty(
                        TRUST_MANAGER_FACTORY_ALGORITHM, TrustManagerFactory.getDefaultAlgorithm());
            }

            try {
                if (trustManagerFactoryProvider != null) {
                    trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm, trustManagerFactoryProvider);
                } else {
                    trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
                }
                trustManagerFactory.init(_trustStore);
            } catch (KeyStoreException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TMF_INIT_FAILED(), e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TMF_ALGORITHM_NOT_SUPPORTED(), e);
            } catch (NoSuchProviderException e) {
                throw new IllegalStateException(LocalizationMessages.SSL_TMF_PROVIDER_NOT_REGISTERED(), e);
            }
        }

        try {
            String secProtocol = "TLS";
            if (securityProtocol != null) {
                secProtocol = securityProtocol;
            }
            final SSLContext sslContext = SSLContext.getInstance(secProtocol);
            sslContext.init(
                    keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                    trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null,
                    null);
            return sslContext;
        } catch (KeyManagementException e) {
            throw new IllegalStateException(LocalizationMessages.SSL_CTX_INIT_FAILED(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(LocalizationMessages.SSL_CTX_ALGORITHM_NOT_SUPPORTED(), e);
        }
    }

    /**
     * Retrieve the SSL context configuration from the supplied properties.
     *
     * @param props properties containing the SSL context configuration.
     */
    public void retrieve(Properties props) {
        trustStoreProvider = props.getProperty(TRUST_STORE_PROVIDER);
        keyStoreProvider = props.getProperty(KEY_STORE_PROVIDER);

        trustManagerFactoryProvider = props.getProperty(TRUST_MANAGER_FACTORY_PROVIDER);
        keyManagerFactoryProvider = props.getProperty(KEY_MANAGER_FACTORY_PROVIDER);

        trustStoreType = props.getProperty(TRUST_STORE_TYPE);
        keyStoreType = props.getProperty(KEY_STORE_TYPE);

        if (props.getProperty(TRUST_STORE_PASSWORD) != null) {
            trustStorePass = props.getProperty(TRUST_STORE_PASSWORD).toCharArray();
        } else {
            trustStorePass = null;
        }

        if (props.getProperty(KEY_STORE_PASSWORD) != null) {
            keyStorePass = props.getProperty(KEY_STORE_PASSWORD).toCharArray();
        } else {
            keyStorePass = null;
        }

        trustStoreFile = props.getProperty(TRUST_STORE_FILE);
        keyStoreFile = props.getProperty(KEY_STORE_FILE);

        trustStoreBytes = null;
        keyStoreBytes = null;

        trustStore = null;
        keyStore = null;

        securityProtocol = "TLS";
    }
}
