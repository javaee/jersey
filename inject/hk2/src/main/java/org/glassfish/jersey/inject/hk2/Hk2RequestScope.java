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

package org.glassfish.jersey.inject.hk2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.LazyUid;
import org.glassfish.jersey.process.internal.RequestScope;

import static org.glassfish.jersey.internal.guava.Preconditions.checkState;

public class Hk2RequestScope extends RequestScope {

    @Override
    public org.glassfish.jersey.process.internal.RequestContext createContext() {
        return new Instance();
    }

    /**
     * Implementation of the request scope instance.
     */
    public static final class Instance implements org.glassfish.jersey.process.internal.RequestContext {

        private final ExtendedLogger logger = new ExtendedLogger(Logger.getLogger(Instance.class.getName()), Level.FINEST);

        /*
         * Scope instance UUID.
         *
         * For performance reasons, it's only generated if toString() method is invoked,
         * e.g. as part of some low-level logging.
         */
        private final LazyUid id = new LazyUid();

        /**
         * A map of injectable instances in this scope.
         */
        private final Map<ForeignDescriptor, Object> store;

        /**
         * Holds the number of snapshots of this scope.
         */
        private final AtomicInteger referenceCounter;

        private Instance() {
            this.store = new HashMap<>();
            this.referenceCounter = new AtomicInteger(1);
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
        public Hk2RequestScope.Instance getReference() {
            // TODO: replace counter with a phantom reference + reference queue-based solution
            referenceCounter.incrementAndGet();
            return this;
        }

        /**
         * Get an inhabitant stored in the scope instance that matches the active descriptor .
         *
         * @param <T>        inhabitant type.
         * @param descriptor inhabitant descriptor.
         * @return matched inhabitant stored in the scope instance or {@code null} if not matched.
         */
        @SuppressWarnings("unchecked")
        public <T> T get(ForeignDescriptor descriptor) {
            return (T) store.get(descriptor);
        }

        /**
         * Store a new inhabitant for the given descriptor.
         *
         * @param <T>        inhabitant type.
         * @param descriptor inhabitant descriptor.
         * @param value      inhabitant value.
         * @return old inhabitant previously stored for the given descriptor or
         * {@code null} if none stored.
         */
        @SuppressWarnings("unchecked")
        public <T> T put(ForeignDescriptor descriptor, T value) {
            checkState(!store.containsKey(descriptor),
                    "An instance for the descriptor %s was already seeded in this scope. Old instance: %s New instance: %s",
                    descriptor,
                    store.get(descriptor),
                    value);

            return (T) store.put(descriptor, value);
        }

        /**
         * Remove a value for the descriptor if present in the scope instance store.
         *
         * @param descriptor key for the value to be removed.
         */
        @SuppressWarnings("unchecked")
        public <T> void remove(ForeignDescriptor descriptor) {
            final T removed = (T) store.remove(descriptor);
            if (removed != null) {
                descriptor.dispose(removed);
            }
        }

        public boolean contains(ForeignDescriptor provider) {
            return store.containsKey(provider);
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
                    new HashSet<>(store.keySet()).forEach(this::remove);
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
}

