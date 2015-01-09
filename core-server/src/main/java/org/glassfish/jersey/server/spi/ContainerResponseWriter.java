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
package org.glassfish.jersey.server.spi;

import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;

/**
 * A suspendable, request-scoped I/O container response writer.
 *
 * I/O container sends a new instance of the response writer with every request as part
 * of the call to the Jersey application
 * {@link ApplicationHandler#apply(org.glassfish.jersey.server.ContainerRequest)}  apply(...)}
 * method. Each container response writer represents an open connection to the client
 * (waiting for a response).
 * <p>
 * For each request the Jersey runtime will make sure to directly call either
 * {@link #suspend(long, TimeUnit, TimeoutHandler) suspend(...)}
 * or {@code commit()} method on a response writer supplied to the
 * {@code JerseyApplication.apply(...)} method before the method has finished. Therefore
 * the I/O container implementations may assume that when the {@code JerseyApplication.apply(...)}
 * method is finished, the container response writer is either {@link #commit() commited},
 * or {@link #suspend(long, TimeUnit, TimeoutHandler) suspended}.
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
     * otherwise -1. I/O containers may use this value to determine whether the
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
     * The method returns {@code true} to indicate the response writer was suspended successfully.
     * In case the provider has already been suspended earlier, the method returns {@code false}.
     * <p>
     * I/O container must not automatically {@link #commit() commit} the response writer
     * when the processing on the I/O container thread is finished and the thread is
     * released. Instead, the Jersey runtime will make sure to manually close
     * the container response writer instance by explicitly calling the {@link #commit()}
     * or {@link #failure(Throwable)} method at some later point in time.
     * </p>
     * <p>
     * Once suspended, the specified suspend timeout can be further updated using
     * {@link #setSuspendTimeout(long, java.util.concurrent.TimeUnit) } method.
     * </p>
     *
     * @param timeOut        time-out value. Value less or equal to 0, indicates that
     *                       the processing is suspended indefinitely.
     * @param timeUnit       time-out time unit.
     * @param timeoutHandler time-out handler to process a time-out event if it
     *                       occurs.
     * @return {@code true} if the suspend operation completed successfully, {@code false} otherwise.
     *
     * @see #setSuspendTimeout(long, TimeUnit)
     * @see #commit()
     */
    public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler);

    /**
     * Set the suspend timeout.
     *
     * Once the container response writer is suspended, the suspend timeout value
     * can be further updated by the method.
     *
     * @param timeOut  time-out value. Value less or equal to 0, indicates that
     *                 the processing is suspended indefinitely.
     * @param timeUnit time-out time unit.
     * @throws IllegalStateException in case the response writer has not been suspended
     *                               yet.
     */
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException;

    /**
     * Commit the response & close the container response writer.
     *
     * Indicates to the I/O container that request has been fully processed and response
     * has been fully written. This signals the I/O  container to finish the request/response
     * processing, clean up any state, flush any streams, release resources etc.
     *
     * @see #suspend(long, TimeUnit, TimeoutHandler)
     * @see #failure(Throwable)
     */
    public void commit();

    /**
     * Propagate an unhandled error to the I/O container.
     *
     * Indicates to the I/O container that the request processing has finished with an error
     * that could not be processed by the Jersey runtime. The I/O container is expected to process
     * the exception in a container-specific way. This method also signals the I/O container to
     * finish the request/response processing, clean up any state, flush any streams, release
     * resources etc.
     *
     * @param error unhandled request processing error.
     *
     * @see #suspend(long, TimeUnit, TimeoutHandler)
     * @see #commit()
     */
    public void failure(Throwable error);

    /**
     * Return {@code true} if the entity buffering should be enabled in Jersey.
     *
     * If enabled, the outbound entity is buffered by Jersey runtime up to a configured amount of bytes
     * prior to being written to the output stream to determine its size that may be used to set the value
     * of HTTP <tt>{@value javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH}</tt> header.
     * <p>
     * Containers that provide it's own solution for determining the message payload size may decide to
     * return {@code false} to prevent Jersey from buffering message entities unnecessarily.
     * </p>
     *
     * @return {@code true} to enable entity buffering to be done by Jersey runtime, {@code false} otherwise.
     */
    public boolean enableResponseBuffering();
}
