/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.CloseableService;
import org.glassfish.jersey.server.internal.LocalizationMessages;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Binder and Factory implementations for {@code CloseableService}.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class CloseableServiceBinder extends AbstractBinder {

    /**
     * {@code CloseableService} implementation that stores instances of {@code Closeable} into the properties map obtained from
     * {@code HttpContext}.
     */
    private static class DefaultCloseableService implements CloseableService {

        private final static Logger LOGGER = Logger.getLogger(DefaultCloseableService.class.getName());

        private final AtomicBoolean closed = new AtomicBoolean(false);

        private final Set<Closeable> closeables = Sets.newIdentityHashSet();

        @Override
        public void add(final Closeable closeable) {
            if (!closed.get()) {
                closeables.add(closeable);
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                for (final Closeable closeable : closeables) {
                    try {
                        closeable.close();
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE,
                                LocalizationMessages.CLOSEABLE_UNABLE_TO_CLOSE(closeable.getClass().getName()), ex);
                    }
                }
            }
        }
    }

    @Override
    protected void configure() {
        bind(DefaultCloseableService.class).to(CloseableService.class).in(RequestScoped.class);
    }
}
