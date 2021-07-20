/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.server.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;

/**
 * Example of a simple blocking point-to-point messaging resource.
 *
 * This version of the messaging resource blocks when POSTing a new message until
 * the message is retrieved.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path(App.ASYNC_MESSAGING_BLOCKING_PATH)
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class BlockingPostChatResource {

    public static final String POST_NOTIFICATION_RESPONSE = "Message stored.";
    //
    private static final Logger LOGGER = Logger.getLogger(BlockingPostChatResource.class.getName());
    private static final Level DEBUG = Level.INFO;
    //
    private static final ExecutorService QUEUE_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("blocking-post-chat-resource-executor-%d")
            .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
            .build());
    private static final BlockingQueue<String> messages = new ArrayBlockingQueue<String>(5);
    //

    @GET
    public void pickUpMessage(@Suspended final AsyncResponse ar, @QueryParam("id") final String messageId) {
        LOGGER.log(DEBUG, "Received GET <{0}> with context {1} on thread {2}.",
                new Object[]{messageId, ar.toString(), Thread.currentThread().getName()});
        QUEUE_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    final String message = messages.take();
                    LOGGER.log(DEBUG, "Resuming GET <{0}> context {1} with a message {2} on thread {3}.",
                            new Object[]{messageId, ar.toString(), message, Thread.currentThread().getName()});
                    ar.resume(message);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE,
                            "Waiting for a message pick-up interrupted. Cancelling context" + ar.toString(), ex);
                    ar.cancel(); // close the open connection
                }
            }
        });
    }

    @POST
    public void postMessage(@Suspended final AsyncResponse ar, final String message) {
        LOGGER.log(DEBUG, "Received POST <{0}> with context {1} on thread {2}. Suspending the context.",
                new Object[]{message, ar.toString(), Thread.currentThread().getName()});
        QUEUE_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    messages.put(message);
                    LOGGER.log(DEBUG, "POSTed message <{0}> successfully queued. Resuming POST with context {1} on thread {2}.",
                            new Object[]{message, ar.toString(), Thread.currentThread().getName()});
                    ar.resume(POST_NOTIFICATION_RESPONSE);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE,
                            "Waiting for a queueing a message '" + message + "' has been interrupted.", ex);
                    ar.resume(ex); // propagate info about the problem
                }
            }
        });
    }
}
