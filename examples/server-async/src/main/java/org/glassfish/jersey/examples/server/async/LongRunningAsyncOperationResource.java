/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.server.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.concurrent.TimeUnit.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Suspend;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.ExecutionContext;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Example resource for long running async operations.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
//TODO move the test to integration tests.
//@Path(App.LONG_RUNNING_ASYNC_OP_PATH)
@Produces("text/plain")
public class LongRunningAsyncOperationResource {

    public static final String NOTIFICATION_RESPONSE = "Hello async world!";
    //
    private static final Logger LOGGER = Logger.getLogger(LongRunningAsyncOperationResource.class.getName());
    private static final int SLEEP_TIME_IN_MILLIS = 1000;
    private static final ExecutorService TASK_EXECUTOR = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("long-running-resource-executor-%d").build());
    @Context
    private ExecutionContext ctx;

    @GET
    @Path("basicSyncExample")
    public String basicSyncExample() {
        try {
            Thread.sleep(SLEEP_TIME_IN_MILLIS);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Response processing interrupted", ex);
        }
        return NOTIFICATION_RESPONSE;
    }

    @GET
    @Suspend(timeOut = 15, timeUnit = SECONDS)
    @Path("suspendViaAnnotation")
    public void suspendViaAnnotationExample() {
        TASK_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "Response processing interrupted", ex);
                }
                ctx.resume(NOTIFICATION_RESPONSE);
            }
        });

        // default suspend;
    }

    @GET
    @Suspend(timeOut = 15, timeUnit = SECONDS)
    @Path("suspendViaAnnotation2")
    public void suspendViaAnnotationExample2(@Context final ExecutionContext ctx2) {
        TASK_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "Response processing interrupted", ex);
                }
                ctx2.resume(NOTIFICATION_RESPONSE);
            }
        });

        // default suspend;
    }

    @GET
    @Path("suspendViaContext")
    public String suspendViaContextExample(@QueryParam("query") final String query) {
        if (!isComplex(query)) {
            return "Simple result for " + query; // process simple queries synchronously
        }

        ctx.suspend(); // programmatic suspend
        TASK_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "Response processing interrupted", ex);
                }
                ctx.resume("Complex result for " + query);
            }
        });

        return null; // return value ignored for suspended requests
    }

    private boolean isComplex(String query) {
        return "complex".equalsIgnoreCase(query);
    }

    @GET
    @Path("timeoutPropagated")
    @Suspend(timeOut = 15000) // default time unit is milliseconds
    public void timeoutValueConflict_PropagationExample() {
        TASK_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "Response processing interrupted", ex);
                }
                ctx.resume(NOTIFICATION_RESPONSE);
            }
        });
        ctx.suspend(); // time-out values propagated from the @Suspend annotation; the call is redundant
    }

    @GET
    @Path("timeoutOverriden")
    @Suspend(timeOut = 15000) // default time unit is milliseconds
    public void timeoutValueConflict_OverridingExample(
            @QueryParam("timeOut") Long timeOut, @QueryParam("timeUnit") TimeUnit timeUnit) {
        TASK_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "Response processing interrupted", ex);
                }
                ctx.resume(NOTIFICATION_RESPONSE);
            }
        });
        if (timeOut != null && timeUnit != null) {
            ctx.suspend(timeOut, timeUnit); // time-out values specified in the @Suspend annotation are overriden
        } else {
            // suspend using annotation values
        }
    }

    @GET
    @Path("suspendHandleUsage")
    public void suspendHandleUsageExample() {
        TASK_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "Response processing interrupted", ex);
                }
                ctx.resume(NOTIFICATION_RESPONSE);
            }
        });

        ctx.suspend(); // retrieving a handle to monitor the suspended request state

        TASK_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                while (!ctx.isDone()) {
                }
                LOGGER.log(Level.INFO, "Context resumed with a response!");
            }
        });
    }
}
