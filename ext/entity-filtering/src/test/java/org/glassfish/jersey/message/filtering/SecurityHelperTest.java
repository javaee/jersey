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

import javax.annotation.security.RolesAllowed;

import org.glassfish.jersey.internal.inject.CustomAnnotationLiteral;
import org.glassfish.jersey.message.filtering.spi.FilteringHelper;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * {@link org.glassfish.jersey.message.filtering.SecurityHelper} unit tests.
 *
 * @author Michal Gajdos
 */
public class SecurityHelperTest {

    @Test
    public void testFilteringScopes() throws Exception {
        Annotation[] annotations;
        Set<String> expected;

        // Empty annotations.
        annotations = new Annotation[0];
        assertThat(SecurityHelper.getFilteringScopes(annotations), equalTo(Collections.<String>emptySet()));

        // Not security annotations.
        annotations = new Annotation[] {CustomAnnotationLiteral.INSTANCE, CustomAnnotationLiteral.INSTANCE};
        assertThat(SecurityHelper.getFilteringScopes(annotations), equalTo(Collections.<String>emptySet()));

        // Mixed.
        annotations = new Annotation[] {CustomAnnotationLiteral.INSTANCE, SecurityAnnotations
                .rolesAllowed("manager"), CustomAnnotationLiteral.INSTANCE};
        expected = Sets.newHashSet(RolesAllowed.class.getName() + "_manager");
        assertThat(SecurityHelper.getFilteringScopes(annotations), equalTo(expected));

        // Multiple.
        annotations = new Annotation[] {SecurityAnnotations.rolesAllowed("manager", "user")};
        expected = Sets.newHashSet(RolesAllowed.class.getName() + "_manager", RolesAllowed.class.getName() + "_user");
        assertThat(SecurityHelper.getFilteringScopes(annotations), equalTo(expected));

        // PermitAll weirdo.
        annotations = new Annotation[] {SecurityAnnotations.permitAll()};
        assertThat(SecurityHelper.getFilteringScopes(annotations), equalTo(FilteringHelper.getDefaultFilteringScope()));

        // DenyAll weirdo.
        annotations = new Annotation[] {SecurityAnnotations.denyAll()};
        assertThat(SecurityHelper.getFilteringScopes(annotations), equalTo(null));
    }

    @Test
    public void testFilteringScopesWithContext() throws Exception {
        final SecurityContext context = new TestSecurityContext();

        Annotation[] annotations;
        Set<String> expected;

        // Empty annotations.
        annotations = new Annotation[0];
        assertThat(SecurityHelper.getFilteringScopes(context, annotations), equalTo(Collections.<String>emptySet()));

        // Not security annotations.
        annotations = new Annotation[] {CustomAnnotationLiteral.INSTANCE, CustomAnnotationLiteral.INSTANCE};
        assertThat(SecurityHelper.getFilteringScopes(context, annotations), equalTo(Collections.<String>emptySet()));

        // Mixed.
        annotations = new Annotation[] {CustomAnnotationLiteral.INSTANCE, SecurityAnnotations
                .rolesAllowed("manager"), CustomAnnotationLiteral.INSTANCE};
        expected = Sets.newHashSet(RolesAllowed.class.getName() + "_manager");
        assertThat(SecurityHelper.getFilteringScopes(context, annotations), equalTo(expected));

        // Multiple.
        annotations = new Annotation[] {SecurityAnnotations.rolesAllowed("client", "user")};
        expected = Sets.newHashSet(RolesAllowed.class.getName() + "_user");
        assertThat(SecurityHelper.getFilteringScopes(context, annotations), equalTo(expected));

        // PermitAll weirdo.
        annotations = new Annotation[] {SecurityAnnotations.permitAll()};
        assertThat(SecurityHelper.getFilteringScopes(context, annotations), equalTo(FilteringHelper.getDefaultFilteringScope()));

        // DenyAll weirdo.
        annotations = new Annotation[] {SecurityAnnotations.denyAll()};
        assertThat(SecurityHelper.getFilteringScopes(context, annotations), equalTo(null));
    }

}
