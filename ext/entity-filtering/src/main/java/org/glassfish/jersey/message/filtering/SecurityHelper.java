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

package org.glassfish.jersey.message.filtering;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.core.SecurityContext;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.glassfish.jersey.message.filtering.spi.FilteringHelper;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Utility methods for security Entity Data Filtering.
 *
 * @author Michal Gajdos
 */
final class SecurityHelper {

    private static final Set<String> roles = Sets.newHashSet();

    /**
     * Get entity-filtering scopes of security annotations present among given annotations.
     * <p>
     * A scope look like:
     * <ul>
     * <li>&lt;fully qualified annotation class name&gt;, or</li>
     * <li>&lt;fully qualified annotation class name&gt;_&lt;role&gt;</li>
     * </ul>
     * </p>
     *
     * @param annotations a list of annotations (doesn't need to contain only security annotations)
     * @return a set of entity-filtering scopes.
     */
    static Set<String> getFilteringScopes(final Annotation[] annotations) {
        return getFilteringScopes(null, annotations);
    }

    /**
     * Get entity-filtering scopes of security annotations present among given annotations with respect to given
     * {@link SecurityContext}. Resulting set contains only scopes that pass the security context check.
     * <p>
     * A scope look like:
     * <ul>
     * <li>&lt;fully qualified annotation class name&gt;, or</li>
     * <li>&lt;fully qualified annotation class name&gt;_&lt;role&gt;</li>
     * </ul>
     * </p>
     *
     * @param securityContext security context to check whether a user is in specified logical role.
     * @param annotations a list of annotations (doesn't need to contain only security annotations)
     * @return a set of entity-filtering scopes.
     */
    static Set<String> getFilteringScopes(final SecurityContext securityContext, final Annotation[] annotations) {
        if (annotations.length == 0) {
            return Collections.emptySet();
        }

        for (final Annotation annotation : annotations) {
            if (annotation instanceof RolesAllowed) {
                final Set<String> bindings = Sets.newHashSet();

                for (final String role : ((RolesAllowed) annotation).value()) {
                    if (securityContext == null || securityContext.isUserInRole(role)) {
                        bindings.add(getRolesAllowedScope(role));
                    }
                }

                return bindings;
            } else if (annotation instanceof PermitAll) {
                return FilteringHelper.getDefaultFilteringScope();
            } else if (annotation instanceof DenyAll) {
                return null;
            }
        }

        return Collections.emptySet();
    }

    /**
     * Get entity-filtering scope for {@link RolesAllowed}s role.
     *
     * @param role role to retrieve entity-filtering scope for.
     * @return entity-filtering scope.
     */
    static String getRolesAllowedScope(final String role) {
        roles.add(role);
        return RolesAllowed.class.getName() + "_" + role;
    }

    /**
     * Get authorization roles that has been derived from examining entity classes.
     *
     * @return already processed authorization roles.
     */
    static Set<String> getProcessedRoles() {
        return roles;
    }

    /**
     * Prevent instantiation.
     */
    private SecurityHelper() {
    }
}
