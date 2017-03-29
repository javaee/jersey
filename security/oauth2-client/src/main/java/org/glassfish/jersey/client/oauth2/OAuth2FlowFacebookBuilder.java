/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.oauth2;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.glassfish.jersey.message.internal.ReaderWriter;

/**
 * Class that provides methods to build {@link OAuth2CodeGrantFlow} pre-configured for usage
 * with Facebook provider.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
class OAuth2FlowFacebookBuilder {

    /**
     * Get a builder that can be directly used to perform Authorization Code Grant flow defined by
     * Facebook documentation.
     *
     * @param clientIdentifier Client identifier.
     * @param redirectUri Redirect URI
     * @param client Client instance that should be used to perform Access token request.
     * @return Builder instance.
     */
    public static OAuth2CodeGrantFlow.Builder getFacebookAuthorizationBuilder(ClientIdentifier clientIdentifier,
                                                                              String redirectUri, Client client) {

        final AuthCodeGrantImpl.Builder builder = new AuthCodeGrantImpl.Builder();
        builder.accessTokenUri("https://graph.facebook.com/oauth/access_token");
        builder.authorizationUri("https://www.facebook.com/dialog/oauth");
        builder.redirectUri(redirectUri);
        builder.clientIdentifier(clientIdentifier);
        client.register(FacebookTokenMessageBodyReader.class);
        builder.client(client);
        return builder;
    }


    /**
     * Entity provider that deserializes entity returned from Access Token request into {@link TokenResult}.
     * The format of data is in query param style: "access_token=45a64a654&expires_in=3600".
     */
    @Consumes("text/plain")
    static class FacebookTokenMessageBodyReader implements MessageBodyReader<TokenResult> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.equals(TokenResult.class);
        }

        @Override
        public TokenResult readFrom(Class<TokenResult> type, Type genericType, Annotation[] annotations,
                                    MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                    InputStream entityStream) throws IOException, WebApplicationException {

            Map<String, Object> map = new HashMap<>();
            final String str = ReaderWriter.readFromAsString(entityStream, mediaType);
            final String[] splitArray = str.split("&");
            for (String s : splitArray) {
                final String[] keyValue = s.split("=");
                map.put(keyValue[0], keyValue[1]);
            }

            return new TokenResult(map);
        }
    }


}
