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

package org.glassfish.jersey.examples.server.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

/**
 * Example of a simple resource with a long-running operation executed in a
 * custom application thread.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path("long-running")
@Produces("text/plain")
public class LongRunningEchoResource {

    private static final int SLEEP_TIME_IN_MILLIS = 1000;
    private static final ExecutorService TASK_EXECUTOR = Executors.newCachedThreadPool();

    /**
     * Synchronously echo the last path segment after sleeping for a long time.
     *
     * @param echo message to echo.
     * @return echoed message.
     */
    @GET
    @Path("sync/{echo}")
    public String syncEcho(@PathParam("echo") final String echo) {
        try {
            Thread.sleep(SLEEP_TIME_IN_MILLIS);
        } catch (final InterruptedException ex) {
            throw new ServiceUnavailableException();
        }
        return echo;
    }

    /**
     * Asynchronously echo the last path segment after sleeping for a long time.
     *
     * @param echo message to echo.
     * @param ar AsynchronousResponse, will contain echoed message.
     */
    @GET
    @Path("async/{echo}")
    public void asyncEcho(@PathParam("echo") final String echo, @Suspended final AsyncResponse ar) {
        TASK_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);
                } catch (final InterruptedException ex) {
                    ar.cancel();
                }
                ar.resume(echo);
            }
        });
    }
}
