/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.message.filtering.spi;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.glassfish.jersey.message.filtering.EntityFiltering;
import org.glassfish.jersey.spi.Contract;

/**
 * Entry point for obtaining entity-filtering scopes used to process a request/response entity. Entity-filtering scopes are
 * obtained from (sorted by priority):
 * <ul>
 * <li>entity annotations - provided with entity when creating request/response</li>
 * <li>annotations stored under
 * {@value org.glassfish.jersey.message.filtering.EntityFilteringFeature#ENTITY_FILTERING_SCOPE} property obtained from
 * {@link javax.security.auth.login.Configuration configuration}
 * </li>
 * <li>entity-filtering annotations present on resource methods/classes (on server)</li>
 * </ul>
 * <p/>
 * Note: Definition of entity-filtering scopes can be found in {@link ScopeResolver}.
 *
 * @author Michal Gajdos
 * @see ScopeResolver
 */
@Contract
public interface ScopeProvider {

    /**
     * Default entity-filtering scope.
     * <p/>
     * Default scope is used in {@link ObjectGraph object graph} to retrieve a subgraph instance at the moment subgraph's entity
     * class does not define any entity-filtering scope the object graph was created for.
     * <p/>
     * This scope is created for an {@link EntityGraph entity graph} if no other entity-filtering / security annotation is present
     * on a class.
     */
    public static final String DEFAULT_SCOPE = EntityFiltering.class.getName();

    /**
     * Get entity-filtering scopes to be used to process an entity.
     *
     * @param entityAnnotations entity annotations provided with entity when creating request/response.
     * @param defaultIfNotFound flag determining whether the default entity-filtering scope should be returned if no other
     * scope can be obtained.
     * @return non-null entity-filtering scopes.
     */
    public Set<String> getFilteringScopes(final Annotation[] entityAnnotations, final boolean defaultIfNotFound);
}
