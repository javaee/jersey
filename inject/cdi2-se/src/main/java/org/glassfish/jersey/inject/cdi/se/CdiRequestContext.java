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

package org.glassfish.jersey.inject.cdi.se;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.LazyUid;
import org.glassfish.jersey.process.internal.RequestContext;

/**
 * Implementation of the request context.
 */
public final class CdiRequestContext implements RequestContext {

    private static final ExtendedLogger logger =
            new ExtendedLogger(Logger.getLogger(CdiRequestContext.class.getName()), Level.FINEST);

    /*
     * Scope instance UUID.
     *
     * For performance reasons, it's only generated if toString() method is invoked,
     * e.g. as part of some low-level logging.
     */
    private final LazyUid id = new LazyUid();

    /**
     * Holds the number of snapshots of this scope.
     */
    private final AtomicInteger referenceCounter;

    /**
     * A map of injectable instances in this scope.
     */
    private final Map<String, Object> store;

    CdiRequestContext() {
        this.store = new HashMap<>();
        this.referenceCounter = new AtomicInteger(1);
    }

    Map<String, Object> getStore() {
        return store;
    }

    /**
     * Get a "new" reference of the scope instance. This will increase
     * the internal reference counter which prevents the scope instance
     * to be destroyed until a {@link #release()} method is explicitly
     * called (once per each {@code getReference()} method call).
     *
     * @return referenced scope instance.
     */
    @Override
    public RequestContext getReference() {
        // TODO: replace counter with a phantom reference + reference queue-based solution
        referenceCounter.incrementAndGet();
        return this;
    }

    /**
     * Release a single reference to the current request scope instance.
     * <p>
     * Once all instance references are released, the instance will be recycled.
     */
    @Override
    public void release() {
        if (referenceCounter.decrementAndGet() < 1) {
            try {
                store.clear();
            } finally {
                logger.debugLog("Released scope instance {0}", this);
            }
        }
    }

    @Override
    public String toString() {
        return "Instance{"
                + "id=" + id
                + ", referenceCounter=" + referenceCounter
                + ", store size=" + store.size()
                + '}';
    }
}