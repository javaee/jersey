/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.oauth.twitterclient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.oauth1.AccessToken;
import org.glassfish.jersey.client.oauth1.ConsumerCredentials;
import org.glassfish.jersey.client.oauth1.OAuth1AuthorizationFlow;
import org.glassfish.jersey.client.oauth1.OAuth1ClientSupport;
import org.glassfish.jersey.jackson.JacksonFeature;

/** Simple command-line application that uses Jersey OAuth client library to authenticate
 * with Twitter.
 *
 * @author Martin Matula
 * @author Miroslav Fuksa
 */
public class App {
    private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
    private static final String FRIENDS_TIMELINE_URI = "https://api.twitter.com/1.1/statuses/home_timeline.json";
    private static final Properties PROPERTIES = new Properties();
    private static final String PROPERTIES_FILE_NAME = "twitterclient.properties";
    private static final String PROPERTY_CONSUMER_KEY = "consumerKey";
    private static final String PROPERTY_CONSUMER_SECRET = "consumerSecret";
    private static final String PROPERTY_TOKEN = "token";
    private static final String PROPERTY_TOKEN_SECRET = "tokenSecret";

    /**
     * Main method that creates a {@link Client client} and initializes the OAuth support with
     * configuration needed to connect to the Twitter and retrieve statuses.
     * <p/>
     * Execute this method to demo
     *
     * @param args Command line arguments.
     * @throws Exception Thrown when error occurs.
     */
    public static void main(final String[] args) throws Exception {
        // retrieve consumer key/secret and token/secret from the property file
        // or system properties
        loadSettings();

        final ConsumerCredentials consumerCredentials = new ConsumerCredentials(
                PROPERTIES.getProperty(PROPERTY_CONSUMER_KEY),
                PROPERTIES.getProperty(PROPERTY_CONSUMER_SECRET));

        final Feature filterFeature;
        if (PROPERTIES.getProperty(PROPERTY_TOKEN) == null) {

            // we do not have Access Token yet. Let's perfom the Authorization Flow first,
            // let the user approve our app and get Access Token.
            final OAuth1AuthorizationFlow authFlow = OAuth1ClientSupport.builder(consumerCredentials)
                    .authorizationFlow(
                            "https://api.twitter.com/oauth/request_token",
                            "https://api.twitter.com/oauth/access_token",
                            "https://api.twitter.com/oauth/authorize")
                    .build();
            final String authorizationUri = authFlow.start();

            System.out.println("Enter the following URI into a web browser and authorize me:");
            System.out.println(authorizationUri);
            System.out.print("Enter the authorization code: ");
            final String verifier;
            try {
                verifier = IN.readLine();
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
            final AccessToken accessToken = authFlow.finish(verifier);

            // store access token for next application execution
            PROPERTIES.setProperty(PROPERTY_TOKEN, accessToken.getToken());
            PROPERTIES.setProperty(PROPERTY_TOKEN_SECRET, accessToken.getAccessTokenSecret());

            // get the feature that will configure the client with consumer credentials and
            // received access token
            filterFeature = authFlow.getOAuth1Feature();
        } else {
            final AccessToken storedToken = new AccessToken(PROPERTIES.getProperty(PROPERTY_TOKEN),
                    PROPERTIES.getProperty(PROPERTY_TOKEN_SECRET));
            // build a new feature from the stored consumer credentials and access token
            filterFeature = OAuth1ClientSupport.builder(consumerCredentials).feature()
                    .accessToken(storedToken).build();
        }


        // create a new Jersey client and register filter feature that will add OAuth signatures and
        // JacksonFeature that will process returned JSON data.
        final Client client = ClientBuilder.newBuilder()
                .register(filterFeature)
                .register(JacksonFeature.class)
                .build();

        // make requests to protected resources
        // (no need to care about the OAuth signatures)
        final Response response = client.target(FRIENDS_TIMELINE_URI).request().get();
        if (response.getStatus() != 200) {
            String errorEntity = null;
            if (response.hasEntity()) {
                errorEntity = response.readEntity(String.class);
            }
            throw new RuntimeException("Request to Twitter was not successful. Response code: "
                    + response.getStatus() + ", reason: " + response.getStatusInfo().getReasonPhrase()
                    + ", entity: " + errorEntity);
        }

        // persist the current consumer key/secret and token/secret for future use
        storeSettings();

        final List<Status> statuses = response.readEntity(new GenericType<List<Status>>() {
        });

        System.out.println("Tweets:\n");
        for (final Status s : statuses) {
            System.out.println(s.getText());
            System.out.println("[posted by " + s.getUser().getName() + " at " + s.getCreatedAt() + "]");
        }


    }

    private static void loadSettings() {
        FileInputStream st = null;
        try {
            st = new FileInputStream(PROPERTIES_FILE_NAME);
            PROPERTIES.load(st);
        } catch (final IOException e) {
            // ignore
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (final IOException ex) {
                    // ignore
                }
            }
        }

        for (final String name : new String[]{PROPERTY_CONSUMER_KEY, PROPERTY_CONSUMER_SECRET,
                PROPERTY_TOKEN, PROPERTY_TOKEN_SECRET}) {
            final String value = System.getProperty(name);
            if (value != null) {
                PROPERTIES.setProperty(name, value);
            }
        }

        if (PROPERTIES.getProperty(PROPERTY_CONSUMER_KEY) == null
                || PROPERTIES.getProperty(PROPERTY_CONSUMER_SECRET) == null) {
            System.out.println("No consumerKey and/or consumerSecret found in twitterclient.properties file. "
                    + "You have to provide these as system properties. See README.html for more information.");
            System.exit(1);
        }
    }

    private static void storeSettings() {
        FileOutputStream st = null;
        try {
            st = new FileOutputStream(PROPERTIES_FILE_NAME);
            PROPERTIES.store(st, null);
        } catch (final IOException e) {
            // ignore
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
            } catch (final IOException ex) {
                // ignore
            }
        }
    }

}
