/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.oauth1;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureException;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.oauth1.internal.OAuthServerRequest;

/**
 * OAuth request filter that filters all requests indicating in the Authorization
 * header they use OAuth. Checks if the incoming requests are properly authenticated
 * and populates the security context with the corresponding user principal and roles.
 * <p>
 *
 * @author Paul C. Bryan <pbryan@sun.com>
 * @author Martin Matula
 */
@Priority(Priorities.AUTHENTICATION)
class OAuth1ServerFilter implements ContainerRequestFilter {

    /** OAuth Server */
    @Inject
    private OAuth1Provider provider;

    /** Manages and validates incoming nonces. */
    private final NonceManager nonces;

    /** Value to return in www-authenticate header when 401 response returned. */
    private final String wwwAuthenticateHeader;

    /** OAuth protocol versions that are supported. */
    private final Set<String> versions;

    /** Regular expression pattern for path to ignore. */
    private final Pattern ignorePathPattern;

    @Inject
    private OAuth1Signature oAuth1Signature;

    @Inject
    private Provider<ExtendedUriInfo> uriInfo;

    private final boolean optional;

    /**
     * Create a new filter.
     * @param rc Resource config.
     */
    @Inject
    public OAuth1ServerFilter(Configuration rc) {
        // establish supported OAuth protocol versions
        HashSet<String> v = new HashSet<String>();
        v.add(null);
        v.add("1.0");
        versions = Collections.unmodifiableSet(v);

        // optional initialization parameters (defaulted)
        String realm = OAuth1ServerProperties.getValue(rc.getProperties(), OAuth1ServerProperties.REALM, "default", String.class);
        /* Maximum age (in milliseconds) of timestamp to accept in incoming messages. */
        int maxAge = OAuth1ServerProperties.getValue(rc.getProperties(), OAuth1ServerProperties.MAX_AGE, 300000);
        /* Average requests to process between nonce garbage collection passes. */
        int gcPeriod = OAuth1ServerProperties.getValue(rc.getProperties(), OAuth1ServerProperties.GC_PERIOD, 100);
        ignorePathPattern = pattern(
                OAuth1ServerProperties.getValue(rc.getProperties(), OAuth1ServerProperties.IGNORE_PATH_PATTERN,
                        null, String.class)); // no pattern
        optional = PropertiesHelper.isProperty(rc.getProperties(), OAuth1ServerProperties.NO_FAIL);

        final String timeUnitStr = OAuth1ServerProperties.getValue(rc.getProperties(), OAuth1ServerProperties.TIMESTAMP_UNIT,
                String.class);
        final TimeUnit timeUnit = timeUnitStr != null ? TimeUnit.valueOf(timeUnitStr) : TimeUnit.SECONDS;

        final int maxCacheSize = OAuth1ServerProperties.getValue(rc.getProperties(), OAuth1ServerProperties.MAX_NONCE_CACHE_SIZE,
                2000000);

        nonces = new NonceManager(maxAge, gcPeriod, timeUnit, maxCacheSize);

        // www-authenticate header for the life of the object
        wwwAuthenticateHeader = "OAuth realm=\"" + realm + "\"";
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        // do not filter requests that do not use OAuth authentication
        String authHeader = request.getHeaderString(OAuth1Parameters.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.toUpperCase().startsWith(OAuth1Parameters.SCHEME.toUpperCase())) {
            return;
        }

        // do not filter requests that matches to access or token resources
        final Method handlingMethod = uriInfo.get().getMatchedResourceMethod().getInvocable().getHandlingMethod();
        if (handlingMethod.isAnnotationPresent(TokenResource.class)
                || handlingMethod.getDeclaringClass().isAnnotationPresent(TokenResource.class)) {
            return;
        }

        // do not filter if the request path matches pattern to ignore
        if (match(ignorePathPattern, request.getUriInfo().getPath())) {
            return;
        }

        OAuth1SecurityContext sc;
        try {
            sc = getSecurityContext(request);
        } catch (OAuth1Exception e) {
            if (optional) {
                return;
            } else {
                throw e;
            }
        }

        request.setSecurityContext(sc);
    }

    private OAuth1SecurityContext getSecurityContext(ContainerRequestContext request) throws OAuth1Exception {
        OAuthServerRequest osr = new OAuthServerRequest(request);
        OAuth1Parameters params = new OAuth1Parameters().readRequest(osr);

        // apparently not signed with any OAuth parameters; unauthorized
        if (params.size() == 0) {
            throw newUnauthorizedException();
        }

        // get required OAuth parameters
        String consumerKey = requiredOAuthParam(params.getConsumerKey());
        String token = params.getToken();
        String timestamp = requiredOAuthParam(params.getTimestamp());
        String nonce = requiredOAuthParam(params.getNonce());

        // enforce other supported and required OAuth parameters
        requiredOAuthParam(params.getSignature());
        supportedOAuthParam(params.getVersion(), versions);

        // retrieve secret for consumer key
        OAuth1Consumer consumer = provider.getConsumer(consumerKey);
        if (consumer == null) {
            throw newUnauthorizedException();
        }

        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret(consumer.getSecret());
        OAuth1SecurityContext sc;
        String nonceKey;

        if (token == null) {
            if (consumer.getPrincipal() == null) {
                throw newUnauthorizedException();
            }
            nonceKey = "c:" + consumerKey;
            sc = new OAuth1SecurityContext(consumer, request.getSecurityContext().isSecure());
        } else {
            OAuth1Token accessToken = provider.getAccessToken(token);
            if (accessToken == null) {
                throw newUnauthorizedException();
            }

            OAuth1Consumer atConsumer = accessToken.getConsumer();
            if (atConsumer == null || !consumerKey.equals(atConsumer.getKey())) {
                throw newUnauthorizedException();
            }

            nonceKey = "t:" + token;
            secrets.tokenSecret(accessToken.getSecret());
            sc = new OAuth1SecurityContext(accessToken, request.getSecurityContext().isSecure());
        }

        if (!verifySignature(osr, params, secrets)) {
            throw newUnauthorizedException();
        }

        if (!nonces.verify(nonceKey, timestamp, nonce)) {
            throw newUnauthorizedException();
        }

        return sc;
    }

    private static String requiredOAuthParam(String value) throws OAuth1Exception {
        if (value == null) {
            throw newBadRequestException();
        }
        return value;
    }

    private static String supportedOAuthParam(String value, Set<String> set) throws OAuth1Exception {
        if (!set.contains(value)) {
            throw newBadRequestException();
        }
        return value;
    }

    private static Pattern pattern(String p) {
        if (p == null) {
            return null;
        }
        return Pattern.compile(p);
    }

    private static boolean match(Pattern pattern, String value) {
        return (pattern != null && value != null && pattern.matcher(value).matches());
    }

    private boolean verifySignature(OAuthServerRequest osr, OAuth1Parameters params, OAuth1Secrets secrets) {
        try {
            return oAuth1Signature.verify(osr, params, secrets);
        } catch (OAuth1SignatureException ose) {
            throw newBadRequestException();
        }
    }

    private static OAuth1Exception newBadRequestException() throws OAuth1Exception {
        return new OAuth1Exception(Response.Status.BAD_REQUEST, null);
    }

    private OAuth1Exception newUnauthorizedException() throws OAuth1Exception {
        return new OAuth1Exception(Response.Status.UNAUTHORIZED, wwwAuthenticateHeader);
    }

}
