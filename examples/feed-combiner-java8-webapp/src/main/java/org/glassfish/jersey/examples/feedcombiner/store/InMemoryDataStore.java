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

package org.glassfish.jersey.examples.feedcombiner.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * A simple in-memory store for serializable values based on key.
 * <p/>
 * All instances of this interface and their methods are thread-safe.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public interface InMemoryDataStore {

    /**
     * Store new data or replace existing data stored under a {@code key} with the new
     * data.
     * <p/>
     * The method throws an exception if the {@code key} is {@code null}.
     *
     * @param key  a unique identifier of the location where the data should be
     *             stored or replaced. Must not be {@code null}.
     * @param data new data to be stored under the given {@code key}. May be {@code null},
     *             in which case the data will be removed from the store.
     * @return {@code null} if there are no data stored under the given {@code key}
     * or any replaced data previously stored under the {@code key}.
     * @throws NullPointerException in case the {@code key} is {@code null}.
     */
    <T extends Serializable> Serializable put(String key, T data);

    /**
     * Retrieve data from the data store.
     * <p/>
     * The method retrieves the data stored under a unique {@code key} from the data store
     * and returns the data cast into the Java type specified by {@code type} parameter.
     *
     * @param key  a unique identifier of the location from which the stored data should be
     *             retrieved. Must not be {@code null}.
     * @param type expected Java type of the data being retrieved. Must not be {@code null}.
     * @return retrieved data or {@code null} if there are no data stored under the given
     * {@code key}.
     * @throws NullPointerException in case the {@code key} or {@code type} is {@code null}.
     * @throws ClassCastException   in case the data stored under the given {@code key}
     *                              cannot be cast to the Java type represented by
     *                              {@code type} parameter.
     */
    <T extends Serializable> T get(String key, Class<T> type);

    /**
     * Save current content of the data store to an output stream.
     * <p/>
     * The operation is guaranteed to produce a consistent snapshot of the data store
     * inner state.
     *
     * @param out output stream where the content of the data store is saved.
     *            The method does not close the stream.
     * @throws IOException in case the save operation failed.
     */
    void save(OutputStream out) throws IOException;

    /**
     * Load content of the data store from an input stream.
     * <p/>
     * Any content previously stored in the data store will be discarded and replaced
     * with the content loaded from the input stream.
     *
     * @param in input stream from which the content of the data store is loaded.
     *           The method does not close the stream.
     * @throws IOException in case the load operation failed.
     */
    void load(InputStream in) throws IOException;
}