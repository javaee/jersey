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

import java.util.List;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.glassfish.hk2.api.AnnotationLiteral;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Convenience utility methods for creating instances of security annotations.
 *
 * @author Michal Gajdos
 */
public final class SecurityAnnotations {

    /**
     * Create {@link RolesAllowed} annotation implementation for given set of roles.
     *
     * @param roles roles to be part of the annotation.
     * @return annotation implementation.
     */
    public static RolesAllowed rolesAllowed(final String... roles) {
        final List<String> list = Lists.newArrayListWithCapacity(roles.length);
        for (final String role : roles) {
            if (role != null) {
                list.add(role);
            }
        }
        return new RolesAllowedImpl(list.toArray(new String[list.size()]));
    }

    /**
     * Create {@link PermitAll} annotation implementation.
     *
     * @return annotation implementation.
     */
    public static PermitAll permitAll() {
        return new PermitAllImpl();
    }

    /**
     * Create {@link DenyAll} annotation implementation.
     *
     * @return annotation implementation.
     */
    public static DenyAll denyAll() {
        return new DenyAllImpl();
    }

    /**
     * DenyAll annotation implementation.
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static final class RolesAllowedImpl extends AnnotationLiteral<RolesAllowed> implements RolesAllowed {

        private final String[] roles;

        private RolesAllowedImpl(final String[] roles) {
            this.roles = roles;
        }

        @Override
        public String[] value() {
            return roles;
        }
    }

    /**
     * DenyAll annotation implementation.
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static final class DenyAllImpl extends AnnotationLiteral<DenyAll> implements DenyAll {
    }

    /**
     * PermitAll annotation implementation.
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static class PermitAllImpl extends AnnotationLiteral<PermitAll> implements PermitAll {
    }

    /**
     * Prevent instantiation.
     */
    private SecurityAnnotations() {
    }
}
