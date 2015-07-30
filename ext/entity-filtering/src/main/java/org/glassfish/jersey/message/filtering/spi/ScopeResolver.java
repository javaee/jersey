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

import org.glassfish.jersey.spi.Contract;

/**
 * Class used to resolve entity-filtering scopes from annotations. Annotations passed to {@code #resolve()} method
 * can be one of the following: entity annotations (provided when creating request/response entity),
 * annotations obtained from {@link javax.ws.rs.core.Configuration configuration}, resource method / resource class annotations.
 * <p/>
 * Entity-filtering scope is supposed to be an unique string that can be derived from an annotations and that can be further used
 * in internal entity data filtering structures. Examples of such unique strings are:
 * <ul>
 * <li><code>@MyDetailedView</code> -&gt; <code>my.package.MyDetailedView</code></li>
 * <li>
 * <code>@RolesAllowed({"manager", "user"})</code> -&gt; <code>javax.annotation.security.RolesAllowed_manager</code> and
 * <code>javax.annotation.security.RolesAllowed_user</code>
 * </li>
 * </ul>
 * <p/>
 * {@link ScopeResolver Scope resolvers} are invoked from {@link ScopeProvider scope provider} instance.
 *
 * @author Michal Gajdos
 */
@Contract
public interface ScopeResolver {

    /**
     * Resolve entity-filtering scopes for given annotations.
     *
     * @param annotations list of arbitrary annotations.
     * @return non-null set of entity-filtering scopes.
     */
    public Set<String> resolve(final Annotation[] annotations);
}
