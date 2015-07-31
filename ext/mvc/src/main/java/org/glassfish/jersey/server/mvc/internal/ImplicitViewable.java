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

package org.glassfish.jersey.server.mvc.internal;

import java.util.List;

import org.glassfish.jersey.server.mvc.Viewable;

/**
 * {@link Viewable} implementation representing return value of enhancing methods added to
 * {@link org.glassfish.jersey.server.model.Resource resources} annotated with {@link org.glassfish.jersey.server.mvc.Template}.
 *
 * @author Michal Gajdos
 * @see TemplateModelProcessor
 * @see org.glassfish.jersey.server.mvc.Template
 */
final class ImplicitViewable extends Viewable {

    private final List<String> templateNames;

    private final Class<?> resolvingClass;

    /**
     * Create a {@code ImplicitViewable}.
     *
     * @param templateNames allowed template names for which a {@link Viewable viewable} can be resolved.
     * @param model the model, may be {@code null}.
     * @param resolvingClass the class to use to resolve the template name if the template is not absolute,
     * if {@code null} then the resolving class will be obtained from the last matching resource.
     * @throws IllegalArgumentException if the template name is {@code null}.
     */
    ImplicitViewable(final List<String> templateNames, final Object model, final Class<?> resolvingClass)
            throws IllegalArgumentException {
        super("", model);

        this.templateNames = templateNames;
        this.resolvingClass = resolvingClass;
    }

    /**
     * Get allowed template names for which a {@link Viewable viewable} can be resolved.
     *
     * @return allowed template names.
     */
    public List<String> getTemplateNames() {
        return templateNames;
    }

    /**
     * Get the resolving class.
     *
     * @return Resolving class.
     */
    public Class<?> getResolvingClass() {
        return resolvingClass;
    }
}
