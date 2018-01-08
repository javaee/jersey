/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.server.mvc;

import org.glassfish.jersey.server.mvc.internal.LocalizationMessages;

/**
 * A viewable type referencing a template by name and a model to be passed
 * to the template. Such a type may be returned by a resource method of a
 * resource class. In this respect the template is the view and the controller
 * is the resource class in the Model View Controller pattern.
 * <p/>
 * The template name may be declared as absolute template name if the name
 * begins with a '/', otherwise the template name is declared as a relative
 * template name. If the template name is relative then the class of the
 * last matching resource is utilized to create an absolute path by default. However,
 * the responsibility of resolving the absolute template name is up to
 * {@link org.glassfish.jersey.server.mvc.spi.ViewableContext} which can override the
 * default resolving behaviour.
 * <p/>
 *
 * @author Paul Sandoz
 * @author Michal Gajdos
 *
 * @see Template
 * @see org.glassfish.jersey.server.mvc.spi.ViewableContext
 * @see org.glassfish.jersey.server.mvc.internal.ResolvingViewableContext
 */
public class Viewable {

    private final String templateName;

    private final Object model;

    /**
     * Construct a new viewable type with a template name.
     * <p/>
     * The model will be set to {@code null}.
     *
     * @param templateName the template name, shall not be {@code null}.
     * @throws IllegalArgumentException if the template name is {@code null}.
     */
    public Viewable(String templateName) throws IllegalArgumentException {
        this(templateName, null);
    }

    /**
     * Construct a new viewable type with a template name and a model.
     *
     * @param templateName the template name, shall not be {@code null}.
     * @param model the model, may be {@code null}.
     * @throws IllegalArgumentException if the template name is {@code null}.
     */
    public Viewable(String templateName, Object model) throws IllegalArgumentException {
        if (templateName == null) {
            throw new IllegalArgumentException(LocalizationMessages.TEMPLATE_NAME_MUST_NOT_BE_NULL());
        }

        this.templateName = templateName;
        this.model = model;
    }

    /**
     * Get the template name.
     *
     * @return the template name.
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Get the model.
     *
     * @return the model.
     */
    public Object getModel() {
        return model;
    }


    /**
     * Determines whether the template name is represented by an absolute path.
     *
     * @return {@code true} if the template name is absolute, and starts with a
     *         '/' character, {@code false} otherwise.
     */
    public boolean isTemplateNameAbsolute() {
        return templateName.length() > 0 && templateName.charAt(0) == '/';
    }
}
