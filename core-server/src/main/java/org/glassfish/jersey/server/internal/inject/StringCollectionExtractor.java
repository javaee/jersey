/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Extract parameter value as a specific {@code String} Java collection type.
 * <p />
 * This class can be seen as a special, optimized, case of {@link CollectionExtractor}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class StringCollectionExtractor implements MultivaluedParameterExtractor<Collection<String>> {

    private final String parameter;
    private final String defaultValue;

    /**
     * Create new string collection parameter extractor.
     *
     * @param parameterName parameter name.
     * @param defaultValue  default parameter value.
     */
    protected StringCollectionExtractor(String parameterName, String defaultValue) {
        this.parameter = parameterName;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getName() {
        return parameter;
    }

    @Override
    public String getDefaultValueString() {
        return defaultValue;
    }

    @Override
    public Collection<String> extract(MultivaluedMap<String, String> parameters) {
        List<String> stringList = parameters.get(parameter);

        final Collection<String> collection = newCollection();
        if (stringList != null) {
            collection.addAll(stringList);
        } else if (defaultValue != null) {
            collection.add(defaultValue);
        }

        return collection;
    }

    /**
     * Get a new string collection instance that will be used to store the extracted parameters.
     *
     * The method is overridden by concrete implementations to return an instance
     * of a proper collection sub-type.
     *
     * @return instance of a proper collection sub-type
     */
    protected abstract Collection<String> newCollection();

    private static final class ListString extends StringCollectionExtractor {

        public ListString(String parameter, String defaultValue) {
            super(parameter, defaultValue);
        }

        @Override
        protected List<String> newCollection() {
            return new ArrayList<String>();
        }
    }

    private static final class SetString extends StringCollectionExtractor {

        public SetString(String parameter, String defaultValue) {
            super(parameter, defaultValue);
        }

        @Override
        protected Set<String> newCollection() {
            return new HashSet<String>();
        }
    }

    private static final class SortedSetString extends StringCollectionExtractor {

        public SortedSetString(String parameter, String defaultValue) {
            super(parameter, defaultValue);
        }

        @Override
        protected SortedSet<String> newCollection() {
            return new TreeSet<String>();
        }
    }

    /**
     * Get string collection extractor instance supporting the given collection
     * class type for the parameter specified.
     *
     * @param collectionType collection type to be supported by the extractor.
     * @param parameterName  extracted parameter name.
     * @param defaultValue   default parameter value.
     * @return string collection extractor instance supporting the given collection
     *         class type.
     */
    public static StringCollectionExtractor getInstance(Class<?> collectionType, String parameterName, String defaultValue) {
        if (List.class == collectionType) {
            return new ListString(parameterName, defaultValue);
        } else if (Set.class == collectionType) {
            return new SetString(parameterName, defaultValue);
        } else if (SortedSet.class == collectionType) {
            return new SortedSetString(parameterName, defaultValue);
        } else {
            throw new RuntimeException("Unsupported collection type: " + collectionType.getName());
        }
    }
}
