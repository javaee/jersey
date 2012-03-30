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
package org.glassfish.jersey.filter;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.PreMatchRequestFilter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RequestFilter;
import javax.ws.rs.ext.ResponseFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Universal logging filter.
 *
 * Can be used on client or server side. Has the highest priority.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Provider
@BindingPriority(Integer.MIN_VALUE)
@SuppressWarnings("ClassWithMultipleLoggers")
public class LoggingFilter implements PreMatchRequestFilter, RequestFilter, ResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());
    private static final String NOTIFICATION_PREFIX = "* ";
    private static final String REQUEST_PREFIX = "> ";
    private static final String RESPONSE_PREFIX = "< ";
    //
    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;
    private final AtomicLong _id = new AtomicLong(0);
    private boolean printEntity = true;

    /**
     * Create a logging filter logging the request and response to a default JDK
     * logger, named as the fully qualified class name of this class. Entity
     * logging is turned on.
     */
    public LoggingFilter() {
        this(LOGGER, false);
    }

    /**
     * Create a logging filter with custom logger and custom settings of entity
     * logging.
     *
     * @param logger the logger to log requests and responses.
     * @param printEntity if true, entity will be logged as well.
     */
    public LoggingFilter(Logger logger, boolean printEntity) {
        this.logger = logger;
        this.printEntity = printEntity;
    }

    @Override
    public void preMatchFilter(FilterContext context) throws IOException {
        long id = this._id.incrementAndGet();
        context.setRequest(logRequest(id, context.getRequest()));
    }

    @Override
    public void postFilter(FilterContext context) throws IOException {
        long id = this._id.incrementAndGet();
        context.setResponse(logResponse(id, context.getResponse()));
    }

    @Override
    public void preFilter(FilterContext context) throws IOException {
        long id = this._id.incrementAndGet();
        context.setRequest(logRequest(id, context.getRequest()));
    }

    private void log(StringBuilder b) {
        if (logger != null) {
            logger.info(b.toString());
        }
    }

    private Request logRequest(long id, Request request) throws IOException {
        StringBuilder b = new StringBuilder();

        printRequestLine(b, id, request);
        printPrefixedHeaders(b, id, REQUEST_PREFIX, request.getHeaders().asMap());

        // TODO define large entities logging threshold via configuration
        //      or add special handling for entity streams
        if (printEntity && request.hasEntity()) {
            request.bufferEntity();
            b.append(request.readEntity(String.class)).append("\n");
        }

        log(b);

        return request;
    }

    private Response logResponse(long id, Response response) throws IOException {
        StringBuilder b = new StringBuilder();

        printResponseLine(b, id, response);
        printPrefixedHeaders(b, id, RESPONSE_PREFIX, response.getHeaders().asMap());

        // TODO define large entities logging threshold via configuration
        //      or add special handling for entity streams
        if (printEntity && response.hasEntity()) {
            response.bufferEntity();
            b.append(response.readEntity(String.class)).append("\n");
        }

        log(b);

        return response;
    }

    private StringBuilder prefixId(StringBuilder b, long id) {
        b.append(Long.toString(id)).append(" ");
        return b;
    }

    private void printRequestLine(StringBuilder b, long id, Request request) {
        prefixId(b, id).append(NOTIFICATION_PREFIX).append("LoggingFilter - Request received on thread ").append(Thread.currentThread().getName()).append("\n");
        prefixId(b, id).append(REQUEST_PREFIX).append(request.getMethod()).append(" ").
                append(request.getUri().toASCIIString()).append("\n");
    }

    private void printResponseLine(StringBuilder b, long id, Response response) {
        prefixId(b, id).append(NOTIFICATION_PREFIX).
                append("LoggingFilter - Response received on thread ").append(Thread.currentThread().getName()).append("\n");
        prefixId(b, id).append(RESPONSE_PREFIX).
                append(Integer.toString(response.getStatus())).
                append("\n");
    }

    private void printPrefixedHeaders(StringBuilder b, long id, final String prefix, Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            List<String> val = e.getValue();
            String header = e.getKey();

            if (val.size() == 1) {
                prefixId(b, id).append(prefix).append(header).append(": ").append(val.get(0)).append("\n");
            } else {
                StringBuilder sb = new StringBuilder();
                boolean add = false;
                for (String s : val) {
                    if (add) {
                        sb.append(',');
                    }
                    add = true;
                    sb.append(s);
                }
                prefixId(b, id).append(prefix).append(header).append(": ").append(sb.toString()).append("\n");
            }
        }
    }
}
