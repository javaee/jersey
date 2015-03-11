/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.authentication;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Features that provides Http Basic and Digest client authentication (based on RFC 2617).
 * <p>
 * The feature can work in following modes:
 * <ul>
 *  <li><b>BASIC:</b> Basic preemptive authentication. In preemptive mode the authentication information
 *  is send always with each HTTP request. This mode is more usual than the following non-preemptive mode
 *  (if you require BASIC authentication you will probably use this preemptive mode). This mode must
 *  be combined with usage of SSL/TLS as the password is send only BASE64 encoded.</li>
 *  <li><i>BASIC NON-PREEMPTIVE:</i> Basic non-preemptive authentication. In non-preemptive mode the
 *  authentication information is added only when server refuses the request with {@code 401} status code and
 *  then the request is repeated with authentication information. This mode has negative impact on the performance.
 *  The advantage is that it does not send credentials when they are not needed. This mode must
 *  be combined with usage of SSL/TLS as the password is send only BASE64 encoded.
 *  </li>
 *  <li><b>DIGEST:</b> Http digest authentication. Does not require usage of SSL/TLS.</li>
 *  <li><b>UNIVERSAL:</b> Combination of basic and digest authentication. The feature works in non-preemptive
 *  mode which means that it sends requests without authentication information. If {@code 401} status
 *  code is returned, the request is repeated and an appropriate authentication is used based on the
 *  authentication requested in the response (defined in {@code WWW-Authenticate} HTTP header. The feature
 *  remembers which authentication requests were successful for given URI and next time tries to preemptively
 *  authenticate against this URI with latest successful authentication method.
 *  </li>
 * </ul>
 * </p>
 * <p>
 * To initialize the feature use static method of this feature.
 * </p>
 * <p>
 * Example of building the feature in
 * Basic authentication mode:
 * <pre>
 * HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("user", "superSecretPassword");
 * </pre>
 * </p>
 * <p>
 * Example of building the feature in basic non-preemptive mode:
 * <pre>
 * HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
 *     .nonPreemptive().credentials("user", "superSecretPassword").build();
 * </pre>
 * </p>
 * <p>
 * Example of building the feature in universal mode:
 * <pre>
 * HttpAuthenticationFeature feature = HttpAuthenticationFeature.universal("user", "superSecretPassword");
 * </pre>
 * </p>
 * <p>
 * Example of building the feature in universal mode with different credentials for basic and digest:
 * <pre>
 * HttpAuthenticationFeature feature = HttpAuthenticationFeature.universalBuilder()
 *      .credentialsForBasic("user", "123456")
 *      .credentials("adminuser", "hello")
 *      .build();
 * </pre>
 * </p>
 * Example of building the feature in basic preemptive mode with no default credentials. Credentials will have
 * to be supplied with each request using request properties (see below):
 * <pre>
 * HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().build();
 * </pre>
 * </p>
 * <p>
 * Once the feature is built it needs to be registered into the {@link javax.ws.rs.client.Client},
 * {@link javax.ws.rs.client.WebTarget} or other client configurable object. Example:
 * <pre>
 * final Client client = ClientBuilder.newClient();
 * client.register(feature);
 * </pre>
 * </p>
 *
 * Then you invoke requests as usual and authentication will be handled by the feature.
 * You can change the credentials for each request using properties
 * {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature#HTTP_AUTHENTICATION_USERNAME} and
 * {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature#HTTP_AUTHENTICATION_PASSWORD}. Example:
 * <pre>
 * final Response response = client.target("http://localhost:8080/rest/homer/contact").request()
 *    .property(HTTP_AUTHENTICATION_BASIC_USERNAME, "homer")
 *    .property(HTTP_AUTHENTICATION_BASIC_PASSWORD, "p1swd745").get();
 * </pre>
 * <p>
 * This class also contains property key definitions for overriding only specific basic or digest credentials:
 * <ul>
 *     <li>
 *      {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature#HTTP_AUTHENTICATION_BASIC_USERNAME} and
 *      {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature#HTTP_AUTHENTICATION_BASIC_PASSWORD}
 *      </li>
 *     <li>
 *      {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature#HTTP_AUTHENTICATION_DIGEST_USERNAME} and
 *      {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature#HTTP_AUTHENTICATION_DIGEST_PASSWORD}.
 *      </li>
 * </ul>
 * </p>
 *
 * @author Miroslav Fuksa
 *
 * @since 2.5
 */
public class HttpAuthenticationFeature implements Feature {

    /**
     * Feature authentication mode.
     */
    static enum Mode {
        /**
         * Basic preemptive.
         **/
        BASIC_PREEMPTIVE,
        /**
         * Basic non preemptive
         */
        BASIC_NON_PREEMPTIVE,
        /**
         * Digest.
         */
        DIGEST,
        /**
         * Universal.
         */
        UNIVERSAL
    }

    /**
     * Builder that creates instances of {@link HttpAuthenticationFeature}.
     */
    public static interface Builder {

        /**
         * Set credentials.
         *
         * @param username Username.
         * @param password Password as byte array.
         * @return This builder.
         */
        public Builder credentials(String username, byte[] password);

        /**
         * Set credentials.
         *
         * @param username Username.
         * @param password Password as {@link String}.
         * @return This builder.
         */
        public Builder credentials(String username, String password);

        /**
         * Build the feature.
         *
         * @return Http authentication feature configured from this builder.
         */
        public HttpAuthenticationFeature build();
    }

    /**
     * Extension of {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.Builder}
     * that builds the http authentication feature configured for basic authentication.
     */
    public static interface BasicBuilder extends Builder {

        /**
         * Configure the builder to create features in non-preemptive basic authentication mode.
         *
         * @return This builder.
         */
        public BasicBuilder nonPreemptive();
    }

    /**
     * Extension of {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.Builder}
     * that builds the http authentication feature configured in universal mode that supports
     * basic and digest authentication.
     */
    public static interface UniversalBuilder extends Builder {

        /**
         * Set credentials that will be used for basic authentication only.
         *
         * @param username Username.
         * @param password Password as {@link String}.
         * @return This builder.
         */
        public UniversalBuilder credentialsForBasic(String username, String password);

        /**
         * Set credentials that will be used for basic authentication only.
         *
         * @param username Username.
         * @param password Password as {@code byte array}.
         * @return This builder.
         */
        public UniversalBuilder credentialsForBasic(String username, byte[] password);

        /**
         * Set credentials that will be used for digest authentication only.
         *
         * @param username Username.
         * @param password Password as {@link String}.
         * @return This builder.
         */
        public UniversalBuilder credentialsForDigest(String username, String password);

        /**
         * Set credentials that will be used for digest authentication only.
         *
         * @param username Username.
         * @param password Password as {@code byte array}.
         * @return This builder.
         */
        public UniversalBuilder credentialsForDigest(String username, byte[] password);
    }

    /**
     * Implementation of all authentication builders.
     */
    static class BuilderImpl implements UniversalBuilder, BasicBuilder {

        private String usernameBasic;
        private byte[] passwordBasic;
        private String usernameDigest;
        private byte[] passwordDigest;
        private Mode mode;

        /**
         * Create a new builder.
         *
         * @param mode Mode in which the final authentication feature should work.
         */
        public BuilderImpl(Mode mode) {
            this.mode = mode;
        }

        @Override
        public Builder credentials(String username, String password) {
            return credentials(username, password == null ? null : password.getBytes(HttpAuthenticationFilter.CHARACTER_SET));
        }

        @Override
        public Builder credentials(String username, byte[] password) {
            credentialsForBasic(username, password);
            credentialsForDigest(username, password);
            return this;
        }

        @Override
        public UniversalBuilder credentialsForBasic(String username, String password) {
            return credentialsForBasic(username,
                    password == null ? null : password.getBytes(HttpAuthenticationFilter.CHARACTER_SET));
        }

        @Override
        public UniversalBuilder credentialsForBasic(String username, byte[] password) {
            this.usernameBasic = username;
            this.passwordBasic = password;
            return this;
        }

        @Override
        public UniversalBuilder credentialsForDigest(String username, String password) {
            return credentialsForDigest(username,
                    password == null ? null : password.getBytes(HttpAuthenticationFilter.CHARACTER_SET));
        }

        @Override
        public UniversalBuilder credentialsForDigest(String username, byte[] password) {
            this.usernameDigest = username;
            this.passwordDigest = password;
            return this;
        }

        @Override
        public HttpAuthenticationFeature build() {
            return new HttpAuthenticationFeature(mode,
                    usernameBasic == null ? null
                            : new HttpAuthenticationFilter.Credentials(usernameBasic, passwordBasic),
                    usernameDigest == null ? null
                            : new HttpAuthenticationFilter.Credentials(usernameDigest, passwordDigest));
        }

        @Override
        public BasicBuilder nonPreemptive() {
            if (mode == Mode.BASIC_PREEMPTIVE) {
                this.mode = Mode.BASIC_NON_PREEMPTIVE;
            }
            return this;
        }
    }

    /**
     * Key of the property that can be set into the {@link javax.ws.rs.client.ClientRequestContext client request}
     * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} in order to override
     * the username for http authentication feature for the request.
     * <p>
     * Example:
     * <pre>
     * Response response = client.target("http://localhost:8080/rest/joe/orders").request()
     *      .property(HTTP_AUTHENTICATION_USERNAME, "joe")
     *      .property(HTTP_AUTHENTICATION_PASSWORD, "p1swd745").get();
     * </pre>
     * </p>
     * The property must be always combined with configuration of {@link #HTTP_AUTHENTICATION_PASSWORD} property
     * (as shown in the example). This property pair overrides all password settings of the authentication
     * feature for the current request.
     * <p>
     * The default value must be instance of {@link String}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String HTTP_AUTHENTICATION_USERNAME = "jersey.config.client.http.auth.username";
    /**
     * Key of the property that can be set into the {@link javax.ws.rs.client.ClientRequestContext client request}
     * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} in order to override
     * the password for http authentication feature for the request.
     * <p>
     * Example:
     * <pre>
     * Response response = client.target("http://localhost:8080/rest/joe/orders").request()
     *      .property(HTTP_AUTHENTICATION_USERNAME, "joe")
     *      .property(HTTP_AUTHENTICATION_PASSWORD, "p1swd745").get();
     * </pre>
     * </p>
     * The property must be always combined with configuration of {@link #HTTP_AUTHENTICATION_USERNAME} property
     * (as shown in the example). This property pair overrides all password settings of the authentication
     * feature for the current request.
     * <p>
     * The value must be instance of {@link String} or {@code byte} array ({@code byte[]}).
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String HTTP_AUTHENTICATION_PASSWORD = "jersey.config.client.http.auth.password";

    /**
     * Key of the property that can be set into the {@link javax.ws.rs.client.ClientRequestContext client request}
     * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} in order to override
     * the username for http basic authentication feature for the request.
     * <p>
     * Example:
     * <pre>
     * Response response = client.target("http://localhost:8080/rest/joe/orders").request()
     *      .property(HTTP_AUTHENTICATION_BASIC_USERNAME, "joe")
     *      .property(HTTP_AUTHENTICATION_BASIC_PASSWORD, "p1swd745").get();
     * </pre>
     * </p>
     * The property must be always combined with configuration of {@link #HTTP_AUTHENTICATION_PASSWORD} property
     * (as shown in the example). The property pair influence only credentials used during basic authentication.
     *
     * <p>
     * The value must be instance of {@link String}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>

     */
    public static final String HTTP_AUTHENTICATION_BASIC_USERNAME = "jersey.config.client.http.auth.basic.username";

    /**
     * Key of the property that can be set into the {@link javax.ws.rs.client.ClientRequestContext client request}
     * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} in order to override
     * the password for http basic authentication feature for the request.
     * <p>
     * Example:
     * <pre>
     * Response response = client.target("http://localhost:8080/rest/joe/orders").request()
     *      .property(HTTP_AUTHENTICATION_BASIC_USERNAME, "joe")
     *      .property(HTTP_AUTHENTICATION_BASIC_PASSWORD, "p1swd745").get();
     * </pre>
     * </p>
     * The property must be always combined with configuration of {@link #HTTP_AUTHENTICATION_USERNAME} property
     * (as shown in the example). The property pair influence only credentials used during basic authentication.
     * <p>
     * The value must be instance of {@link String} or {@code byte} array ({@code byte[]}).
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String HTTP_AUTHENTICATION_BASIC_PASSWORD = "jersey.config.client.http.auth.basic.password";

    /**
     * Key of the property that can be set into the {@link javax.ws.rs.client.ClientRequestContext client request}
     * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} in order to override
     * the username for http digest authentication feature for the request.
     * <p>
     * Example:
     * <pre>
     * Response response = client.target("http://localhost:8080/rest/joe/orders").request()
     *      .property(HTTP_AUTHENTICATION_DIGEST_USERNAME, "joe")
     *      .property(HTTP_AUTHENTICATION_DIGEST_PASSWORD, "p1swd745").get();
     * </pre>
     * </p>
     * The property must be always combined with configuration of {@link #HTTP_AUTHENTICATION_PASSWORD} property
     * (as shown in the example). The property pair influence only credentials used during digest authentication.
     * <p>
     * The value must be instance of {@link String}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String HTTP_AUTHENTICATION_DIGEST_USERNAME = "jersey.config.client.http.auth.digest.username";

    /**
     * Key of the property that can be set into the {@link javax.ws.rs.client.ClientRequestContext client request}
     * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} in order to override
     * the password for http digest authentication feature for the request.
     * <p>
     * Example:
     * <pre>
     * Response response = client.target("http://localhost:8080/rest/joe/orders").request()
     *      .property(HTTP_AUTHENTICATION_DIGEST_USERNAME, "joe")
     *      .property(HTTP_AUTHENTICATION_DIGEST_PASSWORD, "p1swd745").get();
     * </pre>
     * </p>
     * The property must be always combined with configuration of {@link #HTTP_AUTHENTICATION_PASSWORD} property
     * (as shown in the example). The property pair influence only credentials used during digest authentication.
     * <p>
     * The value must be instance of {@link String} or {@code byte} array ({@code byte[]}).
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String HTTP_AUTHENTICATION_DIGEST_PASSWORD = "jersey.config.client.http.auth.digest.password";

    /**
     * Create the builder of the http authentication feature working in basic authentication mode. The builder
     * can build preemptive and non-preemptive basic authentication features.
     *
     * @return Basic http authentication builder.
     */
    public static BasicBuilder basicBuilder() {
        return new BuilderImpl(Mode.BASIC_PREEMPTIVE);
    }

    /**
     * Create the http authentication feature in basic preemptive authentication mode initialized with credentials.
     *
     * @param username Username.
     * @param password Password as {@code byte array}.
     * @return Http authentication feature configured in basic mode.
     */
    public static HttpAuthenticationFeature basic(String username, byte[] password) {
        return build(Mode.BASIC_PREEMPTIVE, username, password);
    }

    /**
     * Create the http authentication feature in basic preemptive authentication mode initialized with credentials.
     *
     * @param username Username.
     * @param password Password as {@link String}.
     * @return Http authentication feature configured in basic mode.
     */
    public static HttpAuthenticationFeature basic(String username, String password) {
        return build(Mode.BASIC_PREEMPTIVE, username, password);
    }

    /**
     * Create the http authentication feature in digest authentication mode initialized without default
     * credentials. Credentials will have to be supplied using request properties for each request.
     *
     * @return Http authentication feature configured in digest mode.
     */
    public static HttpAuthenticationFeature digest() {
        return build(Mode.DIGEST);
    }

    /**
     * Create the http authentication feature in digest authentication mode initialized with credentials.
     *
     * @param username Username.
     * @param password Password as {@code byte array}.
     * @return Http authentication feature configured in digest mode.
     */
    public static HttpAuthenticationFeature digest(String username, byte[] password) {
        return build(Mode.DIGEST, username, password);
    }

    /**
     * Create the http authentication feature in digest authentication mode initialized with credentials.
     *
     * @param username Username.
     * @param password Password as {@link String}.
     * @return Http authentication feature configured in digest mode.
     */
    public static HttpAuthenticationFeature digest(String username, String password) {
        return build(Mode.DIGEST, username, password);
    }

    /**
     * Create the builder that builds http authentication feature in combined mode supporting both,
     * basic and digest authentication.
     *
     * @return Universal builder.
     */
    public static UniversalBuilder universalBuilder() {
        return new BuilderImpl(Mode.UNIVERSAL);
    }

    /**
     * Create the http authentication feature in combined mode supporting both,
     * basic and digest authentication.
     *
     * @param username Username.
     * @param password Password as {@code byte array}.
     * @return Http authentication feature configured in digest mode.
     */
    public static HttpAuthenticationFeature universal(String username, byte[] password) {
        return build(Mode.UNIVERSAL, username, password);
    }

    /**
     * Create the http authentication feature in combined mode supporting both,
     * basic and digest authentication.
     *
     * @param username Username.
     * @param password Password as {@link String}.
     * @return Http authentication feature configured in digest mode.
     */
    public static HttpAuthenticationFeature universal(String username, String password) {
        return build(Mode.UNIVERSAL, username, password);
    }

    private static HttpAuthenticationFeature build(Mode mode) {
        return new BuilderImpl(mode).build();
    }

    private static HttpAuthenticationFeature build(Mode mode, String username, byte[] password) {
        return new BuilderImpl(mode).credentials(username, password).build();
    }

    private static HttpAuthenticationFeature build(Mode mode, String username, String password) {
        return new BuilderImpl(mode).credentials(username, password).build();
    }

    private final Mode mode;
    private final HttpAuthenticationFilter.Credentials basicCredentials;
    private final HttpAuthenticationFilter.Credentials digestCredentials;

    private HttpAuthenticationFeature(Mode mode, HttpAuthenticationFilter.Credentials basicCredentials,
                                      HttpAuthenticationFilter.Credentials digestCredentials) {
        this.mode = mode;
        this.basicCredentials = basicCredentials;

        this.digestCredentials = digestCredentials;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new HttpAuthenticationFilter(mode, basicCredentials, digestCredentials, context.getConfiguration()));
        return true;
    }
}
