/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.moxy.json.MoxyJsonFeature;

/**
 * Fake message aggregator used for testing purposes.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public abstract class AbstractTestAggregator implements DataAggregator {
    private static final Logger LOGGER = Logger.getLogger(AbstractTestAggregator.class.getName());
    private static final String[] MESSAGES = new String[] {
            "Where do your RESTful Web Services want to go today?",
            "Jersey RESTful Web Services framework rocks!",
            "Jersey and JAX-RS are cool!",
            "What are the 5 insane but true things about JAX-RS?",
            "Wow, JAX-RS 2.0 provides asynchronous service and client APIs!",
            "Finally! JAX-RS 2.0 adds filters and interceptors support.",
            "Jersey 2.0 programmatic resource API looks great!",
            "How could I live without Jersey ResourceConfig class??",
            "Just wrote my first JAX-RS service using Jersey.",
            "Jersey is the best RESTful framework ever.",
            "JAX-RS rules the web services.",
            "Jersey 2.0 is the new American idol!"
    };
    private static final String IMG_URI
            = "http://files.softicons.com/download/internet-cons/halloween-avatars-icons-by-deleket/png/48/Voodoo%20Doll.png";

    private final String rgbColor;
    private volatile boolean running;

    AbstractTestAggregator(String rgbColor) {
        this.rgbColor = rgbColor;
    }

    @Override
    public void start(final String keywords, final DataListener msgListener) {
        msgListener.onStart();
        running = true;

        final Random rnd = new Random();
        final String aggregatorPrefix = getPrefix();

        Executors.newSingleThreadExecutor().submit(() -> {
            final Client resourceClient = ClientBuilder.newClient();
            resourceClient.register(new MoxyJsonFeature());
            final WebTarget messageStreamResource = resourceClient.target(App.getApiUri()).path(getPath());

            try {
                while (running) {
                    final Message message = new Message(
                            aggregatorPrefix + " " + MESSAGES[rnd.nextInt(MESSAGES.length)],
                            rgbColor,
                            IMG_URI);
                    msgListener.onMessage(message);
                    final Response r = messageStreamResource.request().put(Entity.json(message));
                    if (r.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                        LOGGER.warning("Unexpected PUT message response status code: " + r.getStatus());
                    }
                    Thread.sleep(rnd.nextInt(1000) + 750);
                }
                msgListener.onComplete();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Waiting for a message has been interrupted.", t);
                msgListener.onError();
            }
        });
    }


    @Override
    public void stop() {
        running = false;
    }

    /**
     * Get relative path to the event stream.
     */
    protected abstract String getPath();

    /**
     * Get message prefix to identify the concrete aggregator.
     *
     * @return message prefix (aggregator qualifier)
     */
    protected abstract String getPrefix();

}
