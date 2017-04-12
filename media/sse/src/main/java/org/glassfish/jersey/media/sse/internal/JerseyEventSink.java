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

package org.glassfish.jersey.media.sse.internal;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.glassfish.jersey.internal.jsr166.Flow;
import org.glassfish.jersey.media.sse.LocalizationMessages;
import org.glassfish.jersey.server.ChunkedOutput;

/**
 * Server-side SSE subscriber.
 * <p>
 * The reference should be obtained via injection into the resource method.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)]
 */
class JerseyEventSink extends ChunkedOutput<OutboundSseEvent> implements SseEventSink, Flow.Subscriber<OutboundSseEvent> {
    private static final Logger LOGGER = Logger.getLogger(JerseyEventSink.class.getName());
    private static final byte[] SSE_EVENT_DELIMITER = "\n".getBytes(Charset.forName("UTF-8"));
    private Flow.Subscription subscription = null;

    JerseyEventSink() {
        super(SSE_EVENT_DELIMITER);
    }

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        checkClosed();
        if (subscription == null) {
            throw new NullPointerException(LocalizationMessages.PARAM_NULL("subscription"));
        }
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }


    @Override
    public void onNext(final OutboundSseEvent item) {
        checkClosed();
        if (item == null) {
            throw new NullPointerException(LocalizationMessages.PARAM_NULL("outboundSseEvent"));
        }
        try {
            write(item);
        } catch (final IOException e) {
            onError(e);
        }
    }

    @Override
    public void onError(final Throwable throwable) {
        checkClosed();
        if (throwable == null) {
            throw new NullPointerException(LocalizationMessages.PARAM_NULL("throwable"));
        }
        subscription.cancel();
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, LocalizationMessages.EVENT_SINK_CLOSE_FAILED(), e);
        }
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        try {
            this.write(event);
            return CompletableFuture.completedFuture(null);
        } catch (IOException e) {
            return CompletableFuture.completedFuture(e);
        }
    }

    public void onComplete() {
        checkClosed();
        subscription.cancel();
        close();
    }

    private void checkClosed() {
        if (isClosed()) {
            throw new IllegalStateException(LocalizationMessages.EVENT_SOURCE_ALREADY_CLOSED());
        }
    }
}
