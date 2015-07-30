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

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

/**
 * {@link Feature} used to add support for Java Security annotations (<code>javax.annotation.security</code>) for Entity Data
 * Filtering feature.
 * <p>
 * Supported annotations are:
 * <ul>
 * <li>{@link javax.annotation.security.PermitAll}</li>
 * <li>{@link javax.annotation.security.RolesAllowed}</li>
 * <li>{@link javax.annotation.security.DenyAll}</li>
 * </ul>
 * </p>
 * <p>
 * It is sufficient to annotate only property accessors of an entity without annotating resource method / resource class although
 * it is not recommended.
 * </p>
 * Note: This feature also registers the {@link EntityFilteringFeature}.
 *
 * @author Michal Gajdos
 * @see org.glassfish.jersey.message.filtering.EntityFilteringFeature
 */
public final class SecurityEntityFilteringFeature implements Feature {

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        if (!config.isRegistered(SecurityEntityProcessor.class)) {
            // RolesAllowed feature.
            if (!config.isRegistered(RolesAllowedDynamicFeature.class)) {
                context.register(RolesAllowedDynamicFeature.class);
            }

            // Binder (FilteringObjectProvider/FilteringGraphTransformer).
            if (!config.isRegistered(EntityFilteringBinder.class)) {
                context.register(new EntityFilteringBinder());
            }

            // Entity Processors.
            context.register(SecurityEntityProcessor.class);
            if (!config.isRegistered(DefaultEntityProcessor.class)) {
                context.register(DefaultEntityProcessor.class);
            }

            // Scope Providers.
            context.register(SecurityScopeResolver.class);
            if (RuntimeType.SERVER.equals(config.getRuntimeType())) {
                context.register(SecurityServerScopeResolver.class);
            }

            // Scope Resolver.
            if (RuntimeType.SERVER == config.getRuntimeType()) {
                context.register(SecurityServerScopeProvider.class);
            } else {
                context.register(CommonScopeProvider.class);
            }

            return true;
        }
        return false;
    }
}
