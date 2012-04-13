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
package org.glassfish.jersey.message.internal;


import java.io.InputStream;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.ResponseHeaders;

import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Mutable response message implementation class.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
class MutableResponse extends AbstractMutableMessage<MutableResponse> implements Response, Response.Builder {

    private transient javax.ws.rs.core.Response jaxrsView;
    private transient javax.ws.rs.core.Response.ResponseBuilder jaxrsBuilderView;
    private transient javax.ws.rs.core.ResponseHeaders jaxrsHeadersView;
    private StatusType status;

    /**
     * Creates new instance of response with status {@link Status#NO_CONTENT NO CONTENT}.
     */
    public MutableResponse() {
        this.status = javax.ws.rs.core.Response.Status.NO_CONTENT;
    }

    /**
     * Creates new instance of response initialized from another instance.
     * @param that Instance from which new instance should be initialized.
     */
    public MutableResponse(MutableResponse that) {
        super(that);

        this.status = that.status;
    }

    /**
     * Creates new instance initialized with {@code status} and {@code workers}.
     * @param status Response {@link Status status code}.
     * @param workers {@link MessageBodyWorkers Message body workers} for this response.
     */
    MutableResponse(StatusType status, MessageBodyWorkers workers) {
        this.status = status;
        entityWorkers(workers);
    }

    /**
     * Creates new instance initialized with integer status code and {@code workers}.
     * @param statusCode Response status code number.
     * @param workers {@link MessageBodyWorkers Message body workers} for this response.
     */
    MutableResponse(int statusCode, MessageBodyWorkers workers) {
        this.status = Statuses.from(statusCode);
        entityWorkers(workers);
    }

    /**
     * Creates new instance initialized with integer status code and {@code workers} and {@link InputStream external non-intercepted input stream}.
     * @param statusCode Response status code number.
     * @param workers {@link MessageBodyWorkers Message body workers} for this response.
     * @param externalInputStream {@link InputStream external input stream} "from wire" which needs to be intercepted.
     */
    MutableResponse(int statusCode, MessageBodyWorkers workers, InputStream externalInputStream) {
        this.status = Statuses.from(statusCode);
        entityWorkers(workers);
        entity().rawEntityStream(externalInputStream);
    }

    @Override
    public StatusType status() {
        return status;
    }

    @Override
    public MutableResponse status(StatusType status) {
        this.status = status;
        return this;
    }

    @Override
    public MutableResponse status(int statusCode) {
        this.status = Statuses.from(statusCode);
        return this;
    }

    @Override
    public MutableResponse clone() {
        return new MutableResponse(this);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public javax.ws.rs.core.Response toJaxrsResponse() {
        if (jaxrsView == null) {
            jaxrsView = new JaxrsResponseView(this);
        }

        return jaxrsView;
    }

    @Override
    public javax.ws.rs.core.Response.ResponseBuilder toJaxrsResponseBuilder() {
        if (jaxrsBuilderView == null) {
            jaxrsBuilderView = new JaxrsResponseBuilderView(this);
        }

        return jaxrsBuilderView;
    }

    @Override
    public ResponseHeaders getJaxrsHeaders() {
        if (jaxrsHeadersView == null) {
            jaxrsHeadersView = new JaxrsResponseHeadersView(this);
        }

        return jaxrsHeadersView;
    }

    @Override
    public boolean isEntityRetrievable() {
        return this.isEmpty() || this.type() != null;
    }

    @Override
    public Builder workers(MessageBodyWorkers workers) {
        entityWorkers(workers);
        return this;
    }

    @Override
    public MessageBodyWorkers workers() {
        return entityWorkers();
    }

}
