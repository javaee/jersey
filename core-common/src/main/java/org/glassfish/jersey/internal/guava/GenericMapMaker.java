/*
 * Copyright (C) 2010 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.jersey.internal.guava;

/**
 * A class exactly like {@link MapMaker}, except restricted in the types of maps it can build.
 * For the most part, you should probably just ignore the existence of this class.
 *
 * @param <K0> the base type for all key types of maps built by this map maker
 * @param <V0> the base type for all value types of maps built by this map maker
 * @author Kevin Bourrillion
 * @since 7.0
 * @deprecated This class existed only to support the generic paramterization necessary for the
 * caching functionality in {@code MapMaker}. That functionality has been moved to {@link
 * CacheBuilder}, which is a properly generified class and thus needs no
 * "Generic" equivalent; simple use {@code CacheBuilder} naturally. For general migration
 * instructions, see the <a
 * href="http://code.google.com/p/guava-libraries/wiki/MapMakerMigration">MapMaker Migration
 * Guide</a>.
 */
@Deprecated
abstract class GenericMapMaker<K0, V0> {

    @SuppressWarnings("unchecked")
        // safe covariant cast
    <K extends K0, V extends V0> MapMaker.RemovalListener<K, V> getRemovalListener() {
        return (MapMaker.RemovalListener<K, V>) GenericMapMaker.NullListener.INSTANCE;
    }

    enum NullListener implements MapMaker.RemovalListener<Object, Object> {
        INSTANCE;

        @Override
        public void onRemoval(MapMaker.RemovalNotification<Object, Object> notification) {
        }
    }

}
