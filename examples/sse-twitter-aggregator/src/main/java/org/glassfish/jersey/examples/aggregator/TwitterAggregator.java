/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.aggregator;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;

/**
 * Twitter message-based data aggregator implementation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class TwitterAggregator implements DataAggregator {
    private static final Logger LOGGER = Logger.getLogger(TwitterAggregator.class.getName());

    private volatile boolean cancelled;
    private final String rgbColor;

    /**
     * Create new twitter message aggregator with a specific message color.
     *
     * @param rgbColor message color.
     */
    public TwitterAggregator(String rgbColor) {
        this.rgbColor = rgbColor;
    }

    @Override
    public void start(final String keywords, final DataListener msgListener) {
        cancelled = false;

//        System.setProperty("http.proxyHost", "www-proxy.us.oracle.com");
//        System.setProperty("http.proxyPort", "80");
//        System.setProperty("https.proxyHost", "www-proxy.us.oracle.com");
//        System.setProperty("https.proxyPort", "80");


        final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<Message>();

        final Future<?> readerHandle = Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                SslConfigurator sslConfig = SslConfigurator.newInstance()
                        .trustStoreFile("./truststore_client")
                        .trustStorePassword("asdfgh")

                        .keyStoreFile("./keystore_client")
                        .keyPassword("asdfgh");

                final Client client = ClientBuilder.newBuilder().sslContext(sslConfig.createSSLContext()).build();
                client.property(ClientProperties.CONNECT_TIMEOUT, 2000)
                        .register(new MoxyJsonFeature())
                        .register(HttpAuthenticationFeature.basic(App.getTwitterUserName(), App.getTwitterUserPassword()))
                        .register(GZipEncoder.class);

                final Response response = client.target("https://stream.twitter.com/1.1/statuses/filter.json")
                        .queryParam("track", keywords)
//                .queryParam("locations", "-122.75,36.8,-121.75,37.8") // San Francisco
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .header(HttpHeaders.HOST, "stream.twitter.com")
                        .header(HttpHeaders.USER_AGENT, "Jersey/2.0")
                        .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                        .get();

                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    LOGGER.log(Level.WARNING, "Error connecting to Twitter Streaming API: " + response.getStatus());
                    msgListener.onError();
                    return;
                }
                msgListener.onStart();

                try {
                    final ChunkedInput<Message> chunks = response.readEntity(new GenericType<ChunkedInput<Message>>() {
                    });
                    try {
                        while (!Thread.interrupted()) {
                            Message message = chunks.read();
                            if (message == null) {
                                break;
                            }
                            try {
                                message.setRgbColor(rgbColor);
                                System.out.println(message.toString());
                                messages.put(message);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    } finally {
                        if (chunks != null) {
                            chunks.close();
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, "Reading from the Twitter stream has failed", t);
                    messages.offer(null);
                    msgListener.onError();
                }
            }
        });

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                final Client resourceClient = ClientBuilder.newClient();
                resourceClient.register(new MoxyJsonFeature());
                final WebTarget messageStreamResource = resourceClient.target(App.getApiUri()).path("message/stream");

                Message message = null;
                try {
                    while (!cancelled && (message = messages.take()) != null) {
                        msgListener.onMessage(message);

                        final Response r = messageStreamResource.request().put(Entity.json(message));
                        if (r.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                            LOGGER.warning("Unexpected PUT message response status code: " + r.getStatus());
                        }
                    }

                    if (message == null) {
                        LOGGER.info("Timed out while waiting for a message.");
                    }
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.WARNING, "Waiting for a message has been interrupted.", ex);
                } finally {
                    readerHandle.cancel(true);
                    msgListener.onComplete();
                }
            }
        });
    }

    @Override
    public void stop() {
        cancelled = true;
    }
}
