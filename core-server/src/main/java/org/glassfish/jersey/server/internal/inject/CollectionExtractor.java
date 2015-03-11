/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverter;

import org.glassfish.jersey.server.internal.LocalizationMessages;

/**
 * Extract parameter value as a typed collection.
 *
 * @param <T> parameter value type.
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class CollectionExtractor<T> extends AbstractParamValueExtractor<T>
        implements MultivaluedParameterExtractor<Collection<T>> {

    /**
     * Create new collection parameter extractor.
     *
     * @param converter          parameter converter to be used to convert parameter from a String.
     * @param parameterName      parameter name.
     * @param defaultStringValue default parameter String value.
     */
    protected CollectionExtractor(final ParamConverter<T> converter,
                                  final String parameterName,
                                  final String defaultStringValue) {
        super(converter, parameterName, defaultStringValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<T> extract(final MultivaluedMap<String, String> parameters) {
        final List<String> stringList = parameters.get(getName());

        final Collection<T> valueList = newCollection();
        if (stringList != null) {
            for (final String v : stringList) {
                valueList.add(fromString(v));
            }
        } else if (isDefaultValueRegistered()) {
            valueList.add(defaultValue());
        }

        return valueList;
    }

    /**
     * Get a new collection instance that will be used to store the extracted parameters.
     * <p/>
     * The method is overridden by concrete implementations to return an instance
     * of a proper collection sub-type.
     *
     * @return instance of a proper collection sub-type
     */
    protected abstract Collection<T> newCollection();

    private static final class ListValueOf<T> extends CollectionExtractor<T> {

        ListValueOf(final ParamConverter<T> converter, final String parameter, final String defaultValueString) {
            super(converter, parameter, defaultValueString);
        }

        @Override
        protected List<T> newCollection() {
            return new ArrayList<>();
        }
    }

    private static final class SetValueOf<T> extends CollectionExtractor<T> {

        SetValueOf(final ParamConverter<T> converter, final String parameter, final String defaultValueString) {
            super(converter, parameter, defaultValueString);
        }

        @Override
        protected Set<T> newCollection() {
            return new HashSet<>();
        }
    }

    private static final class SortedSetValueOf<T> extends CollectionExtractor<T> {

        SortedSetValueOf(final ParamConverter<T> converter, final String parameter, final String defaultValueString) {
            super(converter, parameter, defaultValueString);
        }

        @Override
        protected SortedSet<T> newCollection() {
            return new TreeSet<>();
        }
    }

    /**
     * Get a new {@code CollectionExtractor} instance.
     *
     * @param collectionType     raw collection type.
     * @param converter          parameter converter to be used to convert parameter string values into
     *                           values of the requested Java type.
     * @param parameterName      parameter name.
     * @param defaultValueString default parameter string value.
     * @param <T>                converted parameter Java type.
     * @return new collection parameter extractor instance.
     */
    public static <T> CollectionExtractor getInstance(final Class<?> collectionType,
                                                      final ParamConverter<T> converter,
                                                      final String parameterName,
                                                      final String defaultValueString) {
        if (List.class == collectionType) {
            return new ListValueOf<>(converter, parameterName, defaultValueString);
        } else if (Set.class == collectionType) {
            return new SetValueOf<>(converter, parameterName, defaultValueString);
        } else if (SortedSet.class == collectionType) {
            return new SortedSetValueOf<>(converter, parameterName, defaultValueString);
        } else {
            throw new ProcessingException(LocalizationMessages.COLLECTION_EXTRACTOR_TYPE_UNSUPPORTED());
        }
    }
}
