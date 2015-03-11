/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.filtering;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.message.filtering.spi.ScopeResolver;

import jersey.repackaged.com.google.common.collect.Sets;

@Singleton
public class SelectableScopeResolver implements ScopeResolver {

    /**
     * Prefix for all selectable scopes
     */
    public static final String PREFIX = SelectableScopeResolver.class.getName() + "_";

    /**
     * Scope used for selecting all fields, i.e.: when no filter is applied
     */
    public static final String DEFAULT_SCOPE = PREFIX + "*";

    /**
     * Query parameter name for selectable feature, set to default value
     */
    private static String SELECTABLE_PARAM_NAME = "select";

    @Context
    private Configuration configuration;

    @Context
    private UriInfo uriInfo;

    @PostConstruct
    private void init() {
        final String paramName = (String) configuration.getProperty(SelectableEntityFilteringFeature.QUERY_PARAM_NAME);
        SELECTABLE_PARAM_NAME = paramName != null ? paramName : SELECTABLE_PARAM_NAME;
    }

    @Override
    public Set<String> resolve(final Annotation[] annotations) {
        final Set<String> scopes = new HashSet<>();

        final List<String> fields = uriInfo.getQueryParameters().get(SELECTABLE_PARAM_NAME);
        if (fields != null && !fields.isEmpty()) {
            for (final String field : fields) {
                scopes.addAll(getScopesForField(field));
            }
        } else {
            scopes.add(DEFAULT_SCOPE);
        }
        return scopes;
    }

    private Set<String> getScopesForField(final String fieldName) {
        final Set<String> scopes = Sets.newHashSet();

        // add specific scope in case of specific request
        final String[] fields = Tokenizer.tokenize(fieldName, ",");
        for (final String field : fields) {
            final String[] subfields = Tokenizer.tokenize(field, ".");
            // in case of nested path, add first level as stand-alone to ensure subgraph is added
            scopes.add(SelectableScopeResolver.PREFIX + subfields[0]);
            if (subfields.length > 1) {
                scopes.add(SelectableScopeResolver.PREFIX + field);
            }
        }

        return scopes;
    }
}
