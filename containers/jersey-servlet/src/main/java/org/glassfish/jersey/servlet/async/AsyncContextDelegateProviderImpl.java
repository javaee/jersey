/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.servlet.async;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.servlet.init.internal.LocalizationMessages;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegate;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegateProvider;

/**
 * Servlet 3.x container response writer async extension and related extension factory implementation.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class AsyncContextDelegateProviderImpl implements AsyncContextDelegateProvider {

    private static final Logger LOGGER = Logger.getLogger(AsyncContextDelegateProviderImpl.class.getName());

    @Override
    public final AsyncContextDelegate createDelegate(final HttpServletRequest request, final HttpServletResponse response) {
        return new ExtensionImpl(request, response);
    }

    private static final class ExtensionImpl implements AsyncContextDelegate {

        private static final int NEVER_TIMEOUT_VALUE = -1;

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final AtomicReference<AsyncContext> asyncContextRef;
        private final AtomicBoolean completed;

        /**
         * Create a Servlet 3.x {@link AsyncContextDelegate} with given {@code request} and {@code response}.
         *
         * @param request  request to create {@link AsyncContext} for.
         * @param response response to create {@link AsyncContext} for.
         */
        private ExtensionImpl(final HttpServletRequest request, final HttpServletResponse response) {
            this.request = request;
            this.response = response;
            this.asyncContextRef = new AtomicReference<>();
            this.completed = new AtomicBoolean(false);
        }

        @Override
        public void suspend() throws IllegalStateException {
            // Suspend only if not completed and not suspended before.
            if (!completed.get() && asyncContextRef.get() == null) {
                asyncContextRef.set(getAsyncContext());
            }
        }

        private AsyncContext getAsyncContext() {
            final AsyncContext asyncContext;
            if (request.isAsyncStarted()) {
                asyncContext = request.getAsyncContext();
                try {
                    asyncContext.setTimeout(NEVER_TIMEOUT_VALUE);
                } catch (IllegalStateException ex) {
                    // Let's hope the time out is set properly, otherwise JAX-RS AsyncResponse time-out support
                    // may not work as expected... At least we can log this at fine level...
                    LOGGER.log(Level.FINE, LocalizationMessages.SERVLET_ASYNC_CONTEXT_ALREADY_STARTED(), ex);
                }
            } else {
                asyncContext = request.startAsync(request, response);
                // Tell underlying asyncContext to never time out.
                asyncContext.setTimeout(NEVER_TIMEOUT_VALUE);
            }
            return asyncContext;
        }

        @Override
        public void complete() {
            completed.set(true);

            final AsyncContext asyncContext = asyncContextRef.getAndSet(null);
            if (asyncContext != null) {
                asyncContext.complete();
            }
        }
    }
}
