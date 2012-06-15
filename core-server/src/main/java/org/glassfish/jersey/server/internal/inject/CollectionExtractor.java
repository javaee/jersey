/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.spi.StringValueReader;

/**
 * Extract parameter value as a typed collection.
 *
 * @param <T> parameter value type.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class CollectionExtractor<T> extends AbstractStringReaderExtractor<T> implements MultivaluedParameterExtractor<Collection<T>> {

    protected CollectionExtractor(StringValueReader<T> sr, String parameter, String defaultStringValue) {
        super(sr, parameter, defaultStringValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<T> extract(MultivaluedMap<String, String> parameters) {
        final List<String> stringList = parameters.get(getName());

        final Collection<T> valueList = getInstance();
        if (stringList != null) {
            for (String v : stringList) {
                valueList.add(fromString(v));
            }
        } else if (isDefaultValueRegistered()) {
            valueList.add(defaultValue());
        }

        return valueList;
    }

    protected abstract Collection<T> getInstance();

    private static final class ListValueOf<T> extends CollectionExtractor<T> {

        ListValueOf(StringValueReader<T> sr, String parameter, String defaultValueString) {
            super(sr, parameter, defaultValueString);
        }

        @Override
        protected List<T> getInstance() {
            return new ArrayList<T>();
        }
    }

    private static final class SetValueOf<T> extends CollectionExtractor<T> {

        SetValueOf(StringValueReader<T> sr, String parameter, String defaultValueString) {
            super(sr, parameter, defaultValueString);
        }

        @Override
        protected Set<T> getInstance() {
            return new HashSet<T>();
        }
    }

    private static final class SortedSetValueOf<T> extends CollectionExtractor<T> {

        SortedSetValueOf(StringValueReader<T> sr, String parameter, String defaultValueString) {
            super(sr, parameter, defaultValueString);
        }

        @Override
        protected SortedSet<T> getInstance() {
            return new TreeSet<T>();
        }
    }

    public static <T> MultivaluedParameterExtractor getInstance(Class<?> c,
            StringValueReader<T> sr, String parameter, String defaultValueString) {
        if (List.class == c) {
            return new ListValueOf<T>(sr, parameter, defaultValueString);
        } else if (Set.class == c) {
            return new SetValueOf<T>(sr, parameter, defaultValueString);
        } else if (SortedSet.class == c) {
            return new SortedSetValueOf<T>(sr, parameter, defaultValueString);
        } else {
            throw new RuntimeException();
        }
    }
}
