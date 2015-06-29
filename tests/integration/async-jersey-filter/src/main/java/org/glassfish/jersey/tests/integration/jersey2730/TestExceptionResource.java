/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey2730;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import javax.inject.Singleton;

import org.glassfish.jersey.servlet.internal.ResponseWriter;
import org.glassfish.jersey.tests.integration.jersey2730.exception.MappedException;
import org.glassfish.jersey.tests.integration.jersey2730.exception.UnmappedException;
import org.glassfish.jersey.tests.integration.jersey2730.exception.UnmappedRuntimeException;

/**
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
@Path("/exception")
@Singleton
public class TestExceptionResource {

    /**
     * An instance of thread that was processing a last request to this resource.
     */
    private Thread lastProcessingThread;

    @GET
    @Path("null")
    public void get(@Suspended final AsyncResponse asyncResponse) {
        lastProcessingThread = Thread.currentThread();
        asyncResponse.resume((Throwable) null);
    }

    @GET
    @Path("mapped")
    public void getMappedException(@Suspended final AsyncResponse asyncResponse) {
        lastProcessingThread = Thread.currentThread();
        asyncResponse.resume(new MappedException());
    }

    @GET
    @Path("unmapped")
    public void getUnmappedException(@Suspended final AsyncResponse asyncResponse) {
        lastProcessingThread = Thread.currentThread();
        asyncResponse.resume(new UnmappedException());
    }

    @GET
    @Path("runtime")
    public void getUnmappedRuntimeException(@Suspended final AsyncResponse asyncResponse) {
        lastProcessingThread = Thread.currentThread();
        asyncResponse.resume(new UnmappedRuntimeException());
    }

    /**
     * Returns whether a thread that was processing a last request got stuck in {@link ResponseWriter}.
     * <p/>
     * Under normal circumstances, the last processing thread should return back to the servlet container
     * and its pool.
     * <p/>
     * May not work when executed in parallel.
     *
     * @return
     */
    @GET
    @Path("rpc/lastthreadstuck")
    public boolean lastThreadStuckRpc() {
        if (lastProcessingThread == null || Thread.currentThread() == lastProcessingThread) {
            return false;
        }

        switch (lastProcessingThread.getState()) {
            case BLOCKED:
            case TIMED_WAITING:
            case WAITING:
                for (StackTraceElement stackTraceElement : lastProcessingThread.getStackTrace()) {
                    if (ResponseWriter.class.getName().equals(stackTraceElement.getClassName())) {
                        return true;
                    }
                }
        }

        return false;
    }
}
