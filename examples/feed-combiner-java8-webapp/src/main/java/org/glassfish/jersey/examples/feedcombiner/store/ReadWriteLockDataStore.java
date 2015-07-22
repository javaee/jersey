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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A simple in-memory store for serializable values based on key. Implementation
 * of the {@link InMemoryDataStore data store} and their methods are thread-safe.
 * <p/>
 * The data store provides the capability of an adding {@link DataStoreObserver observers} which
 * are able to listen any changes in the data store.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class ReadWriteLockDataStore implements InMemoryDataStore, ObservableDataStore {

    private static final Logger LOG = LoggerFactory.getLogger(ReadWriteLockDataStore.class.getName());

    // check whether is possible to cast a loaded entity to the datastore type
    private static final Predicate<Object> DATASTORE_CAST_CHECKER = new DataStoreCastChecker();

    // instances ensure thread-safe nature of this data store
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readerLock = lock.readLock();
    private final Lock writerLock = lock.writeLock();

    private HashMap<String, Serializable> datastore = new HashMap<>();

    // observers which get information about saving and removing entities
    private Set<DataStoreObserver> observers = new CopyOnWriteArraySet<>();

    /**
     * Deserializes an {@code Optional<Object>} from the given stream.
     * <p/>
     * The given stream will not be close.
     *
     * @param input the serialized object input stream, must not be null
     * @return the serialized object
     * @throws IOException in case the load operation failed
     **/
    private static Optional<Object> deserialize(InputStream input) throws IOException {
        try {
            BufferedInputStream bis = new BufferedInputStream(input);
            ObjectInputStream ois = new ObjectInputStream(bis);

            return Optional.of(ois.readObject());
        } catch (ClassNotFoundException ex) {
            LOG.error("An error occurred during deserialization an object.", ex);
        }
        return Optional.empty();
    }

    /**
     * Serializes an {@code Serializable} to the specified stream.
     * <p/>
     * The given stream will not be close.
     *
     * @param object the object to serialize to bytes
     * @param output the stream to write to, must not be null
     * @throws IOException in case the load operation failed
     */
    private static void serialize(Serializable object, OutputStream output) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(output);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);
        bos.flush();
    }

    public <T extends Serializable> Serializable put(String key, T data) {
        requireNonNull(key, "The parameter 'key' must not be null");

        writerLock.lock();
        try {

            if (data == null) {
                observers.forEach(observer -> observer.remove(key));
                return datastore.remove(key);
            }

            Serializable previousEntity = datastore.put(key, data);
            if (previousEntity == null) {
                observers.forEach(observer -> observer.save(data));
            }
            return previousEntity;
        } finally {
            writerLock.unlock();
        }
    }

    public <T extends Serializable> T get(String key, Class<T> type) {
        requireNonNull(key, "The parameter 'key' must not be null");
        requireNonNull(type, "The parameter 'type' must not be null");

        readerLock.lock();
        try {
            Serializable object = datastore.get(key);
            return type.cast(object);
        } finally {
            readerLock.unlock();
        }
    }

    public void save(OutputStream out) throws IOException {
        readerLock.lock();
        try {
            serialize(datastore, out);
        } finally {
            readerLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public void load(InputStream in) throws IOException {
        Optional<Object> newDatastore = ReadWriteLockDataStore.deserialize(in);

        newDatastore
                .filter(DATASTORE_CAST_CHECKER)
                .map(HashMap.class::cast)
                .ifPresent(loadedStore -> {
                            writerLock.lock();
                            try {
                                observers.forEach(observer -> observer.removeAll(this.datastore.keySet()));
                                this.datastore = loadedStore;
                                observers.forEach(observer -> observer.saveAll(loadedStore.values()));
                            } finally {
                                writerLock.unlock();
                            }
                        }
                );
    }

    @Override
    public void addObserver(DataStoreObserver observer) {
        requireNonNull(observer, "It's not possible to add 'null' observer.");
        observers.add(observer);
    }

    @Override
    public void deleteObserver(DataStoreObserver observer) {
        observers.remove(observer);
    }

    public Collection<Serializable> getAll() {
        readerLock.lock();
        try {
            return datastore.values();
        } finally {
            readerLock.unlock();
        }
    }

    /**
     * The instance of this class checks whether is possible to cast an unknown loaded entity
     * to the datastore type.
     *
     * @author Petr Bouda
     **/
    private static final class DataStoreCastChecker implements Predicate<Object> {

        @Override
        public boolean test(Object entity) {
            try {
                @SuppressWarnings({"unchecked", "unused"})
                HashMap<String, Serializable> loadedStore = (HashMap<String, Serializable>) entity;
            } catch (ClassCastException ex) {
                LOG.error("The loaded object is not a valid type of the datastore.", ex);
                return false;
            }
            return true;
        }
    }
}
