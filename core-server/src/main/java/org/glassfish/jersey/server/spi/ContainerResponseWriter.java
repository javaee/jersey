/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spi;

import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;

/**
 * A suspendable, request-scoped container response writer.
 *
 * Container sends a new instance of the response writer with every request as part
 * of the call to the Jersey application
 * {@link ApplicationHandler#apply(org.glassfish.jersey.server.ContainerRequest)}  apply(...)}
 * method. Each container response writer represents an open connection to the client
 * (waiting for a response).
 * <p>
 * For each request the Jersey runtime will make sure to directly call either
 * {@link #suspend(long, TimeUnit, TimeoutHandler) suspend(...)}, {@link #cancel()}
 * or {@code commit()} method on a response writer supplied to the
 * {@code JerseyApplication.apply(...)} method before the method has finished. Therefore
 * the container implementations may assume that when the {@code JerseyApplication.apply(...)}
 * method is finished, the container response writer is either {@link #commit() commited},
 * {@link #cancel() canceled} or {@link #suspend(long, TimeUnit, TimeoutHandler) suspended}.
 * </p>
 *
 * @author Marek Potociar
 */
public interface ContainerResponseWriter {

    /**
     * Time-out handler can be registered when the container response writer gets
     * suspended.
     *
     * Should the suspend operation time out, the container is responsible for
     * invoking the {@link TimeoutHandler#onTimeout(ContainerResponseWriter)}
     * callback method to get the response that should be returned to the client.
     */
    public interface TimeoutHandler {

        /**
         * Method is called, when {@link ContainerResponseWriter#suspend(long, TimeUnit,
         * ContainerResponseWriter.TimeoutHandler) ContainerResponseWriter.suspend(...)}
         * operation times out.
         *
         * The custom time-out handler implementation is responsible for making
         * sure a (time-out) response is written to the context and that the
         * container context is properly closed.
         *
         * @param responseWriter suspended container response writer that timed out.
         */
        public void onTimeout(ContainerResponseWriter responseWriter);
    }

    /**
     * Write the status and headers of the response and return an output stream
     * for the web application to write the entity of the response.
     * <p>
     * If the response content length is declared to be greater or equal to 0, it
     * means that the content length in bytes of the entity to be written is known,
     * otherwise -1. Containers may use this value to determine whether the
     * {@code "Content-Length"} header can be set or utilize chunked transfer encoding.
     * </p>
     *
     * @param contentLength greater or equal to 0 if the content length in bytes
     *     of the entity to be written is known, otherwise -1. Containers
     *     may use this value to determine whether the {@code "Content-Length"}
     *     header can be set or utilize chunked transfer encoding.
     * @param responseContext the JAX-RS response to be written. The status and headers
     *     are obtained from the response.
     * @return the output stream to write the entity (if any).
     * @throws ContainerException if an error occurred when writing out the
     *                            status and headers or obtaining the output stream.
     */
    public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext)
            throws ContainerException;

    /**
     * Suspend the request/response processing.
     *
     * Container must not automatically {@link #commit() commit} the response writer
     * when the processing on the container thread is finished and the thread is
     * released. Instead, the Jersey runtime will make sure to manually close
     * the container response writer instance by either calling {@link #commit()}
     * or {@link #cancel()} method.
     * <p />
     * Once suspended, the specified suspend timeout can be further updated using
     * {@link #setSuspendTimeout(long, java.util.concurrent.TimeUnit) } method.
     *
     * @param timeOut        time-out value. Value less or equal to 0, indicates that
     *                       the processing is suspended indefinitely.
     * @param timeUnit       time-out time unit.
     * @param timeoutHandler time-out handler to process a time-out event if it
     *                       occurs.
     * @throws IllegalStateException in case the container response writer has
     *                               already been suspended.
     * @see #setSuspendTimeout(long, TimeUnit)
     * @see #cancel()
     * @see #commit()
     */
    public void suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) throws IllegalStateException;

    /**
     * Set the suspend timeout.
     *
     * Once the container response writer is suspended, the suspend timeout value
     * can be further updated by the method.
     *
     * @param timeOut  time-out value. Value less or equal to 0, indicates that
     *                 the processing is suspended indefinitely.
     * @param timeUnit time-out time unit.
     * @throws IllegalStateException in case the container has not been suspended
     *                               yet.
     * @see #setSuspendTimeout(long, TimeUnit)
     */
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException;

    /**
     * Cancel the request/response processing. This method automatically commits
     * and closes the writer.
     * <p />
     * By invoking this method, {@link org.glassfish.jersey.server.ApplicationHandler
     * Jersey application handler} indicates to the container that the request processing
     * related to this container context has been canceled.
     * <p />
     * Similarly to {@link #commit()}, this enables the container context to release
     * any resources, clean up any state, etc. The main difference is that a call
     * to the {@code cancel()} method indicates that any unsent response data in
     * the container buffer should be discarded.
     *
     * @see #commit()
     * @see #suspend(long, TimeUnit, TimeoutHandler)
     */
    public void cancel();

    /**
     * Commit the response & close the container response writer.
     *
     * Indicates to the container that request has been fully processed and response
     * has been fully written. This signals the container to finish the request/response
     * processing, clean up any state, flush any streams, release resources etc.
     *
     * @see #cancel()
     * @see #suspend(long, TimeUnit, TimeoutHandler)
     */
    public void commit();
}
